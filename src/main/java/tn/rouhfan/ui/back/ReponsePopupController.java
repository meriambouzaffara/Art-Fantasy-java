package tn.rouhfan.ui.back;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReponseReclamationService;

import java.sql.SQLException;
import java.util.Date;

public class ReponsePopupController {

    @FXML private TextArea messageField;

    private int reclamationId;

    private ReponseReclamationService service = new ReponseReclamationService();

    // 🔥 recevoir l'id de la réclamation
    public void setReclamationId(int id) {
        this.reclamationId = id;
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
            showAlert("Succès", "Réponse envoyée !");

            // fermer popup
            Stage stage = (Stage) messageField.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}