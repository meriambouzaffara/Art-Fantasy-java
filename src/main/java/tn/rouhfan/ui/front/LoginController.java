package tn.rouhfan.ui.front;

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
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.PasswordUtils;
import tn.rouhfan.tools.SessionManager;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            User user = userService.findByEmail(email);

            if (user == null) {
                errorLabel.setText("Aucun compte trouvé avec cet e-mail.");
                return;
            }

            if (!PasswordUtils.checkPassword(password, user.getPassword())) {
                errorLabel.setText("Mot de passe incorrect.");
                return;
            }

            // Login réussi
            SessionManager.getInstance().login(user);
            String role = SessionManager.getInstance().getRole();
            System.out.println("Login réussi pour: " + user.getNom() + " " + user.getPrenom() + " | Rôle: " + role);

            // Redirection selon le rôle
            if (role != null && role.toUpperCase().contains("ADMIN")) {
                // ADMIN → Dashboard directement
                navigateTo(event, "/ui/back/BackBase.fxml");
            } else {
                // ARTISTE ou PARTICIPANT → FrontOffice
                navigateTo(event, "/ui/front/FrontBase.fxml");
            }

        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goSignUp(ActionEvent event) {
        navigateTo(event, "/ui/front/SignUp.fxml");
    }

    @FXML
    private void goBack(ActionEvent event) {
        navigateTo(event, "/ui/front/FrontBase.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Navigation error: " + fxmlPath);
            e.printStackTrace();
        }
    }
}