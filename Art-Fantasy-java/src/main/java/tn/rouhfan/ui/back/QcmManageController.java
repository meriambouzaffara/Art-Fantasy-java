package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;
import tn.rouhfan.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QcmManageController {

    @FXML private Label lblTitreCours;
    @FXML private TableView<QcmQuestion> questionTable;
    @FXML private TableColumn<QcmQuestion, String> colQuestion;
    @FXML private TableColumn<QcmQuestion, Void> colAction;
    @FXML private TextArea taQuestion;
    @FXML private TextField tfR1, tfR2, tfR3, tfR4;
    @FXML private CheckBox checkR1, checkR2, checkR3, checkR4;

    private Qcm currentQcm;
    private Connection cnx;
    private final QcmService qcmService = new QcmService();
    private final QcmQuestionService questionService = new QcmQuestionService();
    private final ObservableList<QcmQuestion> questionList = FXCollections.observableArrayList();

    public void setCours(Cours cours) {
        lblTitreCours.setText("Gestion QCM : " + cours.getNom());
        cnx = MyDatabase.getInstance().getConnection();
        try {
            this.currentQcm = qcmService.getOrCreateQcm(cours.getId());
            setupTable();
            refreshTable();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur: " + e.getMessage());
        }
    }

    private void setupTable() {
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Supprimer");
            {
                btnDelete.setStyle("-fx-background-color: #f38ba8; -fx-text-fill: white;");
                btnDelete.setOnAction(event -> handleDeleteQuestion(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
        questionTable.setItems(questionList);
    }

    @FXML
    private void handleSaveQuestion() {
        if (taQuestion.getText().trim().isEmpty()) {
            showError("La question ne peut pas être vide");
            return;
        }

        try {
            QcmQuestion q = new QcmQuestion();
            q.setQcm(currentQcm);
            q.setQuestion(taQuestion.getText().trim());

            // Sauvegarder la question
            questionService.ajouter(q);

            // Sauvegarder les réponses
            sauvegarderReponses(q);

            clearForm();
            refreshTable();
            showSuccess("Question et réponses enregistrées avec succès !");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur: " + e.getMessage());
        }
    }

    private void sauvegarderReponses(QcmQuestion question) throws SQLException {
        List<QcmReponse> reponses = new ArrayList<>();

        ajouterReponseSiNonVide(reponses, tfR1.getText(), checkR1.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfR2.getText(), checkR2.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfR3.getText(), checkR3.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfR4.getText(), checkR4.isSelected(), question);

        String sql = "INSERT INTO qcm_reponse (texte, correcte, id_question) VALUES (?, ?, ?)";
        for (QcmReponse reponse : reponses) {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, reponse.getTexte());
            ps.setBoolean(2, reponse.isCorrecte());
            ps.setInt(3, question.getId());
            ps.executeUpdate();
            ps.close();
        }
    }

    private void ajouterReponseSiNonVide(List<QcmReponse> liste, String texte, boolean correcte, QcmQuestion question) {
        if (texte != null && !texte.trim().isEmpty()) {
            QcmReponse reponse = new QcmReponse();
            reponse.setTexte(texte.trim());
            reponse.setCorrecte(correcte);
            reponse.setQuestion(question);
            liste.add(reponse);
        }
    }

    private void handleDeleteQuestion(QcmQuestion q) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la question");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette question ?\nToutes les réponses seront également supprimées.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Supprimer d'abord les réponses
                supprimerReponsesParQuestion(q.getId());
                // Puis supprimer la question
                questionService.supprimer(q.getId());
                refreshTable();
                showSuccess("Question supprimée avec succès !");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur: " + e.getMessage());
            }
        }
    }

    private void supprimerReponsesParQuestion(int questionId) throws SQLException {
        String sql = "DELETE FROM qcm_reponse WHERE id_question = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, questionId);
        ps.executeUpdate();
        ps.close();
    }

    private void refreshTable() throws SQLException {
        questionList.setAll(questionService.recupererParQcm(currentQcm.getId()));
    }

    private void clearForm() {
        taQuestion.clear();
        tfR1.clear();
        tfR2.clear();
        tfR3.clear();
        tfR4.clear();
        checkR1.setSelected(false);
        checkR2.setSelected(false);
        checkR3.setSelected(false);
        checkR4.setSelected(false);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}