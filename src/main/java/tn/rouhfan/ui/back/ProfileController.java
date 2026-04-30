package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import tn.rouhfan.tools.AppLogger;
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
    @FXML private Label dateCreationLabel;
    
    // Nouveaux éléments UI
    @FXML private Label userNameTitle;
    @FXML private Label verifiedBadge;
    @FXML private ImageView profileImageView;

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
        
        // Titre et Badge de vérification
        userNameTitle.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        verifiedBadge.setVisible(currentUser.isVerified());

        // Chargement de la photo
        loadProfileImage();

        if (currentUser.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
            dateCreationLabel.setText(sdf.format(currentUser.getCreatedAt()));
        } else {
            dateCreationLabel.setText("—");
        }

        clearMessages();

        // Initialiser le statut de la reconnaissance faciale
        initFaceStatus();
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

    /**
     * Gère l'upload de la photo de profil.
     */
    @FXML
    private void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(nomField.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Créer le dossier uploads s'il n'existe pas
                File uploadDir = new File("uploads/profiles");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                // Nouveau nom de fichier unique
                String fileName = "profile_" + currentUser.getId() + "_" + System.currentTimeMillis() + 
                                 selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                File destFile = new File(uploadDir, fileName);

                // Copier le fichier
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Mettre à jour l'utilisateur
                currentUser.setPhotoProfile(destFile.getPath());
                userService.modifierProfil(currentUser);
                
                // Rafraîchir l'UI
                loadProfileImage();
                showProfileSuccess("✅ Photo mise à jour !");
                
            } catch (Exception e) {
                AppLogger.error("Erreur upload photo: " + e.getMessage());
                showProfileError("Erreur lors de l'upload de la photo.");
            }
        }
    }

    private void loadProfileImage() {
        if (currentUser.getPhotoProfile() != null && !currentUser.getPhotoProfile().isEmpty()) {
            File imgFile = new File(currentUser.getPhotoProfile());
            if (imgFile.exists()) {
                try {
                    profileImageView.setImage(new Image(new FileInputStream(imgFile)));
                    return;
                } catch (FileNotFoundException ignored) {}
            }
        }
        // Image par défaut si aucune photo ou erreur
        profileImageView.setImage(new Image(getClass().getResourceAsStream("/ui/placeholder-user.png")));
    }

    // ═══════════════════════════════════════
    //  RECONNAISSANCE FACIALE
    // ═══════════════════════════════════════

    @FXML private Label faceStatusLabel;
    @FXML private javafx.scene.layout.VBox faceCameraBox;
    @FXML private ImageView facePreview;
    @FXML private Label faceDetectionLabel;
    @FXML private javafx.scene.control.Button enrollFaceBtn;
    @FXML private javafx.scene.control.Button disableFaceBtn;
    @FXML private javafx.scene.control.Button captureFaceBtn;
    @FXML private javafx.scene.control.Button cancelFaceBtn;

    private tn.rouhfan.services.FaceRecognitionService faceRecognitionService;
    private tn.rouhfan.services.FaceCameraService faceCameraService;

    /**
     * Initialise l'affichage du statut face login.
     * Appelé depuis loadProfileData().
     */
    private void initFaceStatus() {
        if (faceStatusLabel == null) return; // FXML pas encore chargé

        if (currentUser.isFaceEnabled()) {
            faceStatusLabel.setText("✅ Reconnaissance faciale activée — Vous pouvez vous connecter avec votre visage.");
            faceStatusLabel.setStyle("-fx-text-fill: #00b894; -fx-font-weight: 600;");
            enrollFaceBtn.setVisible(false);
            enrollFaceBtn.setManaged(false);
            disableFaceBtn.setVisible(true);
            disableFaceBtn.setManaged(true);
        } else {
            faceStatusLabel.setText("⚠️ Reconnaissance faciale désactivée — Enregistrez votre visage pour activer le login facial.");
            faceStatusLabel.setStyle("-fx-text-fill: #5a4a72;");
            enrollFaceBtn.setVisible(true);
            enrollFaceBtn.setManaged(true);
            disableFaceBtn.setVisible(false);
            disableFaceBtn.setManaged(false);
        }
    }

    @FXML
    private void handleEnrollFace(ActionEvent event) {
        // Initialiser OpenCV
        faceRecognitionService = new tn.rouhfan.services.FaceRecognitionService();
        if (!faceRecognitionService.initialize()) {
            faceStatusLabel.setText("❌ Erreur: Impossible d'initialiser OpenCV. Vérifiez l'installation.");
            faceStatusLabel.setStyle("-fx-text-fill: #d63031;");
            return;
        }

        faceCameraService = new tn.rouhfan.services.FaceCameraService(faceRecognitionService);

        // Afficher la zone caméra
        faceCameraBox.setVisible(true);
        faceCameraBox.setManaged(true);
        enrollFaceBtn.setVisible(false);
        enrollFaceBtn.setManaged(false);
        captureFaceBtn.setVisible(true);
        captureFaceBtn.setManaged(true);
        captureFaceBtn.setDisable(true);
        cancelFaceBtn.setVisible(true);
        cancelFaceBtn.setManaged(true);

        faceStatusLabel.setText("📷 Positionnez votre visage devant la caméra...");
        faceStatusLabel.setStyle("-fx-text-fill: #241197; -fx-font-weight: 600;");

        // Démarrer la caméra
        boolean started = faceCameraService.startCamera(facePreview, faceCount -> {
            if (faceCount == 1) {
                faceDetectionLabel.setText("✅ Visage détecté");
                faceDetectionLabel.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                captureFaceBtn.setDisable(false);
            } else if (faceCount == 0) {
                faceDetectionLabel.setText("🔍 Aucun visage détecté");
                faceDetectionLabel.setStyle("-fx-text-fill: #e17055; -fx-font-weight: bold;");
                captureFaceBtn.setDisable(true);
            } else {
                faceDetectionLabel.setText("⚠️ " + faceCount + " visages — 1 seul requis");
                faceDetectionLabel.setStyle("-fx-text-fill: #e17055; -fx-font-weight: bold;");
                captureFaceBtn.setDisable(true);
            }
        });

        if (!started) {
            faceStatusLabel.setText("❌ Impossible d'ouvrir la caméra");
            faceStatusLabel.setStyle("-fx-text-fill: #d63031;");
            handleCancelFace(null);
        }
    }

    @FXML
    private void handleCaptureFace(ActionEvent event) {
        if (faceCameraService == null) return;

        faceStatusLabel.setText("🔄 Extraction de l'embedding facial...");
        captureFaceBtn.setDisable(true);

        new Thread(() -> {
            double[] embedding = faceCameraService.captureAndExtract();

            javafx.application.Platform.runLater(() -> {
                if (embedding != null) {
                    boolean success = faceRecognitionService.enrollFace(currentUser, embedding);
                    if (success) {
                        faceStatusLabel.setText("✅ Visage enregistré avec succès ! Le login facial est maintenant activé.");
                        faceStatusLabel.setStyle("-fx-text-fill: #00b894; -fx-font-weight: 600;");
                        // Mettre à jour la session
                        SessionManager.getInstance().getCurrentUser().setFaceEnabled(true);
                        SessionManager.getInstance().getCurrentUser().setFaceEmbedding(currentUser.getFaceEmbedding());
                    } else {
                        faceStatusLabel.setText("❌ Erreur lors de l'enregistrement du visage.");
                        faceStatusLabel.setStyle("-fx-text-fill: #d63031;");
                    }
                } else {
                    faceStatusLabel.setText("❌ Impossible d'extraire l'embedding. Vérifiez que le modèle DNN est présent.");
                    faceStatusLabel.setStyle("-fx-text-fill: #d63031;");
                }

                // Arrêter la caméra et nettoyer l'UI
                handleCancelFace(null);
                initFaceStatus();
            });
        }).start();
    }

    @FXML
    private void handleDisableFace(ActionEvent event) {
        faceRecognitionService = new tn.rouhfan.services.FaceRecognitionService();
        faceRecognitionService.initialize();

        boolean success = faceRecognitionService.disableFace(currentUser);
        if (success) {
            SessionManager.getInstance().getCurrentUser().setFaceEnabled(false);
            SessionManager.getInstance().getCurrentUser().setFaceEmbedding(null);
            faceStatusLabel.setText("🚫 Reconnaissance faciale désactivée.");
            faceStatusLabel.setStyle("-fx-text-fill: #5a4a72;");
        }
        initFaceStatus();
    }

    @FXML
    private void handleCancelFace(ActionEvent event) {
        if (faceCameraService != null) {
            faceCameraService.stopCamera();
        }

        faceCameraBox.setVisible(false);
        faceCameraBox.setManaged(false);
        captureFaceBtn.setVisible(false);
        captureFaceBtn.setManaged(false);
        cancelFaceBtn.setVisible(false);
        cancelFaceBtn.setManaged(false);
        facePreview.setImage(null);
        faceDetectionLabel.setText("");

        initFaceStatus();
    }
}