package tn.rouhfan.ui.back;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ReponsePopupController {

    @FXML private TextArea messageField;
    @FXML private Label fileLabel;

    private int reclamationId;
    private File selectedImageFile = null;

    private ReponseReclamationService service = new ReponseReclamationService();

    // 🔥 recevoir l'id de la réclamation
    public void setReclamationId(int id) {
        this.reclamationId = id;

        // Logique de réponse automatique intelligente
        try {
            ReclamationService reclamationService = new ReclamationService();
            Reclamation currentRec = reclamationService.findById(id);
            if (currentRec != null) {
                String category = currentRec.getCategorie();
                if (category != null && !category.equals("Autre")) {
                    List<Reclamation> allRecs = reclamationService.recuperer();
                    for (Reclamation rec : allRecs) {
                        if (rec.getId() != id && category.equals(rec.getCategorie())) {
                            List<ReponseReclamation> responses = service.getByReclamation(rec.getId());
                            if (!responses.isEmpty()) {
                                messageField.setText(responses.get(0).getMessage());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) messageField.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            fileLabel.setText("Fichier : " + selectedImageFile.getName());
            fileLabel.setVisible(true);
            fileLabel.setManaged(true);
        } else {
            fileLabel.setText("Aucune image sélectionnée");
            fileLabel.setVisible(false);
            fileLabel.setManaged(false);
        }
    }

    @FXML
    private void envoyerReponse() {

        String message = messageField.getText();

        if (message == null || message.isEmpty()) {
            showAlert("Erreur", "Message vide !");
            return;
        }

        ReponseReclamation rr = new ReponseReclamation(
                message,
                new Date(),
                reclamationId
        );

        try {
            service.ajouter(rr);

            // 🔥 Passer le statut à 'traite'
            ReclamationService reclamationService = new ReclamationService();
            reclamationService.modifierStatut(reclamationId, "traite");

            // Note: l'image est sélectionnée mais pas enregistrée en base de données
            // selon la demande (ne pas modifier la base de données).
            // Si nécessaire, on pourrait copier selectedImageFile dans un dossier serveur ici.

            showAlert("Succès", "Réponse envoyée !");

            // fermer popup
            fermerPopup();

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void fermerPopup() {
        Stage stage = (Stage) messageField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}