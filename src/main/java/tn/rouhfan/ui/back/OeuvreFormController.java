package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.tools.SessionManager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.UUID;

public class OeuvreFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField prixField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<Categorie> categorieCombo;
    
    @FXML private ImageView imagePreview;
    @FXML private Label placeholderLabel;
    @FXML private StackPane imageContainer;
    @FXML private Button saveBtn;
    @FXML private Button browseBtn;
    @FXML private Label titreErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label prixErrorLabel;
    @FXML private Label categorieErrorLabel;
    @FXML private Label imageErrorLabel;

    private OeuvreService oeuvreService;
    private CategorieService categorieService;
    
    private Oeuvre currentOeuvre;
    private File selectedImageFile;
    private boolean saved = false;
    private boolean readOnly = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        categorieService = new CategorieService();
        
        setupCombos();
        loadData();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (readOnly) {
            formTitle.setText("👁️ Détails de l'œuvre");
            titreField.setEditable(false);
            descriptionField.setEditable(false);
            prixField.setEditable(false);
            statutCombo.setDisable(true);
            categorieCombo.setDisable(true);
            
            if (saveBtn != null) {
                saveBtn.setVisible(false);
                saveBtn.setManaged(false);
            }
            if (browseBtn != null) {
                browseBtn.setVisible(false);
                browseBtn.setManaged(false);
            }
        }
    }

    private void setupCombos() {
        categorieCombo.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });
        
        statutCombo.setValue("disponible");
    }

    private void loadData() {
        try {
            categorieCombo.setItems(FXCollections.observableArrayList(categorieService.recuperer()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setOeuvre(Oeuvre oeuvre) {
        this.currentOeuvre = oeuvre;
        if (oeuvre != null) {
            formTitle.setText("✏️ Modifier l'œuvre");
            titreField.setText(oeuvre.getTitre());
            descriptionField.setText(oeuvre.getDescription());
            prixField.setText(oeuvre.getPrix() != null ? oeuvre.getPrix().toString() : "");
            statutCombo.setValue(oeuvre.getStatut());
            
            // Selectionner la bonne catégorie
            for (Categorie c : categorieCombo.getItems()) {
                if (oeuvre.getCategorie() != null && c.getIdCategorie() == oeuvre.getCategorie().getIdCategorie()) {
                    categorieCombo.setValue(c);
                    break;
                }
            }

            if (oeuvre.getImage() != null && !oeuvre.getImage().isEmpty()) {
                String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(oeuvre.getImage());
                if (fullPath != null) {
                    imagePreview.setImage(new Image(fullPath));
                    placeholderLabel.setVisible(false);
                }
            }
        }
    }

    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        
        // Ouvrir directement dans le dossier uploads/oeuvres
        File initialDir = new File(tn.rouhfan.tools.ImageUtils.UPLOADS_DIR + "/oeuvres");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(titreField.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            updateImagePreview(file);
        }
    }

    private void updateImagePreview(File file) {
        imagePreview.setImage(new Image(file.toURI().toString()));
        placeholderLabel.setVisible(false);
    }

    @FXML
    private void save(ActionEvent event) {
        if (!validateFields()) return;

        try {
            if (currentOeuvre == null) currentOeuvre = new Oeuvre();

            currentOeuvre.setTitre(titreField.getText());
            currentOeuvre.setDescription(descriptionField.getText());
            currentOeuvre.setPrix(new BigDecimal(prixField.getText()));
            currentOeuvre.setStatut(statutCombo.getValue());
            currentOeuvre.setCategorie(categorieCombo.getValue());
            
            // Utiliser l'utilisateur connecté automatiquement comme artiste
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentOeuvre.getUser() == null && currentUser != null) {
                currentOeuvre.setUser(currentUser);
            }

            // Gérer l'upload d'image via ImageUtils
            if (selectedImageFile != null) {
                String dbPath = tn.rouhfan.tools.ImageUtils.saveUpload(selectedImageFile, "oeuvres");
                currentOeuvre.setImage(dbPath);
            }

            if (currentOeuvre.getId() == 0) {
                oeuvreService.ajouter(currentOeuvre);
            } else {
                oeuvreService.modifier(currentOeuvre);
            }

            saved = true;
            closeDialog();

        } catch (SQLException | IOException e) {
            showAlert("Erreur", "Impossible d'enregistrer: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        // Hide all error labels initially
        titreErrorLabel.setVisible(false);
        titreErrorLabel.setManaged(false);
        descriptionErrorLabel.setVisible(false);
        descriptionErrorLabel.setManaged(false);
        prixErrorLabel.setVisible(false);
        prixErrorLabel.setManaged(false);
        categorieErrorLabel.setVisible(false);
        categorieErrorLabel.setManaged(false);
        imageErrorLabel.setVisible(false);
        imageErrorLabel.setManaged(false);

        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String prixStr = prixField.getText().trim();

        if (titre.isEmpty()) {
            titreErrorLabel.setText("Le titre est obligatoire.");
            titreErrorLabel.setVisible(true);
            titreErrorLabel.setManaged(true);
            isValid = false;
        } else if (titre.length() < 2) {
            titreErrorLabel.setText("Le titre doit faire au moins 2 caractères.");
            titreErrorLabel.setVisible(true);
            titreErrorLabel.setManaged(true);
            isValid = false;
        } else {
            try {
                // Créer une oeuvre temporaire pour le test
                Oeuvre tempOeuvre = new Oeuvre();
                tempOeuvre.setTitre(titre);
                
                // Utiliser l'utilisateur courant pour le test
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentOeuvre != null && currentOeuvre.getUser() != null) {
                    tempOeuvre.setUser(currentOeuvre.getUser());
                } else {
                    tempOeuvre.setUser(currentUser);
                }

                int excludeId = (currentOeuvre != null) ? currentOeuvre.getId() : 0;
                if (oeuvreService.isOeuvreExiste(tempOeuvre, excludeId)) {
                    titreErrorLabel.setText("Vous avez déjà une œuvre avec ce titre.");
                    titreErrorLabel.setVisible(true);
                    titreErrorLabel.setManaged(true);
                    isValid = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (description.isEmpty()) {
            descriptionErrorLabel.setText("La description est obligatoire.");
            descriptionErrorLabel.setVisible(true);
            descriptionErrorLabel.setManaged(true);
            isValid = false;
        }

        if (prixStr.isEmpty()) {
            prixErrorLabel.setText("Le prix est obligatoire.");
            prixErrorLabel.setVisible(true);
            prixErrorLabel.setManaged(true);
            isValid = false;
        } else {
            try {
                BigDecimal prix = new BigDecimal(prixStr);
                if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                    prixErrorLabel.setText("Le prix doit être supérieur à 0,00 DT.");
                    prixErrorLabel.setVisible(true);
                    prixErrorLabel.setManaged(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                prixErrorLabel.setText("Le prix doit être un nombre valide.");
                prixErrorLabel.setVisible(true);
                prixErrorLabel.setManaged(true);
                isValid = false;
            }
        }

        if (categorieCombo.getValue() == null) {
            categorieErrorLabel.setText("Veuillez sélectionner une catégorie.");
            categorieErrorLabel.setVisible(true);
            categorieErrorLabel.setManaged(true);
            isValid = false;
        }

        boolean hasImage = (selectedImageFile != null) || (currentOeuvre != null && currentOeuvre.getImage() != null && !currentOeuvre.getImage().isEmpty());
        if (!hasImage) {
            imageErrorLabel.setText("Veuillez sélectionner une image.");
            imageErrorLabel.setVisible(true);
            imageErrorLabel.setManaged(true);
            isValid = false;
        }

        javafx.application.Platform.runLater(() -> {
            if (titreField.getScene() != null && titreField.getScene().getWindow() != null) {
                ((javafx.stage.Stage) titreField.getScene().getWindow()).sizeToScene();
            }
        });

        return isValid;
    }

    @FXML private void cancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) titreField.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
