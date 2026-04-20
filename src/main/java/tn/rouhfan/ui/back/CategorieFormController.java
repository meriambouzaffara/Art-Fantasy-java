package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.services.CategorieService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.UUID;

public class CategorieFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private ImageView imagePreview;
    @FXML private Label placeholderLabel;
    @FXML private StackPane imageContainer;
    @FXML private Label nomErrorLabel;
    @FXML private Label imageErrorLabel;

    private CategorieService categorieService;
    private Categorie currentCategorie;
    private File selectedImageFile;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categorieService = new CategorieService();
    }

    public void setCategorie(Categorie categorie) {
        this.currentCategorie = categorie;
        if (categorie != null) {
            formTitle.setText("✏️ Modifier la catégorie");
            nomField.setText(categorie.getNomCategorie());
            
            if (categorie.getImageCategorie() != null && !categorie.getImageCategorie().isEmpty()) {
                String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(categorie.getImageCategorie());
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
        fileChooser.setTitle("Choisir une image de catégorie");
        
        // Ouvrir directement dans le dossier uploads/categories
        File initialDir = new File(tn.rouhfan.tools.ImageUtils.UPLOADS_DIR + "/categories");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        // Accepter n'importe quel type d'image
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(nomField.getScene().getWindow());
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
            if (currentCategorie == null) currentCategorie = new Categorie();
            
            currentCategorie.setNomCategorie(nomField.getText());

            // Gérer l'upload d'image via ImageUtils
            if (selectedImageFile != null) {
                String dbPath = tn.rouhfan.tools.ImageUtils.saveUpload(selectedImageFile, "categories");
                currentCategorie.setImageCategorie(dbPath);
            }

            if (currentCategorie.getIdCategorie() == 0) {
                categorieService.ajouter(currentCategorie);
            } else {
                categorieService.modifier(currentCategorie);
            }

            saved = true;
            closeDialog();

        } catch (SQLException | IOException e) {
            showAlert("Erreur", "Impossible d'enregistrer: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        boolean isValid = true;
        
        // Hide errors initially
        nomErrorLabel.setVisible(false);
        nomErrorLabel.setManaged(false);
        imageErrorLabel.setVisible(false);
        imageErrorLabel.setManaged(false);

        String nom = nomField.getText().trim();

        if (nom.isEmpty()) {
            nomErrorLabel.setText("Le nom de la catégorie est obligatoire.");
            nomErrorLabel.setVisible(true);
            nomErrorLabel.setManaged(true);
            isValid = false;
        } else if (nom.length() < 2) {
            nomErrorLabel.setText("Le nom de la catégorie doit faire au moins 2 caractères.");
            nomErrorLabel.setVisible(true);
            nomErrorLabel.setManaged(true);
            isValid = false;
        } else {
            try {
                int excludeId = (currentCategorie != null) ? currentCategorie.getIdCategorie() : 0;
                if (categorieService.isNomExiste(nom, excludeId)) {
                    nomErrorLabel.setText("Cette catégorie existe déjà.");
                    nomErrorLabel.setVisible(true);
                    nomErrorLabel.setManaged(true);
                    isValid = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        boolean hasImage = (selectedImageFile != null) || (currentCategorie != null && currentCategorie.getImageCategorie() != null && !currentCategorie.getImageCategorie().isEmpty());
        if (!hasImage) {
            imageErrorLabel.setText("Veuillez sélectionner une image.");
            imageErrorLabel.setVisible(true);
            imageErrorLabel.setManaged(true);
            isValid = false;
        }

        javafx.application.Platform.runLater(() -> {
            if (nomField.getScene() != null && nomField.getScene().getWindow() != null) {
                ((javafx.stage.Stage) nomField.getScene().getWindow()).sizeToScene();
            }
        });

        return isValid;
    }

    @FXML private void cancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) nomField.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
