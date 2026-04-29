package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import tn.rouhfan.tools.MyDatabase;

public class CoursController implements Initializable {

    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colNom, colNiveau, colDuree, colStatut, colArtiste;
    @FXML private TextField searchField;
    @FXML private VBox formPane, questionsContainer;
    @FXML private Label lblErreurGlobal;
    @FXML private TextField tfNom, tfDuree;
    @FXML private TextArea taDescription, taContenu;
    @FXML private ComboBox<String> cbNiveau, cbStatut;
    @FXML private ComboBox<User> cbArtiste;

    @FXML private ListView<QcmQuestion> lvQuestions;
    @FXML private TextArea tfQuickQuestion;
    @FXML private TextField tfQuickR1, tfQuickR2, tfQuickR3, tfQuickR4;
    @FXML private CheckBox cbR1, cbR2, cbR3, cbR4;
    @FXML private Button btnAddQuestionAction;

    private final CoursService coursService = new CoursService();
    private final UserService userService = new UserService();
    private final QcmService qcmService = new QcmService();
    private final QcmQuestionService questionService = new QcmQuestionService();
    private final Coursreviewservice reviewService = new Coursreviewservice();
    private Connection cnx;

    private final ObservableList<Cours> coursList = FXCollections.observableArrayList();
    private FilteredList<Cours> filteredList;
    private Cours coursAModifier = null;
    private Qcm currentQcm = null;
    private QcmQuestion questionEnModif = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cnx = MyDatabase.getInstance().getConnection();
        setupColumns();
        setupSearch();
        setupQuestionsListView();
        chargerComboBoxes();
        chargerUtilisateurs();
        loadCours();
        afficherFormulaire(false);

        coursTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        System.out.println("Cours sélectionné: " + newSelection.getNom());
                    }
                }
        );
    }

    private void setupQuestionsListView() {
        if (lvQuestions == null) return;
        lvQuestions.setCellFactory(param -> new ListCell<>() {
            private final HBox container = new HBox(10);
            private final Label label = new Label();
            private final Button btnEdit = new Button("✏️");
            private final Button btnDel = new Button("🗑️");
            private final Pane spacer = new Pane();
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                btnDel.setStyle("-fx-background-color: #f38ba8; -fx-text-fill: white;");
                btnEdit.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: white;");
                container.getChildren().addAll(label, spacer, btnEdit, btnDel);
                container.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(QcmQuestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item.getQuestion());
                    btnDel.setOnAction(e -> handleDeleteQuestion(item));
                    btnEdit.setOnAction(e -> handlePrepareEdit(item));
                    setGraphic(container);
                }
            }
        });
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom"));
        colNiveau.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("niveau"));
        colDuree.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("duree"));
        colStatut.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut"));
        colArtiste.setCellValueFactory(cd -> {
            Cours c = cd.getValue();
            if (c != null && c.getArtiste() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        c.getArtiste().getPrenom() + " " + c.getArtiste().getNom());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP "VOIR LES NOTES" — NOUVEAU
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleVoirNotes() {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un cours pour voir ses notes.");
            return;
        }
        ouvrirPopupNotes(selected);
    }

    private void ouvrirPopupNotes(Cours cours) {
        try {
            double moyenne  = reviewService.getMoyenne(cours.getId());
            int    total    = reviewService.getNombreNotes(cours.getId());
            int[]  distrib  = reviewService.getDistributionNotes(cours.getId());

            // ── Fenêtre popup ─────────────────────────────────────────────────
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("★ Notes — " + cours.getNom());
            popup.setResizable(false);

            VBox root = new VBox(20);
            root.setPadding(new Insets(28));
            root.setStyle("-fx-background-color: #f4f7f6;");
            root.setPrefWidth(420);

            // ── En-tête ───────────────────────────────────────────────────────
            VBox header = new VBox(6);
            header.setStyle(
                    "-fx-background-color: linear-gradient(to right, #0f1c3f, #1a3a6e);" +
                            "-fx-background-radius: 14; -fx-padding: 20 24;"
            );
            Label lblTitre = new Label("★  Notes du cours");
            lblTitre.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
            Label lblCoursNom = new Label(cours.getNom());
            lblCoursNom.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.70);");
            header.getChildren().addAll(lblTitre, lblCoursNom);

            // ── Score global ──────────────────────────────────────────────────
            HBox scoreBox = new HBox(20);
            scoreBox.setAlignment(Pos.CENTER);
            scoreBox.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 14;" +
                            "-fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
            );

            // Chiffre moyen
            VBox moyenneBox = new VBox(4);
            moyenneBox.setAlignment(Pos.CENTER);
            Label lblMoyChiffre = new Label(total > 0 ? String.format("%.1f", moyenne) : "—");
            lblMoyChiffre.setStyle("-fx-font-size: 42; -fx-font-weight: bold; -fx-text-fill: #0f1c3f;");

            // Étoiles de la moyenne
            HBox etoilesMoy = new HBox(3);
            etoilesMoy.setAlignment(Pos.CENTER);
            int arrondi = (int) Math.round(moyenne);
            for (int i = 1; i <= 5; i++) {
                Label e = new Label(i <= arrondi ? "★" : "☆");
                e.setStyle("-fx-font-size: 20; -fx-text-fill: #FFD700;");
                etoilesMoy.getChildren().add(e);
            }

            Label lblTotal = new Label(total + " note" + (total > 1 ? "s" : ""));
            lblTotal.setStyle("-fx-font-size: 12; -fx-text-fill: #a0aec0;");
            moyenneBox.getChildren().addAll(lblMoyChiffre, etoilesMoy, lblTotal);

            // ── Distribution des notes ────────────────────────────────────────
            VBox distribBox = new VBox(6);
            distribBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(distribBox, Priority.ALWAYS);

            for (int i = 5; i >= 1; i--) {
                int nb = distrib[i - 1];
                double pct = total > 0 ? (double) nb / total : 0;

                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label lblNote = new Label(i + " ★");
                lblNote.setStyle("-fx-font-size: 12; -fx-text-fill: #4a5568; -fx-min-width: 40;");

                // Barre de progression
                StackPane barBg = new StackPane();
                barBg.setPrefSize(130, 10);
                barBg.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 5;");

                Region barFill = new Region();
                barFill.setPrefHeight(10);
                barFill.setPrefWidth(130 * pct);
                barFill.setStyle("-fx-background-color: #f6d365; -fx-background-radius: 5;");
                StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
                barBg.getChildren().add(barFill);

                Label lblNb = new Label(nb + "");
                lblNb.setStyle("-fx-font-size: 11; -fx-text-fill: #718096; -fx-min-width: 20;");

                row.getChildren().addAll(lblNote, barBg, lblNb);
                distribBox.getChildren().add(row);
            }

            scoreBox.getChildren().addAll(moyenneBox, new Separator(javafx.geometry.Orientation.VERTICAL), distribBox);

            // ── Message si aucune note ────────────────────────────────────────
            if (total == 0) {
                Label lblVide = new Label("Aucune note pour ce cours pour l'instant.");
                lblVide.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 13; -fx-font-style: italic;");
                lblVide.setAlignment(Pos.CENTER);
                lblVide.setMaxWidth(Double.MAX_VALUE);
                root.getChildren().addAll(header, scoreBox, lblVide);
            } else {
                root.getChildren().addAll(header, scoreBox);
            }

            // ── Bouton fermer ─────────────────────────────────────────────────
            Button btnFermer = new Button("Fermer");
            btnFermer.setMaxWidth(Double.MAX_VALUE);
            btnFermer.setStyle(
                    "-fx-background-color: #0f1c3f; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnFermer.setOnAction(e -> popup.close());
            root.getChildren().add(btnFermer);

            popup.setScene(new Scene(root));
            popup.show();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des notes : " + e.getMessage());
        }
    }





    private void handlePrepareEdit(QcmQuestion q) {
        if (q == null) return;
        this.questionEnModif = q;
        tfQuickQuestion.setText(q.getQuestion());

        // Charger les réponses existantes
        try {
            QcmReponseService reponseService = new QcmReponseService();
            List<QcmReponse> reponses = reponseService.recupererParQuestion(q.getId());

            List<TextField> reponseFields = List.of(tfQuickR1, tfQuickR2, tfQuickR3, tfQuickR4);
            List<CheckBox> checkBoxes = List.of(cbR1, cbR2, cbR3, cbR4);

            for (int i = 0; i < reponseFields.size(); i++) {
                if (i < reponses.size()) {
                    reponseFields.get(i).setText(reponses.get(i).getTexte());
                    checkBoxes.get(i).setSelected(reponses.get(i).isCorrecte());
                } else {
                    reponseFields.get(i).clear();
                    checkBoxes.get(i).setSelected(false);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (btnAddQuestionAction != null) btnAddQuestionAction.setText("Modifier la question");
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
                refreshQuestionsList();
                showSuccess("✅ Question supprimée avec succès !");
            } catch (SQLException e) {
                showError("❌ Erreur lors de la suppression: " + e.getMessage());
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

    @FXML
    private void handleQuickAddQuestion() {
        if (currentQcm == null) {
            showError("Veuillez d'abord sélectionner un cours");
            return;
        }

        String questionTexte = tfQuickQuestion.getText().trim();
        if (questionTexte.isEmpty()) {
            showError("La question ne peut pas être vide");
            return;
        }

        try {
            QcmQuestion question;
            if (questionEnModif == null) {
                question = new QcmQuestion();
                question.setQcm(currentQcm);
                question.setQuestion(questionTexte);
                questionService.ajouter(question);
            } else {
                question = questionEnModif;
                question.setQuestion(questionTexte);
                questionService.modifier(question);
                // Supprimer les anciennes réponses
                supprimerReponsesParQuestion(question.getId());
            }

            // Sauvegarder les nouvelles réponses
            sauvegarderReponses(question);

            refreshQuestionsList();
            clearQuestionForm();
            showSuccess("✅ Question et réponses enregistrées avec succès !");

        } catch (SQLException ex) {
            showError("❌ Erreur: " + ex.getMessage());
        }
    }

    private void sauvegarderReponses(QcmQuestion question) throws SQLException {
        List<QcmReponse> reponses = new ArrayList<>();

        ajouterReponseSiNonVide(reponses, tfQuickR1.getText(), cbR1.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfQuickR2.getText(), cbR2.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfQuickR3.getText(), cbR3.isSelected(), question);
        ajouterReponseSiNonVide(reponses, tfQuickR4.getText(), cbR4.isSelected(), question);

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

    private void clearQuestionForm() {
        tfQuickQuestion.clear();
        tfQuickR1.clear();
        tfQuickR2.clear();
        tfQuickR3.clear();
        tfQuickR4.clear();
        cbR1.setSelected(false);
        cbR2.setSelected(false);
        cbR3.setSelected(false);
        cbR4.setSelected(false);
        questionEnModif = null;
        if (btnAddQuestionAction != null) {
            btnAddQuestionAction.setText("Ajouter la question");
        }
    }

    private void refreshQuestionsList() throws SQLException {
        if (currentQcm != null && lvQuestions != null) {
            lvQuestions.setItems(FXCollections.observableArrayList(questionService.recupererParQcm(currentQcm.getId())));
        }
    }

    // ==================== NOUVELLE MÉTHODE POUR VÉRIFIER L'EXISTENCE D'UN COURS ====================
    private boolean coursExisteDeja(String nom, String niveau, Integer idExclu) throws SQLException {
        List<Cours> tousLesCours = coursService.recuperer();

        for (Cours c : tousLesCours) {
            // Si c'est une modification, on exclut le cours en cours d'édition
            if (idExclu != null && c.getId() == idExclu) {
                continue;
            }

            // Vérifier si le nom et le niveau sont identiques (ignorer la casse)
            if (c.getNom().equalsIgnoreCase(nom.trim()) &&
                    c.getNiveau().equalsIgnoreCase(niveau)) {
                return true;
            }
        }
        return false;
    }

    @FXML
    private void handleSauvegarder() {
        if (!validateForm()) {
            return;
        }

        String nomCours = tfNom.getText().trim();
        String niveauCours = cbNiveau.getValue();
        Integer idExclu = (coursAModifier != null) ? coursAModifier.getId() : null;

        try {
            // Vérifier si un cours avec le même nom et même niveau existe déjà
            if (coursExisteDeja(nomCours, niveauCours, idExclu)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention - Doublon détecté");
                alert.setHeaderText("Un cours similaire existe déjà");
                alert.setContentText("Un cours avec le nom \"" + nomCours + "\" et le niveau \"" + niveauCours + "\" existe déjà !\n\n" +
                        "Veuillez choisir un nom différent ou un niveau différent.");
                alert.showAndWait();

                // Mettre en évidence les champs concernés
                tfNom.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 5;");
                cbNiveau.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 5;");

                // Remettre le style par défaut après 3 secondes
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ex) {}
                    javafx.application.Platform.runLater(() -> {
                        tfNom.setStyle("");
                        cbNiveau.setStyle("");
                    });
                }).start();

                return;
            }

            // Réinitialiser les styles
            tfNom.setStyle("");
            cbNiveau.setStyle("");

            Cours c = new Cours(
                    tfNom.getText().trim(),
                    taDescription.getText(),
                    cbNiveau.getValue(),
                    tfDuree.getText(),
                    cbStatut.getValue(),
                    taContenu.getText(),
                    cbArtiste.getValue()
            );

            if (coursAModifier == null) {
                coursService.ajouter(c);
                showSuccess("✅ Cours ajouté avec succès !");
            } else {
                c.setId(coursAModifier.getId());
                coursService.modifier(c);
                showSuccess("✅ Cours modifié avec succès !");
            }
            loadCours();
            afficherFormulaire(false);
            clearForm();
        } catch (SQLException e) {
            showError("❌ Erreur: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (tfNom.getText().trim().isEmpty()) {
            showError("Le nom du cours est obligatoire !");
            tfNom.requestFocus();
            return false;
        }
        if (cbNiveau.getValue() == null) {
            showError("Veuillez sélectionner un niveau !");
            return false;
        }
        if (cbStatut.getValue() == null) {
            showError("Veuillez sélectionner un statut !");
            return false;
        }
        if (cbArtiste.getValue() == null) {
            showError("Veuillez sélectionner un artiste !");
            return false;
        }
        return true;
    }

    private void clearForm() {
        tfNom.clear();
        taDescription.clear();
        tfDuree.clear();
        taContenu.clear();
        cbNiveau.setValue(null);
        cbStatut.setValue(null);
        cbArtiste.setValue(null);
        coursAModifier = null;
        tfNom.setStyle("");
        cbNiveau.setStyle("");
    }

    @FXML
    private void editCours(ActionEvent e) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un cours à modifier !");
            return;
        }
        coursAModifier = selected;
        preRemplirFormulaire(selected);
        try {
            this.currentQcm = qcmService.getOrCreateQcm(selected.getId());
            refreshQuestionsList();
            questionsContainer.setVisible(true);
            questionsContainer.setManaged(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Erreur lors du chargement du QCM: " + ex.getMessage());
        }
        afficherFormulaire(true);
    }

    @FXML
    private void deleteCours(ActionEvent event) {
        Cours selectedCours = coursTable.getSelectionModel().getSelectedItem();

        if (selectedCours == null) {
            showError("Veuillez sélectionner un cours à supprimer !");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer le cours");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer le cours \"" + selectedCours.getNom() + "\" ?\n\n⚠️ Cette action est irréversible !");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                coursService.supprimer(selectedCours.getId());
                loadCours();
                showSuccess("✅ Cours supprimé avec succès !");
                coursTable.getSelectionModel().clearSelection();
                afficherFormulaire(false);
            } catch (SQLException e) {
                showError("❌ Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
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

    private void preRemplirFormulaire(Cours c) {
        tfNom.setText(c.getNom());
        taDescription.setText(c.getDescription());
        tfDuree.setText(c.getDuree());
        taContenu.setText(c.getContenu());
        cbNiveau.setValue(c.getNiveau());
        cbStatut.setValue(c.getStatut());
        cbArtiste.setValue(c.getArtiste());
    }



    private void setupSearch() {
        filteredList = new FilteredList<>(coursList, p -> true);
        searchField.textProperty().addListener((o, old, n) ->
                filteredList.setPredicate(c -> n == null || n.isEmpty() ||
                        c.getNom().toLowerCase().contains(n.toLowerCase()))
        );
        coursTable.setItems(filteredList);
    }

    private void loadCours() {
        try {
            coursList.setAll(coursService.recuperer());
        } catch (SQLException e) {
            showError("Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void chargerComboBoxes() {
        cbNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));
        cbStatut.setItems(FXCollections.observableArrayList("Brouillon", "En attente", "Publié"));
    }

    private void chargerUtilisateurs() {
        try {
            cbArtiste.setItems(FXCollections.observableArrayList(userService.recuperer()));
            cbArtiste.setConverter(new javafx.util.StringConverter<User>() {
                @Override
                public String toString(User user) {
                    if (user == null) return "";
                    return user.getPrenom() + " " + user.getNom();
                }

                @Override
                public User fromString(String string) {
                    return null; // Non nécessaire car le ComboBox n'est pas éditable
                }
            });
        } catch (SQLException e) {
            showError("Erreur chargement artistes: " + e.getMessage());
        }
    }

    private void afficherFormulaire(boolean v) {
        formPane.setVisible(v);
        formPane.setManaged(v);
    }

    @FXML
    private void handleGererQcm() {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un cours pour gérer son QCM !");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ui/back/QcmManageView.fxml")
            );
            javafx.scene.Parent root = loader.load();

            QcmManageController ctrl = loader.getController();
            ctrl.setCours(selected);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Gestion QCM – " + selected.getNom());
            stage.setScene(new javafx.scene.Scene(root, 1000, 680));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir la gestion QCM : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        afficherFormulaire(false);
        clearForm();
    }

    @FXML
    private void addCours(ActionEvent e) {
        coursAModifier = null;
        currentQcm = null;
        clearForm();
        afficherFormulaire(true);
        questionsContainer.setVisible(false);
    }

    @FXML
    private void refresh() {
        loadCours();
        showSuccess("Liste actualisée !");
    }
}