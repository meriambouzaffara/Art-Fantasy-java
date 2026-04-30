package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.services.GoogleOAuthService;
import tn.rouhfan.services.PasswordResetService;
import tn.rouhfan.tools.AppLogger;

/**
 * Contrôleur pour la page "Mot de passe oublié".
 *
 * Deux modes de vérification :
 * ─────────────────────────────
 * Mode 1 (Email) :
 *   Étape 1 : Saisir l'email → envoyer un code 6 chiffres
 *   Étape 2 : Saisir le code → naviguer vers ResetPassword
 *
 * Mode 2 (Google OAuth) :
 *   Étape 1 : Saisir l'email
 *   Étape 2 : Cliquer "Vérifier via Google" → prouver la propriété de l'email
 *   Étape 3 : Si l'email Google correspond → naviguer vers ResetPassword
 */
public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private VBox tokenBox;
    @FXML private TextField tokenField;
    @FXML private Label messageLabel;
    @FXML private Button actionButton;
    @FXML private Button googleVerifyButton;

    private final PasswordResetService resetService = new PasswordResetService();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();

    private boolean codeSent = false;
    private String currentEmail;

    @FXML
    public void initialize() {
        messageLabel.setText("");

        // Afficher ou masquer le bouton Google selon la configuration
        if (googleVerifyButton != null) {
            googleVerifyButton.setVisible(googleOAuthService.isConfigured());
            googleVerifyButton.setManaged(googleOAuthService.isConfigured());
        }
    }

    @FXML
    private void handleAction(ActionEvent event) {
        if (!codeSent) {
            handleSendCode(event);
        } else {
            handleVerifyCode(event);
        }
    }

    // ═══════════════════════════════════════
    //  Mode 1 : Vérification par code email
    // ═══════════════════════════════════════

    /**
     * Étape 1 : Envoyer le code de réinitialisation.
     */
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse e-mail.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse e-mail invalide.");
            return;
        }

        try {
            boolean sent = resetService.generateResetToken(email);
            if (sent) {
                currentEmail = email;
                codeSent = true;

                // Afficher le champ de code
                tokenBox.setVisible(true);
                tokenBox.setManaged(true);
                emailField.setDisable(true);
                actionButton.setText("Vérifier le code");

                // Masquer le bouton Google une fois le code envoyé
                if (googleVerifyButton != null) {
                    googleVerifyButton.setVisible(false);
                    googleVerifyButton.setManaged(false);
                }

                showSuccess("✅ Un code de réinitialisation a été envoyé à " + email);
            } else {
                showError("Aucun compte trouvé avec cette adresse e-mail.");
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            AppLogger.error("[ForgotPassword] Erreur envoi code", e);
        }
    }

    /**
     * Étape 2 : Vérifier le code et naviguer vers ResetPassword.
     */
    private void handleVerifyCode(ActionEvent event) {
        String token = tokenField.getText().trim();

        if (token.isEmpty()) {
            showError("Veuillez entrer le code reçu par email.");
            return;
        }

        if (!token.matches("\\d{6}")) {
            showError("Le code doit contenir 6 chiffres.");
            return;
        }

        try {
            boolean valid = resetService.validateResetToken(currentEmail, token);
            if (valid) {
                // Naviguer vers la page de réinitialisation
                navigateToResetPassword(event, currentEmail, token);
            } else {
                showError("Code invalide ou expiré. Veuillez réessayer.");
            }
        } catch (Exception e) {
            showError("Erreur de vérification: " + e.getMessage());
            AppLogger.error("[ForgotPassword] Erreur vérification code", e);
        }
    }

    // ═══════════════════════════════════════
    //  Mode 2 : Vérification via Google OAuth
    // ═══════════════════════════════════════

    /**
     * Ouvre une fenêtre Google OAuth pour vérifier la propriété de l'email.
     * Si l'email Google correspond à l'email saisi, l'utilisateur est redirigé
     * directement vers la page de réinitialisation (sans code 6 chiffres).
     */
    @FXML
    private void handleGoogleVerify(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez d'abord entrer votre adresse e-mail.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse e-mail invalide.");
            return;
        }

        if (!googleOAuthService.isConfigured()) {
            showError("Google OAuth n'est pas configuré.");
            return;
        }

        try {
            // ── Ouvrir la fenêtre OAuth ──
            Stage oauthStage = new Stage();
            oauthStage.initModality(Modality.APPLICATION_MODAL);
            oauthStage.setTitle("Vérification Google — Rouh el Fann");

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            VBox root = new VBox(0, webView);
            root.setPadding(new Insets(0));
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root, 500, 600);
            oauthStage.setScene(scene);

            // ── Charger l'URL d'autorisation ──
            String authUrl = googleOAuthService.getAuthorizationUrl();
            webEngine.load(authUrl);

            // ── Intercepter le code d'autorisation ──
            webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
                if (newTitle != null && newTitle.contains("code=")) {
                    String code = extractCode(newTitle);
                    if (code != null) {
                        oauthStage.close();
                        completeGoogleVerification(code, email, event);
                    }
                } else if (newTitle != null && newTitle.contains("error")) {
                    oauthStage.close();
                    Platform.runLater(() -> showError("Vérification Google annulée."));
                }
            });

            webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.contains("code=")) {
                    String code = extractCodeFromUrl(newUrl);
                    if (code != null) {
                        oauthStage.close();
                        completeGoogleVerification(code, email, event);
                    }
                } else if (newUrl != null && newUrl.contains("error")) {
                    oauthStage.close();
                    Platform.runLater(() -> showError("Vérification Google annulée."));
                }
            });

            oauthStage.show();

        } catch (Exception e) {
            showError("Erreur Google OAuth: " + e.getMessage());
            AppLogger.error("[ForgotPassword] Erreur Google OAuth", e);
        }
    }

    /**
     * Vérifie que l'email Google correspond à l'email saisi,
     * puis génère un token interne et navigue vers ResetPassword.
     */
    private void completeGoogleVerification(String authorizationCode, String email, ActionEvent event) {
        Platform.runLater(() -> showSuccess("🔄 Vérification Google en cours..."));

        new Thread(() -> {
            try {
                boolean verified = googleOAuthService.verifyEmailOwnership(authorizationCode, email);

                if (verified) {
                    // Générer un token interne pour le flux de reset
                    boolean tokenGenerated = resetService.generateResetToken(email);

                    if (tokenGenerated) {
                        AppLogger.auth("GOOGLE_VERIFY_FOR_RESET", email);

                        Platform.runLater(() -> {
                            showSuccess("✅ Identité vérifiée via Google !");

                            // Passer directement à la page de reset
                            // On génère un token spécial "GOOGLE_VERIFIED" qui sera accepté
                            try {
                                // Récupérer le token généré en base pour cet email
                                // puis naviguer vers ResetPassword
                                FXMLLoader loader = new FXMLLoader(
                                        getClass().getResource("/ui/front/ResetPassword.fxml"));
                                Parent root = loader.load();

                                ResetPasswordController controller = loader.getController();
                                // Utiliser le token généré par generateResetToken()
                                // Il a été stocké en base, on le récupère via la validation
                                controller.setResetData(email, "GOOGLE_VERIFIED");
                                controller.setGoogleVerified(true);

                                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                stage.getScene().setRoot(root);
                            } catch (Exception e) {
                                showError("Erreur navigation: " + e.getMessage());
                            }
                        });
                    } else {
                        Platform.runLater(() -> showError("Aucun compte trouvé avec cette adresse e-mail."));
                    }
                } else {
                    Platform.runLater(() ->
                            showError("❌ L'email Google ne correspond pas à l'email saisi.\n"
                                    + "Veuillez utiliser le même email."));
                }

            } catch (GoogleOAuthService.GoogleOAuthException e) {
                Platform.runLater(() -> showError("Erreur Google: " + e.getMessage()));
                AppLogger.error("[ForgotPassword] Google verification failed", e);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
                AppLogger.error("[ForgotPassword] Error during Google verification", e);
            }
        }).start();
    }

    // ═══════════════════════════════════════
    //  Navigation
    // ═══════════════════════════════════════

    /**
     * Navigue vers la page ResetPassword avec les paramètres email/token.
     */
    private void navigateToResetPassword(ActionEvent event, String email, String token) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/ResetPassword.fxml"));
            Parent root = loader.load();

            // Passer les paramètres au contrôleur
            ResetPasswordController controller = loader.getController();
            controller.setResetData(email, token);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur de navigation: " + e.getMessage());
            AppLogger.error("[ForgotPassword] Erreur navigation ResetPassword", e);
        }
    }

    @FXML
    private void goLogin(ActionEvent event) {
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

    // ═══════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════

    private String extractCode(String title) {
        int idx = title.indexOf("code=");
        if (idx >= 0) {
            String code = title.substring(idx + 5);
            int ampIdx = code.indexOf("&");
            return ampIdx > 0 ? code.substring(0, ampIdx) : code;
        }
        return null;
    }

    private String extractCodeFromUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("code=")) {
                        return java.net.URLDecoder.decode(
                                param.substring(5), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {}
        return extractCode(url);
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #d63031; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #00b894; -fx-font-size: 13px; -fx-font-weight: 600;");
    }
}
