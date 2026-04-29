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
import tn.rouhfan.entities.User;
import tn.rouhfan.entities.CoursReview;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.Coursreviewservice;
import tn.rouhfan.services.Translationservice;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Cours2Controller implements Initializable {

    // ── FXML Liste ────────────────────────────────────────────────────────────
    @FXML private FlowPane   coursesGrid;
    @FXML private ScrollPane mainScroll;
    @FXML private TextField  searchField;

    // ── FXML Détail ───────────────────────────────────────────────────────────
    @FXML private VBox     detailView;
    @FXML private Label    lblNom;
    @FXML private Label    lblNiveau;
    @FXML private TextArea taContenu;

    // ── FXML Traduction ───────────────────────────────────────────────────────
    @FXML private Button            btnTranslate;
    @FXML private ComboBox<String>  cbLangue;
    @FXML private ProgressIndicator translateLoader;
    @FXML private Label             lblTranslateStatus;

    // ── FXML Étoiles ──────────────────────────────────────────────────────────
    @FXML private HBox  starsInput;    // les 5 étoiles cliquables
    @FXML private HBox  starsDisplay;  // affichage moyenne
    @FXML private Label lblMoyenne;    // "4.2"
    @FXML private Label lblNbNotes;    // "(8 notes)"
    @FXML private Label lblMonNote;    // "Votre note actuelle : ⭐⭐⭐⭐☆"
    @FXML private Button btnNoter;

    // ── Services ──────────────────────────────────────────────────────────────
    private final CoursService       coursService    = new CoursService();
    private final Coursreviewservice reviewService   = new Coursreviewservice();
    private final Translationservice translationService = new Translationservice();

    // ── État ──────────────────────────────────────────────────────────────────
    private Cours       selectedCours;
    private List<Cours> allPublishedCourses = new ArrayList<>();
    private boolean     isTranslated        = false;
    private int         selectedNote        = 0;
    private Button[]    starButtons         = new Button[5];

    // Utilisateur connecté récupéré via le SessionManager
    private User currentUser = tn.rouhfan.tools.SessionManager.getInstance().getCurrentUser();

    private VBox contentHost;
    private VBox heroSection;

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        searchField.textProperty().addListener((obs, o, n) -> handleSearch(n));

        if (cbLangue != null) {
            cbLangue.setItems(javafx.collections.FXCollections.observableArrayList(
                    "Arabe (ar)"
            ));
            cbLangue.setValue("Arabe (ar)");
        }
        if (translateLoader != null) { translateLoader.setVisible(false); translateLoader.setManaged(false); }

        buildStarInput();
        loadDataAndCards();
        Platform.runLater(this::initContainerReferences);
    }

    private void initContainerReferences() {
        try {
            if (lblNom.getScene() == null) return;
            Stage stage = (Stage) lblNom.getScene().getWindow();
            BorderPane root = (BorderPane) stage.getScene().getRoot();
            contentHost = (VBox) root.lookup("#contentHost");
            heroSection = (VBox) root.lookup("#heroSection");
        } catch (Exception ex) {
            System.err.println("Erreur conteneurs : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÉTOILES CLIQUABLES
    // ─────────────────────────────────────────────────────────────────────────

    private void buildStarInput() {
        if (starsInput == null) return;
        starsInput.getChildren().clear();

        for (int i = 1; i <= 5; i++) {
            final int note = i;
            Button btn = new Button("☆");
            btn.setStyle("-fx-font-size: 26; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 2; -fx-text-fill: #FFD700;");
            btn.setOnAction(e  -> { selectedNote = note; refreshStarInput(note); });
            btn.setOnMouseEntered(e -> refreshStarInput(note));
            btn.setOnMouseExited(e  -> refreshStarInput(selectedNote));
            starButtons[i - 1] = btn;
            starsInput.getChildren().add(btn);
        }
    }

    private void refreshStarInput(int upTo) {
        for (int i = 0; i < 5; i++)
            starButtons[i].setText(i < upTo ? "★" : "☆");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENREGISTRER LA NOTE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleNoter() {
        if (selectedNote == 0) {
            new Alert(Alert.AlertType.WARNING, "Cliquez sur une étoile pour noter.").showAndWait();
            return;
        }
        try {
            reviewService.noterCours(selectedCours.getId(), currentUser.getId(), selectedNote);
            loadStars(selectedCours.getId()); // rafraîchir moyenne + "votre note"
            new Alert(Alert.AlertType.INFORMATION, "Note enregistrée !").showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    /** Charge la moyenne + la note de l'utilisateur connecté */
    private void loadStars(int coursId) {
        try {
            double moyenne = reviewService.getMoyenne(coursId);
            int    total   = reviewService.getNombreNotes(coursId);

            // Affichage de la moyenne
            if (lblMoyenne != null)
                lblMoyenne.setText(total > 0 ? String.format("%.1f", moyenne) : "-");
            if (lblNbNotes != null)
                lblNbNotes.setText(total + " note" + (total > 1 ? "s" : ""));

            // Étoiles de la moyenne (read-only)
            if (starsDisplay != null) {
                starsDisplay.getChildren().clear();
                int arrondi = (int) Math.round(moyenne);
                for (int i = 1; i <= 5; i++) {
                    Label l = new Label(i <= arrondi ? "★" : "☆");
                    l.setStyle("-fx-font-size: 16; -fx-text-fill: #FFD700;");
                    starsDisplay.getChildren().add(l);
                }
            }

            // Note existante de l'utilisateur
            CoursReview myNote = reviewService.findByCoursAndParticipant(coursId, currentUser.getId());
            if (myNote != null) {
                selectedNote = myNote.getNote();
                refreshStarInput(selectedNote);
                if (lblMonNote != null)
                    lblMonNote.setText("Votre note actuelle : " + myNote.getEtoilesDisplay());
                if (btnNoter != null) btnNoter.setText("✔  Modifier ma note");
            } else {
                selectedNote = 0;
                refreshStarInput(0);
                if (lblMonNote != null) lblMonNote.setText("Vous n'avez pas encore noté ce cours.");
                if (btnNoter != null) btnNoter.setText("✔  Enregistrer ma note");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT CARTES
    // ─────────────────────────────────────────────────────────────────────────

    private void loadDataAndCards() {
        try {
            List<Cours> list = coursService.recupererFront();
            allPublishedCourses = new ArrayList<>(list);
            afficherCartes(allPublishedCourses);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void handleSearch(String query) {
        if (allPublishedCourses == null) return;
        if (query == null || query.isEmpty()) { afficherCartes(allPublishedCourses); return; }
        String q = query.toLowerCase();
        afficherCartes(allPublishedCourses.stream()
                .filter(c -> c.getNom().toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }

    private void afficherCartes(List<Cours> cours) {
        coursesGrid.getChildren().clear();
        for (Cours c : cours) coursesGrid.getChildren().add(createCourseCard(c));
    }

    private String getCoursEmoji(String nom) {
        if (nom == null) return "📚";
        String n = nom.toLowerCase();
        if (n.contains("peinture"))     return "🎨";
        if (n.contains("dessin"))       return "✏️";
        if (n.contains("sculpture"))    return "🗿";
        if (n.contains("musique"))      return "🎵";
        if (n.contains("calligraphie")) return "🖊️";
        return "📚";
    }

    private String getPaletteColor(String niveau) {
        if ("Débutant".equals(niveau))      return "#23BBB7";
        if ("Intermédiaire".equals(niveau)) return "#744D83";
        if ("Avancé".equals(niveau))        return "#23627C";
        return "#23BBB7";
    }

    private VBox createCourseCard(Cours c) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 240);
        card.setAlignment(Pos.CENTER);
        String color = getPaletteColor(c.getNiveau());
        String baseStyle = "-fx-background-color: #E3DBE6; -fx-background-radius: 20;" +
                "-fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 20;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                "-fx-padding: 20; -fx-cursor: hand;";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-scale-x: 1.03; -fx-scale-y: 1.03; -fx-border-width: 4;"));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        Label emojiLabel = new Label(getCoursEmoji(c.getNom()));
        emojiLabel.setStyle("-fx-font-size: 50;");

        Label titleLabel = new Label(c.getNom() != null ? c.getNom().toUpperCase() : "SANS TITRE");
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18; -fx-text-fill: #23627C; -fx-text-alignment: center;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(220);

        // Moyenne sur la carte
        String moyenneStr = "";
        try {
            double moy = reviewService.getMoyenne(c.getId());
            int nb     = reviewService.getNombreNotes(c.getId());
            if (nb > 0) moyenneStr = String.format("★ %.1f  (%d)", moy, nb);
        } catch (SQLException ignored) {}

        Label niveauLabel = new Label(c.getNiveau() != null ? c.getNiveau() : "N/A");
        niveauLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white;" +
                "-fx-padding: 5 18; -fx-background-radius: 20; -fx-background-color: " + color + ";");

        card.getChildren().addAll(emojiLabel, titleLabel, niveauLabel);

        if (!moyenneStr.isEmpty()) {
            Label moyLabel = new Label(moyenneStr);
            moyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #744D83; -fx-font-weight: bold;");
            card.getChildren().add(moyLabel);
        }

        card.setOnMouseClicked(event -> showDetails(c));
        return card;
    }

    // ── TRI ───────────────────────────────────────────────────────────────────
    private int getNiveauOrdre(String n) {
        if (n == null) return 3;
        switch (n) { case "Débutant": return 1; case "Intermédiaire": return 2; default: return 3; }
    }
    @FXML private void trierParNiveauCroissant()  { allPublishedCourses.sort((a,b) -> Integer.compare(getNiveauOrdre(a.getNiveau()), getNiveauOrdre(b.getNiveau()))); handleSearch(searchField.getText()); }
    @FXML private void trierParNiveauDecroissant() { allPublishedCourses.sort((a,b) -> Integer.compare(getNiveauOrdre(b.getNiveau()), getNiveauOrdre(a.getNiveau()))); handleSearch(searchField.getText()); }
    @FXML private void trierParNomCroissant()      { allPublishedCourses.sort((a,b) -> a.getNom().compareToIgnoreCase(b.getNom())); handleSearch(searchField.getText()); }
    @FXML private void trierParNomDecroissant()    { allPublishedCourses.sort((a,b) -> b.getNom().compareToIgnoreCase(a.getNom())); handleSearch(searchField.getText()); }
    @FXML private void resetTri()                  { searchField.clear(); loadDataAndCards(); }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉTAIL
    // ─────────────────────────────────────────────────────────────────────────

    private void showDetails(Cours c) {
        this.selectedCours = c;
        this.isTranslated  = false;
        mainScroll.setVisible(false); mainScroll.setManaged(false);
        detailView.setVisible(true);  detailView.setManaged(true);
        lblNom.setText(c.getNom());
        lblNiveau.setText("Difficulté : " + c.getNiveau());
        taContenu.setText(c.getContenu() != null ? c.getContenu() : "");
        resetTranslateButton();
        loadStars(c.getId()); // ← charge la moyenne + note du user
    }

    @FXML private void retourListe() {
        detailView.setVisible(false); detailView.setManaged(false);
        mainScroll.setVisible(true);  mainScroll.setManaged(true);
        isTranslated = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRADUCTION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleTranslate() {
        if (selectedCours == null) return;
        if (isTranslated) {
            lblNom.setText(selectedCours.getNom());
            taContenu.setText(selectedCours.getContenu() != null ? selectedCours.getContenu() : "");
            isTranslated = false;
            resetTranslateButton();
            return;
        }
        String lang = extractLangCode(cbLangue != null ? cbLangue.getValue() : "Anglais (en)");
        if (translateLoader != null) { translateLoader.setVisible(true); translateLoader.setManaged(true); }
        if (btnTranslate != null) btnTranslate.setDisable(true);
        if (lblTranslateStatus != null) lblTranslateStatus.setText("⏳ Traduction en cours...");

        new Thread(() -> {
            try {
                Translationservice.TranslationResult r = translationService.translateCours(
                        selectedCours.getNom(), selectedCours.getContenu(), lang);
                Platform.runLater(() -> {
                    if (translateLoader != null) { translateLoader.setVisible(false); translateLoader.setManaged(false); }
                    if (btnTranslate != null) { btnTranslate.setDisable(false); btnTranslate.setText("🔄 Voir l'original"); btnTranslate.setStyle("-fx-background-color: #23BBB7; -fx-text-fill: white; -fx-border-radius: 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;"); }
                    if (lblTranslateStatus != null) lblTranslateStatus.setText("✅ Traduit");
                    lblNom.setText(r.nom);
                    taContenu.setText(r.contenu);
                    isTranslated = true;
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (translateLoader != null) { translateLoader.setVisible(false); translateLoader.setManaged(false); }
                    if (btnTranslate != null) btnTranslate.setDisable(false);
                    if (lblTranslateStatus != null) lblTranslateStatus.setText("❌ Erreur");
                });
            }
        }).start();
    }

    private String extractLangCode(String value) {
        if (value == null) return "en";
        int s = value.indexOf('('), e = value.indexOf(')');
        return (s >= 0 && e > s) ? value.substring(s + 1, e) : "en";
    }

    private void resetTranslateButton() {
        if (btnTranslate != null) { btnTranslate.setText("🌐 Traduire"); btnTranslate.setDisable(false); btnTranslate.setStyle("-fx-background-color: white; -fx-text-fill: #23BBB7; -fx-border-color: #23BBB7; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;"); }
        if (lblTranslateStatus != null) lblTranslateStatus.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION QCM
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void goPasserQcm() {
        if (selectedCours == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/QcmPassageView.fxml"));
            Parent qcmView = loader.load();
            QcmPassageController ctrl = loader.getController();
            ctrl.initData(selectedCours, contentHost, heroSection);
            if (heroSection != null) { heroSection.setVisible(false); heroSection.setManaged(false); }
            if (contentHost != null) { contentHost.setVisible(true); contentHost.setManaged(true); contentHost.getChildren().clear(); contentHost.getChildren().add(qcmView); }
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}