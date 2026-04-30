package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.rouhfan.services.EmailVerificationService;
import tn.rouhfan.tools.AppLogger;

/**
 * Contrôleur pour la page de vérification d'email.
 */
public class EmailVerificationController {

    @FXML private TextField tokenField;
    @FXML private Label messageLabel;
    @FXML private Label infoLabel;

    private final EmailVerificationService verificationService = new EmailVerificationService();
    private String email;
    private String userName;

    @FXML
    public void initialize() {
        messageLabel.setText("");
    }

    public void setVerificationData(String email, String userName) {
        this.email = email;
        this.userName = userName;
        if (infoLabel != null) {
            infoLabel.setText("Un code a été envoyé à " + email);
        }
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String token = tokenField.getText().trim();
        if (token.isEmpty() || !token.matches("\\d{6}")) {
            showError("Entrez un code de 6 chiffres.");
            return;
        }
        try {
            if (verificationService.verifyEmail(email, token)) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Compte vérifié");
                a.setHeaderText(null);
                a.setContentText("Votre compte est activé !");
                a.showAndWait();
                navigateTo(event, "/ui/front/Login.fxml");
            } else {
                showError("Code invalide.");
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleResend(ActionEvent event) {
        try {
            String t = verificationService.resendVerificationToken(email, userName);
            if (t != null) showSuccess("Nouveau code envoyé !");
            else showError("Impossible de renvoyer.");
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigateTo(event, "/ui/front/Login.fxml");
    }

    private void navigateTo(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage s = (Stage) ((Node) event.getSource()).getScene().getWindow();
            s.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String m) {
        messageLabel.setText(m);
        messageLabel.setStyle("-fx-text-fill:#d63031;-fx-font-weight:600;");
    }

    private void showSuccess(String m) {
        messageLabel.setText(m);
        messageLabel.setStyle("-fx-text-fill:#00b894;-fx-font-weight:600;");
    }
}
