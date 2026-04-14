package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class CoursController implements Initializable {

    // ══════════════════════════════════════════════════════
    //  TableView
    // ══════════════════════════════════════════════════════
    @FXML private TableView<Cours>            coursTable;
    @FXML private TableColumn<Cours, Integer> colId;
    @FXML private TableColumn<Cours, String>  colNom;
    @FXML private TableColumn<Cours, String>  colDescription;
    @FXML private TableColumn<Cours, String>  colNiveau;
    @FXML private TableColumn<Cours, String>  colDuree;
    @FXML private TableColumn<Cours, String>  colStatut;
    @FXML private TableColumn<Cours, String>  colArtiste;
    @FXML private TextField                   searchField;

    // ══════════════════════════════════════════════════════
    //  Formulaire intégré
    // ══════════════════════════════════════════════════════
    @FXML private VBox             formPane;
    @FXML private Label            lblFormTitre;
    @FXML private TextField        tfNom;
    @FXML private Label            errNom;
    @FXML private TextArea         taDescription;
    @FXML private Label            errDescription;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private Label            errNiveau;
    @FXML private TextField        tfDuree;
    @FXML private Label            errDuree;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label            errStatut;
    @FXML private TextArea         taContenu;
    @FXML private Label            errContenu;
    @FXML private ComboBox<User>   cbArtiste;
    @FXML private Label            errArtiste;
    @FXML private Label            lblErreurGlobal;
    @FXML private Button           btnSauvegarder;

    // ══════════════════════════════════════════════════════
    //  Services & état
    // ══════════════════════════════════════════════════════
    private final CoursService          coursService = new CoursService();
    private final UserService           userService  = new UserService();
    private final ObservableList<Cours> coursList    = FXCollections.observableArrayList();
    private       FilteredList<Cours>   filteredList;
    private       Cours                 coursAModifier = null;

    // ══════════════════════════════════════════════════════
    //  Styles
    // ══════════════════════════════════════════════════════
    private static final String FIELD_OK =
            "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;" +
                    "-fx-prompt-text-fill:#6c7086;-fx-background-radius:6;-fx-padding:8;" +
                    "-fx-border-color:#45475a;-fx-border-radius:6;-fx-border-width:1;";
    private static final String FIELD_ERR =
            "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;" +
                    "-fx-prompt-text-fill:#6c7086;-fx-background-radius:6;-fx-padding:8;" +
                    "-fx-border-color:#f38ba8;-fx-border-radius:6;-fx-border-width:1.5;";
    private static final String COMBO_OK =
            "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;" +
                    "-fx-background-radius:6;-fx-border-color:#45475a;" +
                    "-fx-border-radius:6;-fx-border-width:1;";
    private static final String COMBO_ERR =
            "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;" +
                    "-fx-background-radius:6;-fx-border-color:#f38ba8;" +
                    "-fx-border-radius:6;-fx-border-width:1.5;";

    // ══════════════════════════════════════════════════════
    //  Initialisation
    // ══════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        chargerComboBoxes();
        chargerUtilisateurs();
        attacherValidation();
        loadCours();
        formPane.setVisible(false);
        formPane.setManaged(false);
    }

    // ══════════════════════════════════════════════════════
    //  Colonnes
    // ══════════════════════════════════════════════════════
    private void setupColumns() {
        colId.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cd.getValue().getId()).asObject());
        colNom.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getNom()));
        colDescription.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getDescription()));
        colNiveau.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getNiveau()));
        colDuree.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getDuree()));
        colStatut.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getStatut()));
        colArtiste.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getArtiste() != null
                                ? cd.getValue().getArtiste().getNom() + " "
                                + cd.getValue().getArtiste().getPrenom()
                                : ""));

        // ✅ if-else au lieu de switch -> (compatible Java 11)
        colStatut.setCellFactory(col -> new TableCell<Cours, String>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(val);
                    if ("Publié".equals(val)) {
                        setStyle("-fx-text-fill:#a6e3a1;-fx-font-weight:bold;");
                    } else if ("Brouillon".equals(val)) {
                        setStyle("-fx-text-fill:#f9e2af;-fx-font-weight:bold;");
                    } else if ("En attente".equals(val)) {
                        setStyle("-fx-text-fill:#89b4fa;-fx-font-weight:bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  Recherche en temps réel
    // ══════════════════════════════════════════════════════
    private void setupSearch() {
        filteredList = new FilteredList<>(coursList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                filteredList.setPredicate(c -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    String low = newVal.toLowerCase();
                    return c.getNom().toLowerCase().contains(low)
                            || (c.getDescription() != null
                            && c.getDescription().toLowerCase().contains(low))
                            || (c.getNiveau() != null
                            && c.getNiveau().toLowerCase().contains(low));
                })
        );
        coursTable.setItems(filteredList);
    }

    // ══════════════════════════════════════════════════════
    //  Chargement DB
    // ══════════════════════════════════════════════════════
    private void loadCours() {
        try {
            coursList.setAll(coursService.recuperer());
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les cours : " + e.getMessage());
        }
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = userService.recuperer();
            cbArtiste.setItems(FXCollections.observableArrayList(users));
            cbArtiste.setCellFactory(lv -> new ListCell<User>() {
                @Override
                protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    if (empty || u == null) {
                        setText("");
                    } else {
                        setText(u.getId() + " — " + u.getNom() + " " + u.getPrenom());
                    }
                }
            });
            cbArtiste.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    if (empty || u == null) {
                        setText("");
                    } else {
                        setText(u.getId() + " — " + u.getNom() + " " + u.getPrenom());
                    }
                }
            });
        } catch (SQLException e) {
            showAlert("Erreur", "Chargement utilisateurs : " + e.getMessage());
        }
    }

    private void chargerComboBoxes() {
        cbNiveau.setItems(FXCollections.observableArrayList(
                "Débutant", "Intermédiaire", "Avancé"));
        cbStatut.setItems(FXCollections.observableArrayList(
                "Brouillon", "En attente", "Publié"));
    }

    // ══════════════════════════════════════════════════════
    //  Boutons principaux
    // ══════════════════════════════════════════════════════
    @FXML
    private void refresh(ActionEvent e) {
        loadCours();
    }

    @FXML
    private void addCours(ActionEvent e) {
        coursAModifier = null;
        lblFormTitre.setText("➕ Ajouter un cours");
        btnSauvegarder.setText("💾 Sauvegarder");
        viderFormulaire();
        afficherFormulaire(true);
    }

    @FXML
    private void editCours(ActionEvent e) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un cours à modifier.");
            return;
        }
        coursAModifier = selected;
        lblFormTitre.setText("✏️ Modifier : " + selected.getNom());
        btnSauvegarder.setText("💾 Modifier");
        preRemplirFormulaire(selected);
        afficherFormulaire(true);
    }

    @FXML
    private void deleteCours(ActionEvent e) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un cours à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le cours");
        confirm.setContentText("Supprimer « " + selected.getNom() + " » ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    coursService.supprimer(selected.getId());
                    loadCours();
                    afficherFormulaire(false);
                } catch (SQLException ex) {
                    showAlert("Erreur", "Impossible de supprimer : " + ex.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  Boutons formulaire
    // ══════════════════════════════════════════════════════
    @FXML
    private void handleSauvegarder() {
        lblErreurGlobal.setText("");
        boolean ok = validerNom()
                & validerDescription()
                & validerNiveau()
                & validerDuree()
                & validerStatut()
                & validerContenu()
                & validerArtiste();

        if (!ok) {
            lblErreurGlobal.setText("⚠️ Corrigez les erreurs avant de sauvegarder.");
            return;
        }
        try {
            Cours c = construireCours();
            if (coursAModifier == null) {
                coursService.ajouter(c);
                showSucces("Cours ajouté avec succès !");
            } else {
                c.setId(coursAModifier.getId());
                coursService.modifier(c);
                showSucces("Cours modifié avec succès !");
            }
            loadCours();
            afficherFormulaire(false);
        } catch (SQLException ex) {
            lblErreurGlobal.setText("❌ Erreur DB : " + ex.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        afficherFormulaire(false);
        coursTable.getSelectionModel().clearSelection();
    }

    // ══════════════════════════════════════════════════════
    //  Formulaire : affichage / remplissage / vidage
    // ══════════════════════════════════════════════════════
    private void afficherFormulaire(boolean visible) {
        formPane.setVisible(visible);
        formPane.setManaged(visible);
        if (!visible) viderFormulaire();
    }

    private void preRemplirFormulaire(Cours c) {
        tfNom.setText(c.getNom());
        taDescription.setText(c.getDescription());
        cbNiveau.setValue(c.getNiveau());
        tfDuree.setText(c.getDuree());
        cbStatut.setValue(c.getStatut());
        taContenu.setText(c.getContenu());
        if (c.getArtiste() != null) {
            cbArtiste.getItems().stream()
                    .filter(u -> u.getId() == c.getArtiste().getId())
                    .findFirst()
                    .ifPresent(cbArtiste::setValue);
        }
        cacherToutesErreurs();
    }

    private void viderFormulaire() {
        tfNom.clear();
        taDescription.clear();
        cbNiveau.setValue(null);
        tfDuree.clear();
        cbStatut.setValue(null);
        taContenu.clear();
        cbArtiste.setValue(null);
        appliquerStylesOk();
        cacherToutesErreurs();
        lblErreurGlobal.setText("");
    }

    private void appliquerStylesOk() {
        tfNom.setStyle(FIELD_OK);
        taDescription.setStyle(FIELD_OK);
        cbNiveau.setStyle(COMBO_OK);
        tfDuree.setStyle(FIELD_OK);
        cbStatut.setStyle(COMBO_OK);
        taContenu.setStyle(FIELD_OK);
        cbArtiste.setStyle(COMBO_OK);
    }

    private void cacherToutesErreurs() {
        Label[] labels = {errNom, errDescription, errNiveau,
                errDuree, errStatut, errContenu, errArtiste};
        for (Label l : labels) {
            l.setText("");
            l.setVisible(false);
        }
    }

    // ══════════════════════════════════════════════════════
    //  Validation temps réel
    // ══════════════════════════════════════════════════════
    private void attacherValidation() {
        tfNom.focusedProperty().addListener((o, was, is) -> {
            if (!is) validerNom();
        });
        taDescription.focusedProperty().addListener((o, was, is) -> {
            if (!is) validerDescription();
        });
        cbNiveau.valueProperty().addListener((o, a, b) -> validerNiveau());
        tfDuree.focusedProperty().addListener((o, was, is) -> {
            if (!is) validerDuree();
        });
        cbStatut.valueProperty().addListener((o, a, b) -> validerStatut());
        taContenu.focusedProperty().addListener((o, was, is) -> {
            if (!is) validerContenu();
        });
        cbArtiste.valueProperty().addListener((o, a, b) -> validerArtiste());

        // Limite de caractères
        tfNom.textProperty().addListener((o, old, nw) -> {
            if (nw.length() > 100) tfNom.setText(old);
        });
        taDescription.textProperty().addListener((o, old, nw) -> {
            if (nw.length() > 500) taDescription.setText(old);
        });
    }

    // ══════════════════════════════════════════════════════
    //  Validateurs individuels
    // ══════════════════════════════════════════════════════
    private boolean validerNom() {
        String v = tfNom.getText().trim();
        if (v.isEmpty()) {
            return err(tfNom, errNom, "Le nom est obligatoire.");
        }
        if (v.length() < 3) {
            return err(tfNom, errNom, "Minimum 3 caractères.");
        }
        if (!v.matches("[\\p{L}0-9 \\-:'.&(),!?]+")) {
            return err(tfNom, errNom, "Caractères non autorisés.");
        }
        return ok(tfNom, errNom);
    }

    private boolean validerDescription() {
        String v = taDescription.getText().trim();
        if (!v.isEmpty() && v.length() < 10) {
            return errArea(taDescription, errDescription,
                    "Minimum 10 caractères si renseignée.");
        }
        return okArea(taDescription, errDescription);
    }

    private boolean validerNiveau() {
        if (cbNiveau.getValue() == null) {
            return errCombo(cbNiveau, errNiveau, "Veuillez choisir un niveau.");
        }
        return okCombo(cbNiveau, errNiveau);
    }

    private boolean validerDuree() {
        String v = tfDuree.getText().trim();
        if (!v.isEmpty() && !v.matches("\\d+[hH](\\s*\\d+[mM])?|\\d+\\s*[mM]in|\\d+h")) {
            return err(tfDuree, errDuree, "Format invalide. Ex: 2h 30m, 45min");
        }
        return ok(tfDuree, errDuree);
    }

    private boolean validerStatut() {
        if (cbStatut.getValue() == null) {
            return errCombo(cbStatut, errStatut, "Le statut est obligatoire.");
        }
        return okCombo(cbStatut, errStatut);
    }

    private boolean validerContenu() {
        String v = taContenu.getText().trim();
        if (v.isEmpty()) {
            return errArea(taContenu, errContenu, "Le contenu est obligatoire.");
        }
        if (v.length() < 10) {
            return errArea(taContenu, errContenu, "Minimum 10 caractères.");
        }
        return okArea(taContenu, errContenu);
    }

    private boolean validerArtiste() {
        if (cbArtiste.getValue() == null) {
            return errCombo(cbArtiste, errArtiste, "Veuillez sélectionner un artiste.");
        }
        return okCombo(cbArtiste, errArtiste);
    }

    // ══════════════════════════════════════════════════════
    //  Helpers erreur / ok
    // ══════════════════════════════════════════════════════
    private boolean err(TextField f, Label l, String msg) {
        f.setStyle(FIELD_ERR);
        l.setText(msg);
        l.setVisible(true);
        return false;
    }

    private boolean ok(TextField f, Label l) {
        f.setStyle(FIELD_OK);
        l.setText("");
        l.setVisible(false);
        return true;
    }

    private boolean errArea(TextArea f, Label l, String msg) {
        f.setStyle(FIELD_ERR);
        l.setText(msg);
        l.setVisible(true);
        return false;
    }

    private boolean okArea(TextArea f, Label l) {
        f.setStyle(FIELD_OK);
        l.setText("");
        l.setVisible(false);
        return true;
    }

    private boolean errCombo(ComboBox<?> f, Label l, String msg) {
        f.setStyle(COMBO_ERR);
        l.setText(msg);
        l.setVisible(true);
        return false;
    }

    private boolean okCombo(ComboBox<?> f, Label l) {
        f.setStyle(COMBO_OK);
        l.setText("");
        l.setVisible(false);
        return true;
    }

    // ══════════════════════════════════════════════════════
    //  Construction de l'objet Cours
    // ══════════════════════════════════════════════════════
    private Cours construireCours() {
        return new Cours(
                tfNom.getText().trim(),
                taDescription.getText().trim(),
                cbNiveau.getValue(),
                tfDuree.getText().trim(),
                cbStatut.getValue(),
                taContenu.getText().trim(),
                cbArtiste.getValue()
        );
    }

    // ══════════════════════════════════════════════════════
    //  Utilitaires
    // ══════════════════════════════════════════════════════
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setHeaderText(null);
        a.setContentText("✅ " + msg);
        a.showAndWait();
    }
}