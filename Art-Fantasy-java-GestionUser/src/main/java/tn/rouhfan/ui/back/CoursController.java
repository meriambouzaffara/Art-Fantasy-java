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
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

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

    private final ObservableList<Cours> coursList = FXCollections.observableArrayList();
    private FilteredList<Cours> filteredList;
    private Cours coursAModifier = null;
    private Qcm currentQcm = null;
    private QcmQuestion questionEnModif = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupQuestionsListView();
        chargerComboBoxes();
        chargerUtilisateurs();
        loadCours();
        afficherFormulaire(false);
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
        if (btnAddQuestionAction != null) btnAddQuestionAction.setText("Modifier la question");
    }

    private void handleDeleteQuestion(QcmQuestion q) {
        try {
            questionService.supprimer(q.getId()); // Utilise la nouvelle méthode sécurisée
            refreshQuestionsList();
            lblErreurGlobal.setText("✅ Supprimé.");
        } catch (SQLException e) {
            lblErreurGlobal.setText("❌ Erreur suppression.");
        }
    }

    @FXML
    private void handleQuickAddQuestion() {
        if (currentQcm == null || tfQuickQuestion.getText().trim().isEmpty()) return;
        try {
            if (questionEnModif == null) {
                QcmQuestion q = new QcmQuestion();
                q.setQcm(currentQcm);
                q.setQuestion(tfQuickQuestion.getText().trim());
                questionService.ajouter(q);
            } else {
                questionEnModif.setQuestion(tfQuickQuestion.getText().trim());
                questionService.modifier(questionEnModif);
                questionEnModif = null;
                if (btnAddQuestionAction != null) btnAddQuestionAction.setText("Ajouter la question");
            }
            tfQuickQuestion.clear();
            refreshQuestionsList();
        } catch (SQLException ex) { lblErreurGlobal.setText("❌ " + ex.getMessage()); }
    }

    private void refreshQuestionsList() throws SQLException {
        if (currentQcm != null && lvQuestions != null) {
            lvQuestions.setItems(FXCollections.observableArrayList(questionService.recupererParQcm(currentQcm.getId())));
        }
    }

    // --- AUTRES MÉTHODES (DÉJÀ OK) ---
    @FXML private void handleSauvegarder() {
        try {
            Cours c = new Cours(tfNom.getText().trim(), taDescription.getText(), cbNiveau.getValue(), tfDuree.getText(), cbStatut.getValue(), taContenu.getText(), cbArtiste.getValue());
            if (coursAModifier == null) coursService.ajouter(c);
            else { c.setId(coursAModifier.getId()); coursService.modifier(c); }
            loadCours();
            afficherFormulaire(false);
        } catch (SQLException e) { lblErreurGlobal.setText("❌ Erreur"); }
    }

    @FXML private void editCours(ActionEvent e) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        coursAModifier = selected;
        preRemplirFormulaire(selected);
        try {
            this.currentQcm = qcmService.getOrCreateQcm(selected.getId());
            refreshQuestionsList();
            questionsContainer.setVisible(true);
            questionsContainer.setManaged(true);
        } catch (SQLException ex) { ex.printStackTrace(); }
        afficherFormulaire(true);
    }

    @FXML private void deleteCours(ActionEvent event) {
        Cours s = coursTable.getSelectionModel().getSelectedItem();
        if (s != null) { try { coursService.supprimer(s.getId()); loadCours(); } catch (SQLException e) {} }
    }

    private void preRemplirFormulaire(Cours c) { tfNom.setText(c.getNom()); taDescription.setText(c.getDescription()); tfDuree.setText(c.getDuree()); taContenu.setText(c.getContenu()); cbNiveau.setValue(c.getNiveau()); cbStatut.setValue(c.getStatut()); cbArtiste.setValue(c.getArtiste()); }
    private void setupColumns() { colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id")); colNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom")); colNiveau.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("niveau")); colDuree.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("duree")); colStatut.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut")); }
    private void setupSearch() { filteredList = new FilteredList<>(coursList, p -> true); searchField.textProperty().addListener((o, old, n) -> filteredList.setPredicate(c -> n == null || n.isEmpty() || c.getNom().toLowerCase().contains(n.toLowerCase()))); coursTable.setItems(filteredList); }
    private void loadCours() { try { coursList.setAll(coursService.recuperer()); } catch (SQLException e) {} }
    private void chargerComboBoxes() { cbNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé")); cbStatut.setItems(FXCollections.observableArrayList("Brouillon", "En attente", "Publié")); }
    private void chargerUtilisateurs() { try { cbArtiste.setItems(FXCollections.observableArrayList(userService.recuperer())); } catch (SQLException e) {} }
    private void afficherFormulaire(boolean v) { formPane.setVisible(v); formPane.setManaged(v); }
    @FXML private void handleAnnuler() { afficherFormulaire(false); }
    @FXML private void addCours(ActionEvent e) { coursAModifier = null; currentQcm = null; afficherFormulaire(true); questionsContainer.setVisible(false); }
    @FXML private void refresh() { loadCours(); }
}