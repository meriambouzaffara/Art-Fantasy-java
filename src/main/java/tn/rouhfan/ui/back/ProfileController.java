package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.PasswordUtils;
import tn.rouhfan.tools.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la vue de gestion du profil personnel.
 * Chaque utilisateur peut voir et modifier SES PROPRES informations.
 */
public class ProfileController implements Initializable {

    // Informations du profil
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;

    // Informations en lecture seule
    @FXML private Label roleLabel;
    @FXML private Label statutLabel;
    @FXML private Label typeLabel;
    @FXML private Label dateCreationLabel;

    // Changement de mot de passe
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Messages
    @FXML private Label profileMessage;
    @FXML private Label passwordMessage;

    private final UserService userService = new UserService();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        loadProfileData();
    }

    /**
     * Charge les données du profil depuis l'utilisateur connecté.
     */
    private void loadProfileData() {
        // Recharger les données fraîches depuis la BD
        try {
            currentUser = userService.findById(currentUser.getId());
            SessionManager.getInstance().login(currentUser); // Mettre à jour la session
        } catch (SQLException e) {
            System.err.println("Erreur lors du rechargement du profil: " + e.getMessage());
        }

        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());

        // Infos lecture seule
        String role = SessionManager.getInstance().getRole();
        String roleDisplay;
        switch (role) {
            case "ROLE_ADMIN": roleDisplay = "👑 Administrateur"; break;
            case "ROLE_ARTISTE": roleDisplay = "🎨 Artiste"; break;
            case "ROLE_PARTICIPANT": roleDisplay = "🎭 Participant"; break;
            default: roleDisplay = role;
        }
        roleLabel.setText(roleDisplay);
        statutLabel.setText(currentUser.getStatut() != null ? currentUser.getStatut() : "—");
        typeLabel.setText(currentUser.getType() != null ? currentUser.getType() : "—");

        if (currentUser.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
            dateCreationLabel.setText(sdf.format(currentUser.getCreatedAt()));
        } else {
            dateCreationLabel.setText("—");
        }

        clearMessages();
    }

    /**
     * Sauvegarde les modifications du profil (nom, prénom, email).
     */
    @FXML
    private void handleSaveProfile(ActionEvent event) {
        clearMessages();

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();

        // Validation
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showProfileError("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showProfileError("Adresse e-mail invalide.");
            return;
        }

        // Vérifier si l'email est déjà utilisé par un autre utilisateur
        try {
            User existingUser = userService.findByEmail(email);
            if (existingUser != null && existingUser.getId() != currentUser.getId()) {
                showProfileError("Cet e-mail est déjà utilisé par un autre compte.");
                return;
            }

            // Mettre à jour le profil
            currentUser.setNom(nom);
            currentUser.setPrenom(prenom);
            currentUser.setEmail(email);
            userService.modifierProfil(currentUser);

            // Mettre à jour la session
            SessionManager.getInstance().login(currentUser);

            showProfileSuccess("✅ Profil mis à jour avec succès !");
        } catch (SQLException e) {
            showProfileError("Erreur: " + e.getMessage());
        }
    }

    /**
     * Change le mot de passe après vérification de l'ancien.
     */
    @FXML
    private void handleChangePassword(ActionEvent event) {
        clearMessages();

        String currentPwd = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        // Validation
        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showPasswordError("Veuillez remplir tous les champs de mot de passe.");
            return;
        }

        // Vérifier l'ancien mot de passe
        if (!PasswordUtils.checkPassword(currentPwd, currentUser.getPassword())) {
            showPasswordError("Le mot de passe actuel est incorrect.");
            return;
        }

        if (newPwd.length() < 6) {
            showPasswordError("Le nouveau mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showPasswordError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            userService.modifierPassword(currentUser.getId(), newPwd);

            // Mettre à jour le mot de passe dans l'objet current user et la session
            currentUser.setPassword(PasswordUtils.hashPassword(newPwd));
            SessionManager.getInstance().login(currentUser);

            // Vider les champs
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showPasswordSuccess("✅ Mot de passe modifié avec succès !");
        } catch (SQLException e) {
            showPasswordError("Erreur: " + e.getMessage());
        }
    }

    private void showProfileError(String msg) {
        profileMessage.setText(msg);
        profileMessage.setStyle("-fx-text-fill: #d63031; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void showProfileSuccess(String msg) {
        profileMessage.setText(msg);
        profileMessage.setStyle("-fx-text-fill: #00b894; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void showPasswordError(String msg) {
        passwordMessage.setText(msg);
        passwordMessage.setStyle("-fx-text-fill: #d63031; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void showPasswordSuccess(String msg) {
        passwordMessage.setText(msg);
        passwordMessage.setStyle("-fx-text-fill: #00b894; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void clearMessages() {
        if (profileMessage != null) profileMessage.setText("");
        if (passwordMessage != null) passwordMessage.setText("");
    }

    @FXML
    private void handleViewHistorique(ActionEvent event) {
        HistoriqueParticipationsDialog dialog = new HistoriqueParticipationsDialog(currentUser.getId());
        dialog.show();
    }
}