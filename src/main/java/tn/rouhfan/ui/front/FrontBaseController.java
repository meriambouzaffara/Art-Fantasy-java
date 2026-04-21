package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.Router;

import java.io.IOException;

public class FrontBaseController {

    @FXML private VBox contentHost;
    @FXML private VBox heroSection;

    // Navbar buttons
    @FXML private HBox guestButtons;
    @FXML private HBox userButtons;
    @FXML private Label welcomeLabel;
    @FXML private Button profileBtn;

    @FXML
    public void initialize() {
        showHero(true);
        setupNavbarByRole();
    }

    /**
     * Configure la navbar selon l'utilisateur connecté :
     * - Non connecté : Sign Up + Login
     * - Artiste : Bienvenue + Profil + Déconnexion
     * - Participant : Bienvenue + Profil + Déconnexion
     * - Admin ne devrait jamais arriver ici (redirigé vers Dashboard)
     */
    private void setupNavbarByRole() {
        SessionManager session = SessionManager.getInstance();
        User currentUser = session.getCurrentUser();

        if (currentUser == null) {
            // Non connecté : afficher Sign Up + Login
            guestButtons.setVisible(true);
            guestButtons.setManaged(true);
            userButtons.setVisible(false);
            userButtons.setManaged(false);
        } else {
            // Connecté : afficher Bienvenue + Profil + Déconnexion
            guestButtons.setVisible(false);
            guestButtons.setManaged(false);
            userButtons.setVisible(true);
            userButtons.setManaged(true);

            String role = session.getRole();
            String roleEmoji;
            if (role != null && role.toUpperCase().contains("ARTISTE")) {
                roleEmoji = "🎨";
            } else {
                roleEmoji = "🎭";
            }
            welcomeLabel.setText(roleEmoji + " " + currentUser.getPrenom() + " " + currentUser.getNom());
        }
    }

    // ==================== Navigation ====================

    @FXML
    private void goHome(ActionEvent event) {
        showHero(true);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goCategories(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/GalerieFront.fxml"));
            Parent root = loader.load();
            GalerieFrontController controller = loader.getController();
            controller.setCategoryMode(true);
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Catégories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goOeuvres(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/GalerieFront.fxml"));
            Parent root = loader.load();
            GalerieFrontController controller = loader.getController();
            controller.setCategoryMode(false);
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Oeuvres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goEvenements(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/EvenementsFront.fxml"));
            contentHost.getChildren().add(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goSponsors(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            VBox view = Router.loadView("/ui/front/SponsorsFront.fxml");
            contentHost.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goCours() {
        showContent();
        // Utilisation du chemin relatif géré par votre Router
        Router.setContent(contentHost, "/ui/front/Cours2View.fxml");
    }
    @FXML
    private void goCertificats() {
        showContent();
        Router.setContent(contentHost, "/ui/front/Certificats2View.fxml");
    }
    private void showContent() {
        heroSection.setVisible(false);
        heroSection.setManaged(false);

        contentHost.setVisible(true);
        contentHost.setManaged(true);
    }
    @FXML
    private void goMagasin(ActionEvent event) {
        showHero(false);
        Router.setContent(contentHost, "/ui/front/front_magasins.fxml");
    }
    @FXML
    private void goAvis(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();

        // 🔐 Vérifier si utilisateur connecté
        if (SessionManager.getInstance().getCurrentUser() == null) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez vous connecter pour accéder aux réclamations !");
            alert.showAndWait();

            try {
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
                stage.getScene().setRoot(root);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        // ✅ Charger la vue des réclamations
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/ReclamationFront.fxml"));
            Parent root = loader.load();

            contentHost.getChildren().add(root);

        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Réclamations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void goAbout(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }

    // ==================== Auth ====================

    @FXML
    private void signup(ActionEvent event) {
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/SignUp.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void login(ActionEvent event) {
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openProfile(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/ProfileView.fxml"));
            Parent root = loader.load();
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Profil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionManager.getInstance().logout();
        System.out.println("[FrontBase] Déconnexion effectuée");
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/FrontBase.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void contact(ActionEvent event) {
    }

    // ==================== Helpers ====================

    private void showHero(boolean show) {
        heroSection.setVisible(show);
        heroSection.setManaged(show);
        contentHost.setVisible(!show);
        contentHost.setManaged(!show);
    }
}