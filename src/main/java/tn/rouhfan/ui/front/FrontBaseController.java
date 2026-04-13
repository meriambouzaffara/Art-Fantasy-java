package tn.rouhfan.ui.front;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class FrontBaseController {

    @FXML
    private VBox contentHost;

    @FXML
    private VBox heroSection;

    private String currentUserRole = "ARTIST"; // Default to ARTIST for development, can be changed

    @FXML
    public void initialize() {
        showHero(true);
    }

    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }

    @FXML
    private void goHome(ActionEvent event) {
        showHero(true);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goCategories(ActionEvent event) {
        loadPage("/ui/front/GalerieFront.fxml", true); // true for categories mode
    }

    @FXML
    private void goOeuvres(ActionEvent event) {
        loadPage("/ui/front/GalerieFront.fxml", false); // false for artworks mode
    }

    private void loadPage(String fxmlPath, boolean isCategoryMode) {
        try {
            showHero(false);
            contentHost.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            if (fxmlPath.contains("GalerieFront")) {
                GalerieFrontController controller = loader.getController();
                controller.setUserRole(currentUserRole);
                controller.setCategoryMode(isCategoryMode);
            }
            
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void goEvenements(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goCours(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goMagasin(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goAvis(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goAbout(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }

    private void showHero(boolean show) {
        heroSection.setVisible(show);
        heroSection.setManaged(show);
        contentHost.setVisible(!show);
        contentHost.setManaged(!show);
    }

    @FXML
    private void openDashboard(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ui/back/BackBase.fxml"));
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void signup(ActionEvent event) {}
    @FXML private void login(ActionEvent event) {}
}
