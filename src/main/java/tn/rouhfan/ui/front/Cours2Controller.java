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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Cours2Controller implements Initializable {

    @FXML private FlowPane coursesGrid;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox detailView;
    @FXML private Label lblNom, lblNiveau;
    @FXML private TextArea taContenu;
    @FXML private TextField searchField; // Nouveau champ de recherche

    private final CoursService coursService = new CoursService();
    private Cours selectedCours;
    private List<Cours> allPublishedCourses = new ArrayList<>();

    private VBox contentHost;
    private VBox heroSection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDataAndCards();

        // Listener pour la recherche en temps réel
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch(newValue);
        });

        Platform.runLater(this::initContainerReferences);
    }

    private void handleSearch(String query) {
        if (allPublishedCourses == null) return;

        if (query == null || query.isEmpty()) {
            afficherCartes(allPublishedCourses);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<Cours> filteredList = allPublishedCourses.stream()
                    .filter(c -> c.getNom().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
            afficherCartes(filteredList);
        }
    }

    private void initContainerReferences() {
        try {
            if (lblNom.getScene() == null) return;
            Stage stage = (Stage) lblNom.getScene().getWindow();
            BorderPane root = (BorderPane) stage.getScene().getRoot();
            contentHost = (VBox) root.lookup("#contentHost");
            heroSection = (VBox) root.lookup("#heroSection");
        } catch (Exception ex) {
            System.err.println("Erreur de récupération des conteneurs.");
        }
    }

    private void loadDataAndCards() {
        try {
            List<Cours> list = coursService.recupererFront();
            allPublishedCourses = new ArrayList<>(list);
            afficherCartes(allPublishedCourses);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void afficherCartes(List<Cours> cours) {
        coursesGrid.getChildren().clear();
        for (Cours c : cours) {
            coursesGrid.getChildren().add(createCourseCard(c));
        }
    }

    private String getCoursEmoji(String nom) {
        if (nom == null) return "📚";
        String n = nom.toLowerCase();
        if (n.contains("peinture"))       return "🎨";
        if (n.contains("dessin"))         return "✏️";
        if (n.contains("sculpture"))      return "🗿";
        if (n.contains("musique"))        return "🎵";
        if (n.contains("calligraphie"))   return "🖊️";
        return "📚";
    }

    private String getPaletteColor(String niveau) {
        if ("Débutant".equals(niveau)) return "#23BBB7";
        if ("Intermédiaire".equals(niveau)) return "#744D83";
        if ("Avancé".equals(niveau)) return "#23627C";
        return "#23BBB7";
    }

    private VBox createCourseCard(Cours c) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 240);
        card.setAlignment(Pos.CENTER);

        String accentColor = getPaletteColor(c.getNiveau());
        String baseStyle =
                "-fx-background-color: #E3DBE6;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                        "-fx-padding: 20;" +
                        "-fx-cursor: hand;";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-scale-x: 1.03; -fx-scale-y: 1.03; -fx-border-width: 4;"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        Label emojiLabel = new Label(getCoursEmoji(c.getNom()));
        emojiLabel.setStyle("-fx-font-size: 50;");

        String nomAffiche = (c.getNom() != null) ? c.getNom().toUpperCase() : "SANS TITRE";
        Label titleLabel = new Label(nomAffiche);
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18; -fx-text-fill: #23627C; -fx-text-alignment: center;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(220);

        Label niveauLabel = new Label(c.getNiveau() != null ? c.getNiveau() : "N/A");
        niveauLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-padding: 5 18; -fx-background-radius: 20; " +
                "-fx-background-color: " + accentColor + ";");

        card.getChildren().addAll(emojiLabel, titleLabel, niveauLabel);
        card.setOnMouseClicked(event -> showDetails(c));

        return card;
    }

    private int getNiveauOrdre(String niveau) {
        if (niveau == null) return 3;
        switch (niveau) {
            case "Débutant": return 1;
            case "Intermédiaire": return 2;
            case "Avancé": return 3;
            default: return 3;
        }
    }

    @FXML private void trierParNiveauCroissant() {
        allPublishedCourses.sort((c1, c2) -> Integer.compare(getNiveauOrdre(c1.getNiveau()), getNiveauOrdre(c2.getNiveau())));
        handleSearch(searchField.getText());
    }

    @FXML private void trierParNiveauDecroissant() {
        allPublishedCourses.sort((c1, c2) -> Integer.compare(getNiveauOrdre(c2.getNiveau()), getNiveauOrdre(c1.getNiveau())));
        handleSearch(searchField.getText());
    }

    @FXML private void trierParNomCroissant() {
        allPublishedCourses.sort((c1, c2) -> c1.getNom().compareToIgnoreCase(c2.getNom()));
        handleSearch(searchField.getText());
    }

    @FXML private void trierParNomDecroissant() {
        allPublishedCourses.sort((c1, c2) -> c2.getNom().compareToIgnoreCase(c1.getNom()));
        handleSearch(searchField.getText());
    }

    @FXML private void resetTri() {
        searchField.clear();
        loadDataAndCards();
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

    @FXML private void retourListe() {
        detailView.setVisible(false);
        detailView.setManaged(false);
        mainScroll.setVisible(true);
        mainScroll.setManaged(true);
    }

    @FXML private void goPasserQcm() {
        if (selectedCours == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/QcmPassageView.fxml"));
            Parent qcmView = loader.load();
            QcmPassageController controller = loader.getController();
            controller.initData(selectedCours, contentHost, heroSection);
            if (heroSection != null) { heroSection.setVisible(false); heroSection.setManaged(false); }
            contentHost.setVisible(true);
            contentHost.setManaged(true);
            contentHost.getChildren().clear();
            contentHost.getChildren().add(qcmView);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}