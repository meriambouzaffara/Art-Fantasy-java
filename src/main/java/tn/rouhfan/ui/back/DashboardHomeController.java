package tn.rouhfan.ui.back;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.services.SponsorService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.AppLogger;

/**
 * Contrôleur du Dashboard Home enrichi avec statistiques utilisateurs.
 */
public class DashboardHomeController implements Initializable {

    @FXML private Node rootNode;
    @FXML private Label statEvenements;
    @FXML private Label statSponsors;
    @FXML private Label statTotalUsers;
    @FXML private Label statActiveUsers;
    @FXML private Label statNewToday;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            EvenementService evenementService = new EvenementService();
            int nbEvents = evenementService.recuperer().size();
            if (statEvenements != null) {
                statEvenements.setText(nbEvents + " événements prévus");
            }

            SponsorService sponsorService = new SponsorService();
            int nbSponsors = sponsorService.recuperer().size();
            if (statSponsors != null) {
                statSponsors.setText(nbSponsors + " sponsors actifs");
            }

            // Statistiques utilisateurs
            UserService userService = new UserService();
            if (statTotalUsers != null) {
                statTotalUsers.setText(String.valueOf(userService.countTotal()));
            }
            if (statActiveUsers != null) {
                statActiveUsers.setText(String.valueOf(userService.countActive()));
            }
            if (statNewToday != null) {
                statNewToday.setText(String.valueOf(userService.countCreatedToday()));
            }

        } catch (Exception e) {
            AppLogger.error("Erreur chargement stats Dashboard", e);
        }
    }

    @FXML private void openDashboardHome(Event event) { triggerNavigation("navAccueil"); }
    @FXML private void openUtilisateurs(Event event) { triggerNavigation("navUtilisateurs"); }
    @FXML private void openGalerie(Event event) { triggerNavigation("navGalerie"); }
    @FXML private void openCategories(Event event) { triggerNavigation("navCategories"); }
    @FXML private void openEvenements(Event event) { triggerNavigation("navEvenements"); }
    @FXML private void openSponsors(Event event) { triggerNavigation("navSponsors"); }
    @FXML private void openCours(Event event) { triggerNavigation("navCours"); }
    @FXML private void openCertificats(Event event) { triggerNavigation("navCertificats"); }
    @FXML private void openMagasin(Event event) { triggerNavigation("navMagasin"); }
    @FXML private void openArticles(Event event) { triggerNavigation("navArticles"); }
    @FXML private void openAvis(Event event) { triggerNavigation("navAvis"); }
    @FXML private void openOeuvreStatistiques(Event event) { triggerNavigation("navOeuvreStatistiques"); }
    @FXML private void openAIUsage(Event event) { triggerNavigation("navAIUsage"); }

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