package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.ui.Router;

public class FrontBaseController {

    @FXML
    private VBox contentHost;

    @FXML
    private VBox heroSection;

    @FXML
    public void initialize() {
        showHero(true);
    }

    @FXML
    private void goHome(ActionEvent event) {
        showHero(true);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goEvenements(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            VBox view = Router.loadView("/ui/front/EvenementsFront.fxml");
            contentHost.getChildren().add(view);
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
    private void goCours(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goMagasin(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goCategories(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goOeuvres(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goAvis(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goAbout(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
    }

    private void showHero(boolean show) {
        heroSection.setVisible(show);
        heroSection.setManaged(show);
        contentHost.setVisible(!show);
        contentHost.setManaged(!show);
    }

    @FXML
    private void signup(ActionEvent event) {
        // Navigation vers l'inscription
    }

    @FXML
    private void login(ActionEvent event) {
        // Navigation vers la connexion
    }

    @FXML
    private void contact(ActionEvent event) {
    }

    @FXML
    private void logout(ActionEvent event) {
        // Logique de déconnexion à implémenter
    }

    @FXML
    private void openDashboard(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Scene scene = stage.getScene();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/ui/back/BackBase.fxml"));
            scene.setRoot(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
