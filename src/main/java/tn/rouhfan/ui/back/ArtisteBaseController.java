package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.Router;

public class ArtisteBaseController {

    @FXML
    private VBox contentHost;

    @FXML
    private Label pageTitle;

    @FXML
    private Label userNameLabel;

    @FXML
    private Button navAccueil;

    @FXML
    private Button navGalerie;

    @FXML
    private Button navEvenements;

    @FXML
    private Button navCours;

    @FXML
    private Button navCertificats;

    @FXML
    private Button navFavoris;

    @FXML
    public void initialize() {
        // ── GUARD DE SÉCURITÉ ──
        if (!SessionManager.getInstance().checkAccess("ROLE_ARTISTE")) {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Accès refusé");
                alert.setHeaderText(null);
                alert.setContentText("⛔ Vous devez être artiste pour accéder à cette page.");
                alert.showAndWait();

                try {
                    Stage stage = (Stage) contentHost.getScene().getWindow();
                    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
                    stage.getScene().setRoot(root);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        // Afficher le nom de l'utilisateur connecté
        if (SessionManager.getInstance().isLoggedIn()) {
            userNameLabel.setText(SessionManager.getInstance().getFullName());
        }
        openDashboardHome(null);
    }

    @FXML
    private void openDashboardHome(ActionEvent event) {
        setActive(navAccueil);
        pageTitle.setText("Tableau de bord");
        Router.setContent(contentHost, "/ui/back/DashboardHome.fxml");
    }

    @FXML
    private void openGalerie(ActionEvent event) {
        setActive(navGalerie);
        pageTitle.setText("Mes Œuvres");
        Router.setContent(contentHost, "/ui/back/GalerieView.fxml");
    }

    @FXML
    private void openEvenements(ActionEvent event) {
        setActive(navEvenements);
        pageTitle.setText("Mes Événements");
        Router.setContent(contentHost, "/ui/back/EvenementsView.fxml");
    }

    @FXML
    private void openCours(ActionEvent event) {
        setActive(navCours);
        pageTitle.setText("Mes Cours");
        Router.setContent(contentHost, "/ui/back/CoursView.fxml");
    }

    @FXML
    private void openCertificats(ActionEvent event) {
        setActive(navCertificats);
        pageTitle.setText("Mes Certificats");
        Router.setContent(contentHost, "/ui/back/CertificatsView.fxml");
    }

    @FXML
    private void openFavoris(ActionEvent event) {
        setActive(navFavoris);
        pageTitle.setText("Mes Favoris");
        Router.setContent(contentHost, "/ui/back/FavorisView.fxml");
    }

    @FXML
    private void openProfile(ActionEvent event) {
        clearActive();
        pageTitle.setText("Mon Profil");
        Router.setContent(contentHost, "/ui/back/ProfileView.fxml");
    }

    @FXML
    private void backToFront(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Scene scene = stage.getScene();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/ui/front/FrontBase.fxml"));
            scene.setRoot(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        // Déconnexion : vider la session et retourner au login
        SessionManager.getInstance().logout();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Scene scene = stage.getScene();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
            scene.setRoot(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setActive(Button active) {
        clearActive();
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }

    private void clearActive() {
        navAccueil.getStyleClass().remove("active");
        navGalerie.getStyleClass().remove("active");
        navEvenements.getStyleClass().remove("active");
        navCours.getStyleClass().remove("active");
        navCertificats.getStyleClass().remove("active");
        if (navFavoris != null) navFavoris.getStyleClass().remove("active");
    }
}