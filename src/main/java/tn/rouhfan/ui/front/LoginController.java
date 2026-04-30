package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.GoogleOAuthService;
import tn.rouhfan.services.LoginLogService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.SessionManager;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();
    private final LoginLogService loginLogService = new LoginLogService();
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();

    @FXML
    public void initialize() {
        // Initialisation si nécessaire
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        // Vérifier si l'utilisateur est bloqué (Brute Force Protection : 5 échecs en 15 min)
        if (loginLogService.countRecentFailures(email, 15) >= 5) {
            errorLabel.setText("Compte temporairement bloqué (trop de tentatives). Réessayez dans 15 minutes.");
            return;
        }

        try {
            User user = userService.authenticate(email, password);

            if (user != null) {
                if (!user.isVerified()) {
                    errorLabel.setText("Veuillez vérifier votre email avant de vous connecter.");
                    return;
                }

                SessionManager.getInstance().login(user);
                loginLogService.logSuccess(user.getId(), email);
                userService.updateLastLogin(user.getId());

                // Redirection selon le rôle
                String role = SessionManager.getInstance().getRole();
                if (role != null && role.toUpperCase().contains("ADMIN")) {
                    navigateTo(event, "/ui/back/BackBase.fxml");
                } else {
                    navigateTo(event, "/ui/front/FrontBase.fxml");
                }
            } else {
                loginLogService.logFailure(email, "Email ou mot de passe incorrect");
                errorLabel.setText("Email ou mot de passe incorrect.");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion: " + e.getMessage());
            AppLogger.error("[LoginController] Erreur authenticate", e);
        }
    }

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        errorLabel.setText("Ouverture de la fenêtre Google...");
        
        googleOAuthService.openLoginPopup(new GoogleOAuthService.OAuthCallback() {
            @Override
            public void onSuccess(User user) {
                Platform.runLater(() -> {
                    errorLabel.setText("Connecté avec succès !");
                    loginLogService.logSuccess(user.getId(), user.getEmail());
                    
                    String role = SessionManager.getInstance().getRole();
                    if (role != null && role.toUpperCase().contains("ADMIN")) {
                        navigateTo(event, "/ui/back/BackBase.fxml");
                    } else {
                        navigateTo(event, "/ui/front/FrontBase.fxml");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> errorLabel.setText("Erreur Google : " + error));
            }

            @Override
            public void onCancel() {
                Platform.runLater(() -> errorLabel.setText("Authentification Google annulée."));
            }
        });
    }

    @FXML
    private void handleFacebookLogin(ActionEvent event) {
        errorLabel.setText("Facebook login non implémenté.");
    }

    @FXML
    private void handleGithubLogin(ActionEvent event) {
        errorLabel.setText("GitHub login non implémenté.");
    }

    @FXML
    private void handleFaceLogin(ActionEvent event) {
        navigateTo(event, "/ui/front/FaceLogin.fxml");
    }

    @FXML
    private void openChatbot(ActionEvent event) {
        navigateTo(event, "/ui/front/Chatbot.fxml");
    }

    @FXML
    private void goBack(ActionEvent event) {
        navigateTo(event, "/ui/front/FrontBase.fxml");
    }

    @FXML
    private void goForgotPassword(ActionEvent event) {
        navigateTo(event, "/ui/front/ForgotPassword.fxml");
    }

    @FXML
    private void goSignUp(ActionEvent event) {
        navigateTo(event, "/ui/front/SignUp.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                errorLabel.setText("Page introuvable: " + fxmlPath);
                AppLogger.error("FXML introuvable: " + fxmlPath, null);
                return;
            }
            Parent root = FXMLLoader.load(resource);
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            AppLogger.error("Erreur de navigation vers " + fxmlPath, e);
            errorLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}