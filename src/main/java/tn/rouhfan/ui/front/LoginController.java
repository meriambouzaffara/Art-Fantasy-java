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

        // Validation des champs
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            // Chercher l'utilisateur par email
            User user = userService.findByEmail(email);

            if (user == null) {
                errorLabel.setText("Aucun compte trouvé avec cet e-mail.");
                return;
            }

            // Vérifier le mot de passe (supporte bcrypt ET texte clair)
            if (!PasswordUtils.checkPassword(password, user.getPassword())) {
                errorLabel.setText("Mot de passe incorrect.");
                return;
            }

            // Login réussi — enregistrer dans la session
            SessionManager.getInstance().login(user);
            String role = SessionManager.getInstance().getRole();
            System.out.println("Login réussi pour: " + user.getNom() + " " + user.getPrenom() + " | Rôle: " + role);

            // Redirection vers la page d'accueil (connecté)
            // L'utilisateur verra "Bienvenue, Prénom" + bouton Mon Espace
            navigateTo(event, "/ui/front/FrontBase.fxml");

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
