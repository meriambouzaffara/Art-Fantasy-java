package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.ui.Router;

public class BackBaseController {

    @FXML
    private VBox contentHost;

    @FXML
    private Label pageTitle;

    @FXML
    private Button navAccueil;

    @FXML
    private Button navGalerie;

    @FXML
    private Button navUtilisateurs;

    @FXML
    private Button navCategories;

    @FXML
    private Button navEvenements;

    @FXML
    private Button navSponsors;

    @FXML
    private Button navCours;

    @FXML
    private Button navCertificats;

    @FXML
    private Button navMagasin;

    @FXML
    private Button navArticles;

    @FXML
    private Button navAvis;

    @FXML
    public void initialize() {
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
        pageTitle.setText("Gestion Galerie");
        Router.setContent(contentHost, "/ui/back/GalerieView.fxml");
    }

    @FXML
    private void openUtilisateurs(ActionEvent event) {
        setActive(navUtilisateurs);
        pageTitle.setText("Gestion Des Utilisateurs");
        Router.setContent(contentHost, "/ui/back/UtilisateursView.fxml");
    }

    @FXML
    private void openCategories(ActionEvent event) {
        setActive(navCategories);
        pageTitle.setText("Gestion Catégories");
        Router.setContent(contentHost, "/ui/back/CategoriesView.fxml");
    }

    @FXML
    private void openEvenements(ActionEvent event) {
        setActive(navEvenements);
        pageTitle.setText("Gestion événements");
        Router.setContent(contentHost, "/ui/back/EvenementsView.fxml");
    }

    @FXML
    private void openSponsors(ActionEvent event) {
        setActive(navSponsors);
        pageTitle.setText("Gestion sponsors");
        Router.setContent(contentHost, "/ui/back/SponsorsView.fxml");
    }

    @FXML
    private void openCours(ActionEvent event) {
        setActive(navCours);
        pageTitle.setText("Gestion cours");
        Router.setContent(contentHost, "/ui/back/CoursView.fxml");
    }

    @FXML
    private void openCertificats(ActionEvent event) {
        setActive(navCertificats);
        pageTitle.setText("Gestion certificats");
        Router.setContent(contentHost, "/ui/back/CertificatsView.fxml");
    }

    @FXML
    private void openMagasin(ActionEvent event) {
        setActive(navMagasin);
        pageTitle.setText("Gestion magasin");
        Router.setContent(contentHost, "/ui/back/MagasinView.fxml");
    }

    @FXML
    private void openArticles(ActionEvent event) {
        setActive(navArticles);
        pageTitle.setText("Gestion articles");
        // Je vais rediriger vers une page vide pour le moment car ArticleView n'est pas encore créé
        Router.setContent(contentHost, "/ui/back/PlaceholderPage.fxml");
    }

    @FXML
    private void openAvis(ActionEvent event) {
        setActive(navAvis);
        pageTitle.setText("Avis & Réclamations");
        Router.setContent(contentHost, "/ui/back/AvisView.fxml");
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
        navUtilisateurs.getStyleClass().remove("active");
        navCategories.getStyleClass().remove("active");
        navEvenements.getStyleClass().remove("active");
        navSponsors.getStyleClass().remove("active");
        navCours.getStyleClass().remove("active");
        navCertificats.getStyleClass().remove("active");
        navMagasin.getStyleClass().remove("active");
        navArticles.getStyleClass().remove("active");
        navAvis.getStyleClass().remove("active");
    }
}
