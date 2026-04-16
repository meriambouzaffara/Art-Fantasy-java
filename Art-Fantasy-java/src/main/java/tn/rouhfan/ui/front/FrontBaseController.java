package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import tn.rouhfan.ui.Router; // Import de votre classe Router
import javafx.fxml.FXMLLoader;    // ← Pour FXMLLoader
import javafx.scene.Parent;       // ← Pour Parent
import javafx.stage.Stage;        // ← Pour Stage
import java.io.IOException;       // ← Pour IOException
public class FrontBaseController {

    @FXML private VBox contentHost;
    @FXML private VBox heroSection;

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
    @FXML
    private void goEvenements() {
        showContent();
        Router.setContent(contentHost, "/ui/front/EvenementView.fxml");
    }

    @FXML
    private void goHome() {
        // Réinitialisation de la vue d'accueil
        heroSection.setVisible(true);
        heroSection.setManaged(true);
        contentHost.setVisible(false);
        contentHost.setManaged(false);
        contentHost.getChildren().clear();
    }

    private void showContent() {
        // Cache la bannière pour libérer l'espace pour le contenu dynamique
        heroSection.setVisible(false);
        heroSection.setManaged(false);
        contentHost.setVisible(true);
        contentHost.setManaged(true);
    }

    @FXML
    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/BackBase.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthodes requises par FrontBase.fxml pour éviter les erreurs "Cannot resolve symbol"
    @FXML private void goMagasin() {}
    @FXML private void goCategories() {}
    @FXML private void goOeuvres() {}
    @FXML private void goAvis() {}


    @FXML private void signup() {}
    @FXML private void login() {}
    @FXML private void goAbout() {}
}