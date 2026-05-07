package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.NotificationService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.Router;

import java.io.IOException;

public class FrontBaseController {

    @FXML private VBox contentHost;
    @FXML private VBox heroSection;

    // Navbar buttons
    @FXML private HBox guestButtons;
    @FXML private HBox userButtons;
    @FXML private Label usernameLabel;
    @FXML private Button profileBtn;
    @FXML private Button iaBtn;
    @FXML private Button dashboardBtn;
    @FXML private Button logoutBtn;
    @FXML private Button notifBtn;

    // Bouton panier flottant (Magasin)
    @FXML private Button cartButton;

    // Bouton Dashboard dans le Hero (page d'accueil)
    @FXML private Button adminHeroDashBtn;

    private final NotificationService notificationService = new NotificationService();
    private final PanierService panierService = PanierService.getInstance();

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
            boolean isParticipant = false;
            boolean isAdmin = false;
            String roleEmoji = "👤";

            if (role != null) {
                String r = role.toUpperCase();
                if (r.contains("ADMIN")) {
                    isAdmin = true;
                    roleEmoji = "🛠️";
                } else if (r.contains("ARTISTE")) {
                    roleEmoji = "🎨";
                } else {
                    isParticipant = true;
                    roleEmoji = "🎭";
                }
            }

            usernameLabel.setText(roleEmoji + " " + currentUser.getPrenom());
            
            // Créations IA visible pour tous les utilisateurs connectés
            iaBtn.setVisible(true);
            iaBtn.setManaged(true);
            
            // Dashboard uniquement pour l'Admin
            dashboardBtn.setVisible(isAdmin);
            dashboardBtn.setManaged(isAdmin);

            // Bouton Dashboard dans la page d'accueil (Hero)
            if (adminHeroDashBtn != null) {
                adminHeroDashBtn.setVisible(isAdmin);
                adminHeroDashBtn.setManaged(isAdmin);
            }
            
            // Notifications
            int unread = notificationService.countUnread(currentUser.getId());
            if (unread > 0) {
                notifBtn.setText("\uD83D\uDD14 (" + unread + ")");
                notifBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fac62d; -fx-font-weight: bold; -fx-font-size: 16; -fx-cursor: hand; -fx-border-color: #fac62d; -fx-border-radius: 15; -fx-border-width: 1;");
            } else {
                notifBtn.setText("\uD83D\uDD14");
                notifBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fac62d; -fx-font-weight: bold; -fx-font-size: 16; -fx-cursor: hand;");
            }
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
        Router.setContent(contentHost, "/ui/front/front_liste_magasins.fxml");
    }

    @FXML
    private void goDashboard(ActionEvent event) {
        SessionManager session = SessionManager.getInstance();
        User currentUser = session.getCurrentUser();

        // 🔐 Utilisateur non connecté
        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez vous connecter pour accéder au tableau de bord.");
            alert.showAndWait();
            return;
        }

        // 🔐 Vérification du rôle Admin
        String role = session.getRole();
        if (role == null || !role.toUpperCase().contains("ADMIN")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText("⛔ Accès refusé");
            alert.setContentText("Seuls les administrateurs peuvent accéder au tableau de bord.");
            alert.showAndWait();
            return;
        }

        // ✅ Admin → Redirection vers le vrai Dashboard Admin
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/back/BackBase.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement AdminDashboard: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger le tableau de bord");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void goIA(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/IAOeuvresFront.fxml"));
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            
            // Forcer le contenu à remplir l'espace
            VBox.setVgrow(root, Priority.ALWAYS);
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Profil: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setHeaderText("Impossible d'ouvrir le profil");
            alert.setContentText("Une erreur est survenue : " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void openNotifications(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/NotificationsFront.fxml"));
            contentHost.getChildren().add(root);
            
            // Mettre à jour le compteur (réinitialiser)
            notifBtn.setText("\uD83D\uDD14");
            notifBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fac62d; -fx-font-weight: bold; -fx-font-size: 16; -fx-cursor: hand;");
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Notifications: " + e.getMessage());
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

    @FXML
    private void openCart(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/checkout.fxml"));
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Checkout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void openChatbot(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/Chatbot.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle("Assistant Rouh el Fann");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.NONE); // Permet de continuer à utiliser la fenêtre principale
            stage.setResizable(false);
            
            // Positionner en bas à droite
            stage.setOnShown(e -> {
                Stage mainStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.setX(mainStage.getX() + mainStage.getWidth() - stage.getWidth() - 20);
                stage.setY(mainStage.getY() + mainStage.getHeight() - stage.getHeight() - 40);
            });
            
            stage.show();
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur ouverture Chatbot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Helpers ====================

    private void showHero(boolean show) {
        heroSection.setVisible(show);
        heroSection.setManaged(show);
        contentHost.setVisible(!show);
        contentHost.setManaged(!show);
    }
}