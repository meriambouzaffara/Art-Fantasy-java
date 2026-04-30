package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import tn.rouhfan.services.PasswordResetService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.PasswordUtils;

import java.sql.SQLException;

/**
 * Contrôleur pour la page de réinitialisation du mot de passe.
 *
 * Supporte deux modes :
 * - Mode classique : reçoit email + token depuis ForgotPasswordController
 * - Mode Google : reçoit email + flag googleVerified (pas de token requis)
 *
 * Permet de saisir et confirmer un nouveau mot de passe.
 */
public class ResetPasswordController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label strengthLabel;

    private final PasswordResetService resetService = new PasswordResetService();
    private final UserService userService = new UserService();

    private String email;
    private String token;
    private boolean googleVerified = false;

    @FXML
    public void initialize() {
        messageLabel.setText("");
        strengthLabel.setText("");

        // Indicateur de force du mot de passe
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                strengthLabel.setText("");
            } else if (newVal.length() < 6) {
                strengthLabel.setText("⚠️ Trop court");
                strengthLabel.setStyle("-fx-text-fill: #d63031; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else if (!newVal.matches(".*[a-zA-Z].*") || !newVal.matches(".*\\d.*")) {
                strengthLabel.setText("🔶 Moyen — ajoutez lettres + chiffres");
                strengthLabel.setStyle("-fx-text-fill: #e17055; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else if (newVal.length() >= 8) {
                strengthLabel.setText("✅ Fort");
                strengthLabel.setStyle("-fx-text-fill: #00b894; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                strengthLabel.setText("🔵 Bon");
                strengthLabel.setStyle("-fx-text-fill: #0984e3; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Reçoit les données de reset depuis ForgotPasswordController.
     */
    public void setResetData(String email, String token) {
        this.email = email;
        this.token = token;
    }

    /**
     * Indique que l'utilisateur a été vérifié via Google OAuth.
     * Dans ce cas, pas besoin de valider le token classique.
     */
    public void setGoogleVerified(boolean googleVerified) {
        this.googleVerified = googleVerified;
    }

    @FXML
    private void handleReset(ActionEvent event) {
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        // Validation
        if (newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (newPwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!newPwd.matches(".*[a-zA-Z].*") || !newPwd.matches(".*\\d.*")) {
            showError("Le mot de passe doit contenir des lettres ET des chiffres.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            boolean success;

            if (googleVerified) {
                // ── Mode Google OAuth : mise à jour directe du mot de passe ──
                success = resetPasswordDirect(email, newPwd);
                if (success) {
                    AppLogger.auth("PASSWORD_RESET_VIA_GOOGLE", email);
                }
            } else {
                // ── Mode classique : validation du token puis reset ──
                success = resetService.resetPassword(email, token, newPwd);
                if (success) {
                    AppLogger.auth("PASSWORD_RESET_COMPLETE", email);
                }
            }

            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Mot de passe réinitialisé");
                alert.setHeaderText(null);
                alert.setContentText("✅ Votre mot de passe a été réinitialisé avec succès !\nVous pouvez maintenant vous connecter.");
                alert.showAndWait();

                navigateTo(event, "/ui/front/Login.fxml");
            } else {
                showError("Erreur: le token a expiré ou est invalide. Veuillez recommencer.");
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            AppLogger.error("[ResetPassword] Erreur reset", e);
        }
    }

    /**
     * Réinitialise le mot de passe directement (utilisé après vérification Google).
     * Pas besoin de valider un token car l'identité a déjà été prouvée via OAuth2.
     */
    private boolean resetPasswordDirect(String email, String newPassword) {
        try {
            var user = userService.findByEmail(email);
            if (user == null) return false;

            String hashedPassword = PasswordUtils.hashPassword(newPassword);
            userService.updatePasswordHash(user.getId(), hashedPassword);
            return true;
        } catch (SQLException e) {
            AppLogger.error("[ResetPassword] Erreur resetPasswordDirect", e);
            return false;
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateTo(event, "/ui/front/Login.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            AppLogger.error("Navigation error: " + fxmlPath, e);
        }
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #d63031; -fx-font-size: 13px; -fx-font-weight: 600;");
    }
}
