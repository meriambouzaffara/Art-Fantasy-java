package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.services.CoursService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Cours2Controller implements Initializable {

    @FXML private FlowPane coursesGrid;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox detailView;
    @FXML private Label lblNom, lblNiveau;
    @FXML private TextArea taContenu;

    private final CoursService coursService = new CoursService();
    private Cours selectedCours;

    private VBox contentHost;
    private VBox heroSection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDataAndCards();
        // Attendre que la scène soit complètement chargée
        Platform.runLater(() -> {
            initContainerReferences();
        });
    }

    private void initContainerReferences() {
        try {
            // Vérifier si le label est dans une scène
            if (lblNom.getScene() == null) {
                System.err.println("Scene pas encore prête, réessai dans 200ms...");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pause.setOnFinished(event -> initContainerReferences());
                pause.play();
                return;
            }

            Stage stage = (Stage) lblNom.getScene().getWindow();
            BorderPane root = (BorderPane) stage.getScene().getRoot();
            contentHost = (VBox) root.lookup("#contentHost");
            heroSection = (VBox) root.lookup("#heroSection");

            if (contentHost != null && heroSection != null) {
                System.out.println("Conteneurs récupérés avec succès !");
            } else {
                System.err.println("Conteneurs non trouvés, réessai...");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pause.setOnFinished(event -> initContainerReferences());
                pause.play();
            }
        } catch (Exception ex) {
            System.err.println("Impossible de récupérer les conteneurs: " + ex.getMessage());
            // Réessayer après un délai
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            pause.setOnFinished(event -> initContainerReferences());
            pause.play();
        }
    }

    private void loadDataAndCards() {
        coursesGrid.getChildren().clear();
        try {
            List<Cours> list = coursService.recuperer();
            // Filtrer uniquement les cours avec statut "Publié"
            for (Cours c : list) {
                if ("Publié".equals(c.getStatut())) {
                    coursesGrid.getChildren().add(createCourseCard(c));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private VBox createCourseCard(Cours c) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 220);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 15; -fx-padding: 20; -fx-cursor: hand;");

        Label titleLabel = new Label(c.getNom());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        card.getChildren().addAll(new Label("📚"), titleLabel);
        card.setOnMouseClicked(event -> showDetails(c));

        return card;
    }

    private void showDetails(Cours c) {
        this.selectedCours = c;
        mainScroll.setVisible(false);
        mainScroll.setManaged(false);
        detailView.setVisible(true);
        detailView.setManaged(true);

        lblNom.setText(c.getNom());
        lblNiveau.setText("Difficulté : " + c.getNiveau());
        taContenu.setText(c.getContenu());
    }

    @FXML
    private void retourListe() {
        detailView.setVisible(false);
        detailView.setManaged(false);
        mainScroll.setVisible(true);
        mainScroll.setManaged(true);
    }

    @FXML
    private void goPasserQcm() {
        if (selectedCours == null) {
            System.out.println("Aucun cours sélectionné");
            showErrorAlert("Erreur", "Veuillez sélectionner un cours d'abord");
            return;
        }

        try {
            // Récupérer les conteneurs au moment du besoin (en cas d'échec précédent)
            if (contentHost == null || heroSection == null) {
                System.out.println("Tentative de récupération des conteneurs...");
                Stage stage = (Stage) lblNom.getScene().getWindow();
                BorderPane root = (BorderPane) stage.getScene().getRoot();
                contentHost = (VBox) root.lookup("#contentHost");
                heroSection = (VBox) root.lookup("#heroSection");
            }

            if (contentHost == null) {
                System.err.println("contentHost est null, impossible de continuer");
                showErrorAlert("Erreur", "Problème technique, veuillez réessayer");
                return;
            }

            // Charger la vue du QCM
            String fxmlPath = "/ui/front/QcmPassageView.fxml";
            URL fxmlLocation = getClass().getResource(fxmlPath);

            if (fxmlLocation == null) {
                System.err.println("Fichier FXML introuvable: " + fxmlPath);
                showErrorAlert("Erreur", "Fichier QcmPassageView.fxml introuvable");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent qcmView = loader.load();

            QcmPassageController controller = loader.getController();
            controller.initData(selectedCours, contentHost, heroSection);

            // Cacher heroSection et afficher le QCM
            if (heroSection != null) {
                heroSection.setVisible(false);
                heroSection.setManaged(false);
            }

            contentHost.setVisible(true);
            contentHost.setManaged(true);
            contentHost.getChildren().clear();
            contentHost.getChildren().add(qcmView);

        } catch (IOException ex) {
            System.err.println("Erreur lors du chargement du QCM: " + ex.getMessage());
            ex.printStackTrace();
            showErrorAlert("Erreur", "Impossible de charger le QCM: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Erreur inattendue: " + ex.getMessage());
            ex.printStackTrace();
            showErrorAlert("Erreur", "Une erreur inattendue s'est produite");
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}