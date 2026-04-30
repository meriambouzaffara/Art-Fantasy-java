package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.FaceCameraService;
import tn.rouhfan.services.FaceRecognitionService;
import tn.rouhfan.services.LoginLogService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.SessionManager;

import java.io.IOException;

/**
 * Contrôleur pour le login par reconnaissance faciale.
 */
public class FaceLoginController {

    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;
    @FXML private Label faceCountLabel;
    @FXML private Button captureBtn;
    @FXML private Button startCameraBtn;
    @FXML private Button stopCameraBtn;
    @FXML private VBox resultBox;
    @FXML private Label resultLabel;

    private FaceRecognitionService faceService;
    private FaceCameraService cameraService;
    private final LoginLogService loginLogService = new LoginLogService();

    @FXML
    public void initialize() {
        faceService = new FaceRecognitionService();
        captureBtn.setDisable(true);
        stopCameraBtn.setDisable(true);
        resultBox.setVisible(false);

        if (!faceService.initialize()) {
            statusLabel.setText("⚠️ Erreur: Impossible d'initialiser OpenCV");
            statusLabel.setStyle("-fx-text-fill: #d63031;");
            startCameraBtn.setDisable(true);
        } else {
            statusLabel.setText("✅ OpenCV initialisé — Cliquez sur Démarrer");
            statusLabel.setStyle("-fx-text-fill: #00b894;");
        }

        cameraService = new FaceCameraService(faceService);
    }

    @FXML
    private void startCamera(ActionEvent event) {
        statusLabel.setText("📷 Démarrage de la caméra...");
        resultBox.setVisible(false);

        boolean started = cameraService.startCamera(cameraPreview, faceCount -> {
            if (faceCount == 0) {
                faceCountLabel.setText("🔍 Aucun visage détecté");
                faceCountLabel.setStyle("-fx-text-fill: #e17055;");
                captureBtn.setDisable(true);
            } else if (faceCount == 1) {
                faceCountLabel.setText("✅ 1 visage détecté");
                faceCountLabel.setStyle("-fx-text-fill: #00b894;");
                captureBtn.setDisable(false);
            } else {
                faceCountLabel.setText("⚠️ " + faceCount + " visages — 1 seul requis");
                faceCountLabel.setStyle("-fx-text-fill: #e17055;");
                captureBtn.setDisable(true);
            }
        });

        if (started) {
            statusLabel.setText("📷 Caméra active — Positionnez votre visage");
            startCameraBtn.setDisable(true);
            stopCameraBtn.setDisable(false);
        } else {
            statusLabel.setText("❌ Erreur: Caméra introuvable");
            statusLabel.setStyle("-fx-text-fill: #d63031;");
        }
    }

    @FXML
    private void stopCamera(ActionEvent event) {
        cameraService.stopCamera();
        statusLabel.setText("⏹️ Caméra arrêtée");
        startCameraBtn.setDisable(false);
        stopCameraBtn.setDisable(true);
        captureBtn.setDisable(true);
        faceCountLabel.setText("");
        cameraPreview.setImage(null);
    }

    @FXML
    private void captureAndLogin(ActionEvent event) {
        statusLabel.setText("🔄 Analyse du visage en cours...");
        captureBtn.setDisable(true);
        resultBox.setVisible(false);

        // Exécuter en arrière-plan
        new Thread(() -> {
            double[] embedding = cameraService.captureAndExtract();

            if (embedding == null) {
                Platform.runLater(() -> {
                    showResult(false, "❌ Impossible d'extraire le visage.\nVérifiez que le modèle d'embedding est présent.");
                    captureBtn.setDisable(false);
                });
                return;
            }

            // Chercher l'utilisateur correspondant
            User matchedUser = faceService.findMatchingUser(embedding);

            Platform.runLater(() -> {
                if (matchedUser != null) {
                    // Login réussi
                    showResult(true, "✅ Bienvenue " + matchedUser.getPrenom() + " " + matchedUser.getNom() + " !");

                    // Effectuer la connexion
                    SessionManager.getInstance().login(matchedUser);
                    loginLogService.logSuccess(matchedUser.getId(), matchedUser.getEmail());

                    AppLogger.info("[FaceLogin] LOGIN_SUCCESS via Face ID | " + matchedUser.getEmail());

                    // Rediriger après 2 secondes
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            cameraService.stopCamera();
                            navigateAfterLogin(event, matchedUser);
                        });
                    }).start();

                } else {
                    showResult(false, "❌ Visage non reconnu.\nVeuillez réessayer ou utilisez un autre mode de connexion.");
                    loginLogService.logFailure("face_login", "Visage non reconnu");
                    captureBtn.setDisable(false);
                }
            });
        }).start();
    }

    private void showResult(boolean success, String message) {
        resultBox.setVisible(true);
        resultLabel.setText(message);
        if (success) {
            resultBox.setStyle("-fx-background-color: rgba(0, 184, 148, 0.1); -fx-background-radius: 12; -fx-padding: 15;");
            resultLabel.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold; -fx-font-size: 14;");
        } else {
            resultBox.setStyle("-fx-background-color: rgba(214, 48, 49, 0.1); -fx-background-radius: 12; -fx-padding: 15;");
            resultLabel.setStyle("-fx-text-fill: #d63031; -fx-font-weight: bold; -fx-font-size: 14;");
        }
    }

    private void navigateAfterLogin(ActionEvent event, User user) {
        try {
            Stage stage = (Stage) cameraPreview.getScene().getWindow();
            String role = SessionManager.getInstance().getRole();
            String fxmlPath = (role != null && role.toUpperCase().contains("ADMIN"))
                    ? "/ui/back/BackBase.fxml"
                    : "/ui/front/FrontBase.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            AppLogger.error("[FaceLogin] Erreur de navigation", e);
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        cameraService.stopCamera();
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            AppLogger.error("[FaceLogin] Erreur retour", e);
        }
    }

    /**
     * Appelé quand la fenêtre est fermée — libère la caméra.
     */
    public void cleanup() {
        if (cameraService != null) {
            cameraService.stopCamera();
        }
    }
}
