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
import tn.rouhfan.services.UserService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class OeuvreFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField prixField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<Categorie> categorieCombo;
    @FXML private ComboBox<User> artisteCombo;
    
    @FXML private ImageView imagePreview;
    @FXML private Label placeholderLabel;
    @FXML private StackPane imageContainer;
    @FXML private Button saveBtn;
    @FXML private Button browseBtn;
    @FXML private Label titreError;
    @FXML private Label descriptionError;
    @FXML private Label prixError;
    @FXML private Label categorieError;
    @FXML private Label artisteError;
    @FXML private Label imageError;

    private OeuvreService oeuvreService;
    private CategorieService categorieService;
    private UserService userService;
    
    private Oeuvre currentOeuvre;
    private File selectedImageFile;
    private boolean saved = false;
    private boolean readOnly = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        categorieService = new CategorieService();
        userService = new UserService();
        
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
            artisteCombo.setDisable(true);
            
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
        // Categorie Combo display
        categorieCombo.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });

        // Artiste Combo display
        artisteCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String string) { return null; }
        });
        
        statutCombo.setValue("disponible");
    }

    private void loadData() {
        try {
            categorieCombo.setItems(FXCollections.observableArrayList(categorieService.recuperer()));
            artisteCombo.setItems(FXCollections.observableArrayList(userService.recuperer()));
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
            
            // Selectionner le bon artiste
            for (User u : artisteCombo.getItems()) {
                if (oeuvre.getUser() != null && u.getId() == oeuvre.getUser().getId()) {
                    artisteCombo.setValue(u);
                    break;
                }
            }

            if (oeuvre.getImage() != null && !oeuvre.getImage().isEmpty()) {
                File file = new File(oeuvre.getImage());
                if (file.exists()) {
                    updateImagePreview(file);
                }
            }
        }
    }

    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        
        // Ouvrir directement dans le dossier uploads
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) uploadsDir.mkdirs();
        fileChooser.setInitialDirectory(uploadsDir);
        
        // Accepter n'importe quel type d'image
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
        Image img = new Image(file.toURI().toString());
        imagePreview.setImage(img);
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
            currentOeuvre.setUser(artisteCombo.getValue());

            // Gérer l'upload d'image
            if (selectedImageFile != null) {
                String fileName = UUID.randomUUID().toString() + "_" + selectedImageFile.getName();
                File destDir = new File("uploads/oeuvres");
                if (!destDir.exists()) destDir.mkdirs();
                
                File destFile = new File(destDir, fileName);
                Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                currentOeuvre.setImage(destFile.getPath());
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
        clearErrors();
        boolean valid = true;
        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String prixStr = prixField.getText().trim();

        if (titre.isEmpty()) {
            showError(titreError, "Le titre est obligatoire.");
            valid = false;
        } else if (titre.length() < 2) {
            showError(titreError, "Le titre doit faire au moins 2 caractères.");
            valid = false;
        }

        if (description.isEmpty()) {
            showError(descriptionError, "La description est obligatoire.");
            valid = false;
        }

        if (prixStr.isEmpty()) {
            showError(prixError, "Le prix est obligatoire.");
            valid = false;
        } else {
            try {
                BigDecimal prix = new BigDecimal(prixStr);
                if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                    showError(prixError, "Le prix doit être supérieur à 0,00 DT.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showError(prixError, "Le prix doit être un nombre valide.");
                valid = false;
            }
        }

        if (categorieCombo.getValue() == null) {
            showError(categorieError, "Veuillez sélectionner une catégorie.");
            valid = false;
        }

        if (artisteCombo.getValue() == null) {
            showError(artisteError, "Veuillez sélectionner un artiste.");
            valid = false;
        }

        boolean hasImage = (selectedImageFile != null) || (currentOeuvre != null && currentOeuvre.getImage() != null && !currentOeuvre.getImage().isEmpty());
        if (!hasImage) {
            showError(imageError, "Veuillez sélectionner une image.");
            valid = false;
        }

        return valid;
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        titreError.setVisible(false);
        titreError.setManaged(false);
        descriptionError.setVisible(false);
        descriptionError.setManaged(false);
        prixError.setVisible(false);
        prixError.setManaged(false);
        categorieError.setVisible(false);
        categorieError.setManaged(false);
        artisteError.setVisible(false);
        artisteError.setManaged(false);
        imageError.setVisible(false);
        imageError.setManaged(false);
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
