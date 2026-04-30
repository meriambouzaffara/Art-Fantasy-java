package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.EmailVerificationService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.PasswordUtils;

/**
 * Contrôleur d'inscription enrichi :
 * - Validation forte du mot de passe
 * - Envoi email de vérification
 * - Le compte est créé avec isVerified=false
 */
public class SignUpController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();
    private final EmailVerificationService verificationService = new EmailVerificationService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
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

        // Validation nom/prénom
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }
        if (nom.length() < 2 || !nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
            errorLabel.setText("Nom invalide (min 2 lettres).");
            return;
        }
        if (prenom.length() < 2 || !prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
            errorLabel.setText("Prénom invalide (min 2 lettres).");
            return;
        }

        // Validation email
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Adresse e-mail invalide.");
            return;
        }

        // Validation password forte
        if (password.length() < 6) {
            errorLabel.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            errorLabel.setText("Le mot de passe doit contenir au moins une lettre.");
            return;
        }
        if (!password.matches(".*\\d.*")) {
            errorLabel.setText("Le mot de passe doit contenir au moins un chiffre.");
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
            User existing = userService.findByEmail(email);
            if (existing != null) {
                errorLabel.setText("Un compte avec cet e-mail existe déjà.");
                return;
            }

            String role;
            String typeValue;
            switch (type) {
                case "Artiste":
                    role = "[\"ROLE_ARTISTE\"]";
                    typeValue = "artiste";
                    break;
                default:
                    role = "[\"ROLE_PARTICIPANT\"]";
                    typeValue = "participant";
                    break;
            }

            // Créer le compte avec isVerified=false
            String hashedPassword = PasswordUtils.hashPassword(password);
            User newUser = new User(nom, prenom, email, hashedPassword, role, "actif", false, typeValue);
            userService.ajouter(newUser);

            AppLogger.auth("SIGNUP", email + " | Type: " + typeValue);

            // Envoyer email de vérification
            String userName = prenom + " " + nom;
            verificationService.generateVerificationToken(newUser.getId(), email, userName);

            // Naviguer vers la page de vérification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/EmailVerification.fxml"));
            Parent root = loader.load();
            EmailVerificationController controller = loader.getController();
            controller.setVerificationData(email, userName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            errorLabel.setText("Erreur: " + e.getMessage());
            AppLogger.error("[SignUp] Erreur inscription", e);
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