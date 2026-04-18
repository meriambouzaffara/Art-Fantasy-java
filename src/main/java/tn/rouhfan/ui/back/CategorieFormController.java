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
                File file = new File(categorie.getImageCategorie());
                if (file.exists()) {
                    updateImagePreview(file);
                }
            }
        }
    }

    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de catégorie");
        
        // Ouvrir directement dans le dossier uploads
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) uploadsDir.mkdirs();
        fileChooser.setInitialDirectory(uploadsDir);

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
        Image img = new Image(file.toURI().toString());
        imagePreview.setImage(img);
        placeholderLabel.setVisible(false);
    }

    @FXML
    private void save(ActionEvent event) {
        if (!validateFields()) return;

        try {
            if (currentCategorie == null) currentCategorie = new Categorie();
            
            currentCategorie.setNomCategorie(nomField.getText());

            // Gérer l'upload d'image
            if (selectedImageFile != null) {
                String fileName = UUID.randomUUID().toString() + "_" + selectedImageFile.getName();
                File destDir = new File("uploads/categories");
                if (!destDir.exists()) destDir.mkdirs();
                
                File destFile = new File(destDir, fileName);
                Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                currentCategorie.setImageCategorie(destFile.getPath());
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
