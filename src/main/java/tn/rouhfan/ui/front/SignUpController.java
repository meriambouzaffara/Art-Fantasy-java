package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.PasswordUtils;

public class SignUpController {

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> typeCombo;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        // Options de rôle à l'inscription (Admin n'est pas disponible à l'inscription)
        typeCombo.setItems(FXCollections.observableArrayList("Artiste", "Participant"));
        typeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String type = typeCombo.getValue();

        // Validation
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Veuillez entrer une adresse e-mail valide.");
            return;
        }

        if (password.length() < 6) {
            errorLabel.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        if (type == null || type.isEmpty()) {
            errorLabel.setText("Veuillez choisir un type de profil.");
            return;
        }

        try {
            // Vérifier si l'email existe déjà
            User existing = userService.findByEmail(email);
            if (existing != null) {
                errorLabel.setText("Un compte avec cet e-mail existe déjà.");
                return;
            }

            // Déterminer le rôle selon le type choisi
            String role;
            String typeValue;
            switch (type) {
                case "Artiste":
                    role = "[\"ROLE_ARTISTE\"]";
                    typeValue = "artiste";
                    break;
                case "Participant":
                    role = "[\"ROLE_PARTICIPANT\"]";
                    typeValue = "participant";
                    break;
                default:
                    role = "[\"ROLE_PARTICIPANT\"]";
                    typeValue = "participant";
                    break;
            }

            // Créer et sauvegarder l'utilisateur (password hashé avec BCrypt)
            String hashedPassword = PasswordUtils.hashPassword(password);
            User newUser = new User(nom, prenom, email, hashedPassword, role, "actif", true, typeValue);
            userService.ajouter(newUser);

            System.out.println("Inscription réussie pour: " + nom + " " + prenom + " | Rôle: " + role);

            // Afficher succès et naviguer vers login
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Inscription réussie");
            alert.setHeaderText(null);
            alert.setContentText("Votre compte a été créé avec succès !\nVous pouvez maintenant vous connecter.");
            alert.showAndWait();

            navigateTo(event, "/ui/front/Login.fxml");

        } catch (Exception e) {
            errorLabel.setText("Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goLogin(ActionEvent event) {
        navigateTo(event, "/ui/front/Login.fxml");
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