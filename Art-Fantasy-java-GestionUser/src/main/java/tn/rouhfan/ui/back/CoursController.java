package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
    @FXML private TableColumn<Cours, Integer> colId;
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

    private void setupColumns() {
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom"));
        colNiveau.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("niveau"));
        colDuree.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("duree"));
        colStatut.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut"));
        colArtiste.setCellValueFactory(cellData -> {
            Cours cours = cellData.getValue();
            if (cours != null && cours.getArtiste() != null) {
                String artisteNom = cours.getArtiste().getPrenom() + " " + cours.getArtiste().getNom();
                return new javafx.beans.property.SimpleStringProperty(artisteNom);
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
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
        } catch (SQLException e) {
            showError("Erreur chargement artistes: " + e.getMessage());
        }
    }

    private void afficherFormulaire(boolean v) {
        formPane.setVisible(v);
        formPane.setManaged(v);
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