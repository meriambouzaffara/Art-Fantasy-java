package tn.rouhfan.ui.back;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;

public class DashboardHomeController {

    @FXML
    private Node rootNode;

    @FXML
    private void openDashboardHome(Event event) {
        triggerNavigation("navAccueil");
    }

    @FXML
    private void openUtilisateurs(Event event) {
        triggerNavigation("navUtilisateurs");
    }

    @FXML
    private void openGalerie(Event event) {
        triggerNavigation("navGalerie");
    }

    @FXML
    private void openCategories(Event event) {
        triggerNavigation("navCategories");
    }

    @FXML
    private void openEvenements(Event event) {
        triggerNavigation("navEvenements");
    }

    @FXML
    private void openSponsors(Event event) {
        triggerNavigation("navSponsors");
    }

    @FXML
    private void openCours(Event event) {
        triggerNavigation("navCours");
    }

    @FXML
    private void openCertificats(Event event) {
        triggerNavigation("navCertificats");
    }

    @FXML
    private void openMagasin(Event event) {
        triggerNavigation("navMagasin");
    }

    @FXML
    private void openArticles(Event event) {
        triggerNavigation("navArticles");
    }

    @FXML
    private void openAvis(Event event) {
        triggerNavigation("navAvis");
    }

    private void triggerNavigation(String buttonId) {
        try {
            Parent root = rootNode.getScene().getRoot();
            Button btn = (Button) root.lookup("#" + buttonId);
            if (btn != null) {
                btn.fire();
            }
        } catch (Exception e) {
            System.out.println("Navigation vers: " + buttonId);
        }
    }
}
