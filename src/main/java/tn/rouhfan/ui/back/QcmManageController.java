package tn.rouhfan.ui.back;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;
import tn.rouhfan.tools.MyDatabase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QcmManageController {

    @FXML private Label lblTitreCours;
    @FXML private ListView<QcmQuestion> questionListView;

    @FXML private TextArea taQuestion;
    @FXML private TextField tfR1, tfR2, tfR3, tfR4;
    @FXML private CheckBox checkR1, checkR2, checkR3, checkR4;

    @FXML private Spinner<Integer> spinnerNombre;
    @FXML private Button btnGenererAi;
    @FXML private ProgressIndicator aiLoader;
    @FXML private Label lblAiStatus;

    private Qcm currentQcm;
    private Cours currentCours;
    private Connection cnx;

    private final QcmService qcmService = new QcmService();
    private final QcmQuestionService questionService = new QcmQuestionService();
    private final AiQcmGeneratorService aiGenerator = new AiQcmGeneratorService();
    private final ObservableList<QcmQuestion> questionList = FXCollections.observableArrayList();

    public void setCours(Cours cours) {
        this.currentCours = cours;
        lblTitreCours.setText("Gestion QCM : " + cours.getNom());
        cnx = MyDatabase.getInstance().getConnection();

        if (spinnerNombre != null)
            spinnerNombre.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 5));

        setAiLoading(false);

        try {
            this.currentQcm = qcmService.getOrCreateQcm(cours.getId());
            setupTable();
            refreshTable();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void setupTable() {
        final QcmReponseService reponseService = new QcmReponseService();

        questionListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {

            private final VBox root       = new VBox(6);
            private final Label lblQ      = new Label();
            private final VBox reponsesBox = new VBox(3);
            private final Button btnDel   = new Button("🗑 Supprimer");
            private final HBox header     = new HBox(8);

            {
                lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #0f1c3f; -fx-wrap-text: true;");
                lblQ.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lblQ, javafx.scene.layout.Priority.ALWAYS);

                btnDel.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #e53e3e;"
                        + " -fx-border-color: #fed7d7; -fx-cursor: hand; -fx-background-radius: 6; -fx-font-size: 11;");
                btnDel.setOnAction(e -> {
                    QcmQuestion item = getItem();
                    if (item != null) handleDeleteQuestion(item);
                });

                header.getChildren().addAll(lblQ, btnDel);
                header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                root.setStyle("-fx-padding: 10 12; -fx-background-color: white;"
                        + " -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");
                root.getChildren().addAll(header, reponsesBox);
            }

            @Override
            protected void updateItem(QcmQuestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                lblQ.setText("📌 " + item.getQuestion());
                reponsesBox.getChildren().clear();

                try {
                    java.util.List<tn.rouhfan.entities.QcmReponse> reponses =
                            reponseService.recupererParQuestion(item.getId());

                    if (reponses.isEmpty()) {
                        Label noRep = new Label("   (aucune réponse enregistrée)");
                        noRep.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11;");
                        reponsesBox.getChildren().add(noRep);
                    } else {
                        for (tn.rouhfan.entities.QcmReponse r : reponses) {
                            String icon  = r.isCorrecte() ? "✅" : "❌";
                            String color = r.isCorrecte() ? "#276749" : "#9b2c2c";
                            String bg    = r.isCorrecte() ? "#f0fff4" : "#fff5f5";
                            Label lbl = new Label(icon + "  " + r.getTexte());
                            lbl.setStyle("-fx-font-size: 12; -fx-text-fill: " + color
                                    + "; -fx-background-color: " + bg
                                    + "; -fx-background-radius: 4; -fx-padding: 2 8;");
                            reponsesBox.getChildren().add(lbl);
                        }
                    }
                } catch (java.sql.SQLException ex) {
                    ex.printStackTrace();
                }

                setGraphic(root);
                setStyle("-fx-background-color: transparent;");
            }
        });

        questionListView.setItems(questionList);
    }

    private void refreshTable() throws SQLException {
        questionList.setAll(questionService.recupererParQcm(currentQcm.getId()));
        // Forcer le rafraîchissement des cellules pour recharger les réponses
        questionListView.refresh();
    }

    @FXML
    private void handleSaveQuestion() {
        if (taQuestion.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ manquant", "La question est vide.");
            return;
        }

        boolean auMoinsUneCorrecte = checkR1.isSelected() || checkR2.isSelected() || checkR3.isSelected() || checkR4.isSelected();
        if (!auMoinsUneCorrecte) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Cochez au moins une réponse correcte.");
            return;
        }

        try {
            QcmQuestion q = new QcmQuestion();
            q.setQcm(currentQcm);
            q.setQuestion(taQuestion.getText().trim());
            questionService.ajouter(q);

            sauvegarderReponses(q);
            clearForm();
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Question enregistrée !");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void sauvegarderReponses(QcmQuestion question) throws SQLException {
        List<QcmReponse> reponses = new ArrayList<>();
        ajouterSiNonVide(reponses, tfR1.getText(), checkR1.isSelected(), question);
        ajouterSiNonVide(reponses, tfR2.getText(), checkR2.isSelected(), question);
        ajouterSiNonVide(reponses, tfR3.getText(), checkR3.isSelected(), question);
        ajouterSiNonVide(reponses, tfR4.getText(), checkR4.isSelected(), question);

        String sql = "INSERT INTO qcm_reponse (texte, correcte, id_question) VALUES (?, ?, ?)";
        for (QcmReponse r : reponses) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, r.getTexte());
                ps.setBoolean(2, r.isCorrecte());
                ps.setInt(3, question.getId());
                ps.executeUpdate();
            }
        }
    }

    private void ajouterSiNonVide(List<QcmReponse> liste, String texte, boolean correcte, QcmQuestion question) {
        if (texte != null && !texte.trim().isEmpty()) {
            QcmReponse r = new QcmReponse();
            r.setTexte(texte.trim());
            r.setCorrecte(correcte);
            r.setQuestion(question);
            liste.add(r);
        }
    }

    @FXML
    private void handleGenererAi() {
        if (currentCours == null || currentQcm == null) return;
        int nombre = spinnerNombre.getValue();
        setAiLoading(true);

        new Thread(() -> {
            try {
                List<AiQcmGeneratorService.QuestionData> questionsData = aiGenerator.generateQuestions(currentCours.getContenu(), nombre);
                String sqlR = "INSERT INTO qcm_reponse (texte, correcte, id_question) VALUES (?, ?, ?)";

                for (AiQcmGeneratorService.QuestionData qData : questionsData) {
                    QcmQuestion question = new QcmQuestion();
                    question.setQcm(currentQcm);
                    question.setQuestion(qData.question);
                    questionService.ajouter(question);

                    for (AiQcmGeneratorService.ReponseData rData : qData.reponses) {
                        try (PreparedStatement ps = cnx.prepareStatement(sqlR)) {
                            ps.setString(1, rData.texte);
                            ps.setBoolean(2, rData.correcte);
                            ps.setInt(3, question.getId());
                            ps.executeUpdate();
                        }
                    }
                }

                Platform.runLater(() -> {
                    setAiLoading(false);
                    try { refreshTable(); } catch (SQLException ex) { ex.printStackTrace(); }
                    showAlert(Alert.AlertType.INFORMATION, "Succès IA", questionsData.size() + " questions générées !");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setAiLoading(false);
                    showAlert(Alert.AlertType.ERROR, "Erreur IA", e.getMessage());
                });
            }
        }).start();
    }

    private void setAiLoading(boolean loading) {
        if (aiLoader != null) aiLoader.setVisible(loading);
        if (btnGenererAi != null) btnGenererAi.setDisable(loading);
        if (lblAiStatus != null) lblAiStatus.setText(loading ? "⏳ Génération en cours..." : "");
    }

    private void handleDeleteQuestion(QcmQuestion q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer \"" + q.getQuestion() + "\" ?", ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                supprimerReponsesParQuestion(q.getId());
                questionService.supprimer(q.getId());
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void supprimerReponsesParQuestion(int questionId) throws SQLException {
        String sql = "DELETE FROM qcm_reponse WHERE id_question = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        }
    }

    @FXML
    private void handleRetour() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/rouhfan/ui/back/CoursAdminView.fxml"));
            lblTitreCours.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        taQuestion.clear();
        tfR1.clear(); tfR2.clear(); tfR3.clear(); tfR4.clear();
        checkR1.setSelected(false); checkR2.setSelected(false);
        checkR3.setSelected(false); checkR4.setSelected(false);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}