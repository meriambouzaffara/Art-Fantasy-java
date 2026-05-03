package tn.rouhfan.ui.back;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import tn.rouhfan.entities.Magasin;
import javafx.scene.control.Tooltip;
import tn.rouhfan.services.ArticleService;
import tn.rouhfan.services.MagasinService;
import tn.rouhfan.tools.OpenStreetMapHelper;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MagasinController implements Initializable {

    // ── FXML barre ────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     countLabel;
    @FXML private FlowPane  magasinsFlow;

    // ── FXML formulaire ───────────────────────────────────────────
    @FXML private VBox      formPanel;
    @FXML private Label     formTitle;
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField telField;
    @FXML private TextField emailField;
    @FXML private TextField latField;
    @FXML private TextField lonField;
    @FXML private WebView   locationMap;
    @FXML private Label     selectedPositionLabel;
    @FXML private Label     errorLabel;

    // ── État ──────────────────────────────────────────────────────
    private MagasinService magasinService;
    private ArticleService articleService;
    private List<Magasin>  allMagasins;
    private boolean isEditMode      = false;
    private Magasin selectedForEdit = null;
    private final MapBridge mapBridge = new MapBridge();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        magasinService = new MagasinService();
        articleService = new ArticleService();
        setupMap();
        loadAndRender();
    }

    private void setupMap() {
        if (locationMap == null) {
            return;
        }
        locationMap.setContextMenuEnabled(false);
        WebEngine engine = locationMap.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", mapBridge);
            }
        });
        loadPickerMap(null, null);
    }

    // ── Chargement + rendu ────────────────────────────────────────

    private void loadAndRender() {
        try {
            allMagasins = magasinService.recuperer();
        } catch (SQLException e) {
            allMagasins = List.of();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Chargement impossible : " + e.getMessage());
        }
        renderCards(allMagasins);
    }

    private void renderCards(List<Magasin> magasins) {
        magasinsFlow.getChildren().clear();
        countLabel.setText(magasins.size() + " magasin(s)");
        for (Magasin m : magasins) {
            int nb = 0;
            try { nb = articleService.recupererParMagasin(m).size(); } catch (SQLException ignored) {}
            magasinsFlow.getChildren().add(buildCard(m, nb));
        }
    }

    private VBox buildCard(Magasin m, int nbArticles) {
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 4);");

        // Bandeau coloré
        StackPane banner = new StackPane();
        banner.setPrefHeight(90);
        banner.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #241197, #6c2a90);" +
                        "-fx-background-radius: 16 16 0 0;");
        Label icon = new Label("🖌️");
        icon.setStyle("-fx-font-size: 38;");
        Label badge = new Label(nbArticles + " article" + (nbArticles > 1 ? "s" : ""));
        badge.setStyle(
                "-fx-background-color: #c9a849; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-font-size: 11; -fx-background-radius: 12; -fx-padding: 3 10;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        badge.setTranslateX(-10);
        badge.setTranslateY(10);
        banner.getChildren().addAll(icon, badge);

        // Contenu
        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 14 16 4 16;");
        Label nom     = new Label(m.getNom()     != null ? m.getNom()     : "—");
        nom.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #241197;");
        nom.setWrapText(true);
        Label adresse = new Label("📍 " + (m.getAdresse() != null ? m.getAdresse() : "—"));
        adresse.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72;");
        Label tel     = new Label("📞 " + (m.getTel()     != null ? m.getTel()     : "—"));
        tel.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72;");
        Label email   = new Label("✉️ " + (m.getEmail()   != null ? m.getEmail()   : "—"));
        email.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72;");
        content.getChildren().addAll(nom, adresse, tel, email);

        // Boutons
        VBox actions = new VBox(8);
        actions.setStyle("-fx-padding: 12 16 16 16;");

        Button voirBtn = new Button("📦 Voir les articles");
        voirBtn.setMaxWidth(Double.MAX_VALUE);
        voirBtn.setStyle(
                "-fx-background-color: #241197; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 12;");
        voirBtn.setOnMouseEntered(e -> voirBtn.setStyle(
                "-fx-background-color: #6c2a90; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand;"));
        voirBtn.setOnMouseExited(e -> voirBtn.setStyle(
                "-fx-background-color: #241197; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand;"));
        voirBtn.setOnAction(e -> navigateToArticles(m));

        // ── Bouton Itinéraire ─────────────────────────────────────
        boolean hasCoords = OpenStreetMapHelper.hasCoordinates(m);
        Button itinBtn = new Button("🗺️ Itinéraire");
        itinBtn.setMaxWidth(Double.MAX_VALUE);
        itinBtn.setDisable(!hasCoords);
        itinBtn.setStyle("-fx-background-color: " + (hasCoords ? "#00b894" : "#e0ddf0")
                + "; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand; -fx-font-size: 12;");
        if (!hasCoords) {
            itinBtn.setTooltip(new Tooltip("Position GPS non configurée pour ce magasin"));
        }
        itinBtn.setOnAction(e -> showBackItineraire(m));
        // ─────────────────────────────────────────────────────────

        HBox editDelete = new HBox(8);
        editDelete.setAlignment(Pos.CENTER);
        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle(
                "-fx-background-color: #f0f2f9; -fx-text-fill: #241197; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;");
        editBtn.setOnAction(e -> startEdit(m));
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle(
                "-fx-background-color: #fff0f0; -fx-text-fill: #d63031; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;" +
                        "-fx-border-color: #d63031; -fx-border-radius: 8;");
        deleteBtn.setOnAction(e -> deleteMagasin(m));
        editDelete.getChildren().addAll(editBtn, deleteBtn);
        actions.getChildren().addAll(voirBtn, itinBtn, editDelete);

        card.getChildren().addAll(banner, content, actions);
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(201,168,73,0.25), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 4);"));
        return card;
    }

    // ── Itinéraire ────────────────────────────────────────────────

    private void showBackItineraire(Magasin m) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🗺️ Itinéraire — " + m.getNom());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(740, 540);

        WebView routeMap = new WebView();
        routeMap.setContextMenuEnabled(false);
        routeMap.setPrefSize(720, 480);
        routeMap.getEngine().loadContent(
                OpenStreetMapHelper.buildRouteMap(m.getLatitude(), m.getLongitude(), m.getNom()));

        VBox content = new VBox(8);
        Label hint = new Label(
                "📍 Position : " + (m.getAdresse() != null ? m.getAdresse() : "")
                + String.format("  (lat %.5f, lon %.5f)", m.getLatitude(), m.getLongitude()));
        hint.setStyle("-fx-font-size: 12; -fx-text-fill: #5a4a72;");
        Label hint2 = new Label("Entrez votre adresse de départ dans la barre en haut de la carte.");
        hint2.setStyle("-fx-font-size: 11; -fx-text-fill: #6c2a90;");
        content.getChildren().addAll(hint, hint2, routeMap);
        dialog.getDialogPane().setContent(content);

        dialog.setOnShown(ev -> Platform.runLater(() ->
                routeMap.getEngine().executeScript(
                        "if(window.map){map.invalidateSize();setTimeout(function(){map.invalidateSize();},200);}")));
        dialog.showAndWait();
    }

    // ── Navigation directe vers ArticleView ───────────────────────
    // Utilise getScene().lookup("#contentHost") — pas de service externe
    private void navigateToArticles(Magasin m) {
        try {
            VBox contentHost = (VBox) magasinsFlow.getScene().lookup("#contentHost");
            Label pageTitle  = (Label) magasinsFlow.getScene().lookup("#pageTitle");

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/back/ArticleView.fxml"));
            Node view = loader.load();
            ArticleController ctrl = loader.getController();

            if (pageTitle != null) pageTitle.setText("📦 Articles — " + m.getNom());
            contentHost.getChildren().setAll(view);
            ctrl.filterByMagasin(m);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les articles : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Recherche ─────────────────────────────────────────────────
    @FXML
    private void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) { renderCards(allMagasins); return; }
        List<Magasin> f = allMagasins.stream()
                .filter(m -> (m.getNom()     != null && m.getNom().toLowerCase().contains(q))
                        || (m.getAdresse() != null && m.getAdresse().toLowerCase().contains(q))
                        || (m.getEmail()   != null && m.getEmail().toLowerCase().contains(q))
                        || (m.getTel()     != null && m.getTel().contains(q)))
                .collect(Collectors.toList());
        renderCards(f);
    }

    @FXML private void refresh(ActionEvent e)    { searchField.clear(); hideForm(null); loadAndRender(); }
    @FXML private void showAddForm(ActionEvent e){ isEditMode=false; selectedForEdit=null; formTitle.setText("➕ Nouveau Magasin"); clearForm(); setFormVisible(true); loadPickerMap(null, null); }

    private void startEdit(Magasin m) {
        isEditMode=true; selectedForEdit=m;
        formTitle.setText("✏️ Modifier — " + m.getNom());
        populateForm(m); setFormVisible(true);
    }

    @FXML
    private void saveMagasin(ActionEvent event) {
        hideError();
        String nom=nomField.getText().trim(), adresse=adresseField.getText().trim(),
                tel=telField.getText().trim(),  email=emailField.getText().trim(),
                latTxt=latField.getText().trim(), lonTxt=lonField.getText().trim();
        if (nom.isEmpty()||adresse.isEmpty()||tel.isEmpty()||email.isEmpty()) {
            showFormError("Nom, Adresse, Téléphone et Email sont obligatoires."); return; }
        if (!email.contains("@"))  { showFormError("Email invalide."); return; }
        if (!tel.matches("\\d{8}")) {
            showFormError("Téléphone invalide : exactement 8 chiffres requis."); return;
        }
        char firstDigit = tel.charAt(0);
        if (firstDigit != '2' && firstDigit != '5' && firstDigit != '9') {
            showFormError("Téléphone invalide : doit commencer par 2 (Ooredoo), 5 (Orange) ou 9 (Telecom)."); return;
        }
        Double lat=null, lon=null;
        try {
            if (!latTxt.isEmpty() || !lonTxt.isEmpty()) {
                if (latTxt.isEmpty() || lonTxt.isEmpty()) {
                    showFormError("Latitude et longitude doivent etre renseignees ensemble."); return;
                }
                lat = Double.parseDouble(latTxt);
                lon = Double.parseDouble(lonTxt);
                if (!OpenStreetMapHelper.isValidLatitude(lat) || !OpenStreetMapHelper.isValidLongitude(lon)) {
                    showFormError("Latitude/Longitude hors limites."); return;
                }
            }
        } catch (NumberFormatException e) { showFormError("Latitude/Longitude invalides."); return; }
        try {
            if (isEditMode && selectedForEdit != null) {
                selectedForEdit.setNom(nom); selectedForEdit.setAdresse(adresse);
                selectedForEdit.setTel(tel); selectedForEdit.setEmail(email);
                selectedForEdit.setLatitude(lat); selectedForEdit.setLongitude(lon);
                magasinService.modifier(selectedForEdit);
            } else {
                magasinService.ajouter(new Magasin(nom, adresse, tel, email, lat, lon));
            }
            setFormVisible(false); clearForm(); loadAndRender();
        } catch (SQLException e) { showFormError("Erreur DB : " + e.getMessage()); }
    }

    @FXML private void hideForm(ActionEvent e) { setFormVisible(false); clearForm(); hideError(); }

    @FXML
    private void centerMapOnFields(ActionEvent e) {
        try {
            Double lat = latField.getText().trim().isEmpty() ? null : Double.parseDouble(latField.getText().trim());
            Double lon = lonField.getText().trim().isEmpty() ? null : Double.parseDouble(lonField.getText().trim());
            if (lat != null && lon != null
                    && (!OpenStreetMapHelper.isValidLatitude(lat) || !OpenStreetMapHelper.isValidLongitude(lon))) {
                showFormError("Latitude/Longitude hors limites.");
                return;
            }
            loadPickerMap(lat, lon);
        } catch (NumberFormatException ex) {
            showFormError("Coordonnees invalides.");
        }
    }

    private void deleteMagasin(Magasin m) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Confirmer"); c.setHeaderText("Supprimer « " + m.getNom() + " » ?");
        c.setContentText("Action irréversible.");
        c.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { magasinService.supprimer(m.getId().intValue()); loadAndRender(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR,"Erreur",e.getMessage()); }
            }
        });
    }

    private void populateForm(Magasin m) {
        nomField.setText(orEmpty(m.getNom())); adresseField.setText(orEmpty(m.getAdresse()));
        telField.setText(orEmpty(m.getTel())); emailField.setText(orEmpty(m.getEmail()));
        latField.setText(m.getLatitude()  != null ? String.valueOf(m.getLatitude())  : "");
        lonField.setText(m.getLongitude() != null ? String.valueOf(m.getLongitude()) : "");
        loadPickerMap(m.getLatitude(), m.getLongitude());
    }
    private void clearForm()                     { nomField.clear();adresseField.clear();telField.clear();emailField.clear();latField.clear();lonField.clear(); updateSelectedPosition(null, null); }
    private void setFormVisible(boolean v)       {
        formPanel.setVisible(v); formPanel.setManaged(v);
        if (v && locationMap != null) {
            Platform.runLater(() -> locationMap.getEngine().executeScript("if(window.map){setTimeout(function(){map.invalidateSize();},80);}"));
        }
    }
    private void showFormError(String msg)       { errorLabel.setText("⚠️ "+msg); errorLabel.setVisible(true); errorLabel.setManaged(true); }
    private void hideError()                     { errorLabel.setVisible(false); errorLabel.setManaged(false); }
    private String orEmpty(String s)             { return s != null ? s : ""; }
    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a=new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void loadPickerMap(Double latitude, Double longitude) {
        if (locationMap == null) {
            return;
        }
        locationMap.getEngine().loadContent(OpenStreetMapHelper.buildPickerMap(latitude, longitude));
        updateSelectedPosition(latitude, longitude);
    }

    private void updateSelectedPosition(Double latitude, Double longitude) {
        if (selectedPositionLabel == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            selectedPositionLabel.setText("Aucune position selectionnee");
        } else {
            selectedPositionLabel.setText("Position: " + formatCoordinate(latitude) + ", " + formatCoordinate(longitude));
        }
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    public class MapBridge {
        public void positionSelected(double latitude, double longitude) {
            Platform.runLater(() -> {
                latField.setText(formatCoordinate(latitude));
                lonField.setText(formatCoordinate(longitude));
                updateSelectedPosition(latitude, longitude);
                hideError();
            });
        }
    }
}
