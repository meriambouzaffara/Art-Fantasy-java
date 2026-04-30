package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import tn.rouhfan.entities.Magasin;
import tn.rouhfan.services.ArticleService;
import tn.rouhfan.services.MagasinService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur front-office pour la liste des magasins en cartes.
 * Un clic sur "Voir les articles" charge front_magasins.fxml filtré sur ce magasin.
 */
public class FrontListeMagasinsController implements Initializable {

    @FXML private FlowPane  magasinsFlow;
    @FXML private Label     totalLabel;
    @FXML private Label     resultCount;
    @FXML private TextField searchField;
    @FXML private VBox      emptyMessage;

    private final MagasinService magasinService = new MagasinService();
    private final ArticleService articleService = new ArticleService();
    private List<Magasin> allMagasins;

    // Palette de couleurs pour les cartes (identique à la boutique articles)
    private static final String[] CARD_COLORS = {
            "linear-gradient(to bottom right, #241197, #6c2a90)",
            "linear-gradient(to bottom right, #6c2a90, #e84393)",
            "linear-gradient(to bottom right, #0984e3, #74b9ff)",
            "linear-gradient(to bottom right, #00b894, #55efc4)",
            "linear-gradient(to bottom right, #e17055, #fab1a0)"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMagasins();
    }

    // ── Chargement ────────────────────────────────────────────────

    private void loadMagasins() {
        try {
            allMagasins = magasinService.recuperer();
            totalLabel.setText(allMagasins.size() + " magasin"
                    + (allMagasins.size() > 1 ? "s" : "") + " disponible"
                    + (allMagasins.size() > 1 ? "s" : ""));
        } catch (SQLException e) {
            allMagasins = List.of();
            totalLabel.setText("Erreur de chargement");
        }
        renderMagasins(allMagasins);
    }

    // ── Rendu des cartes ──────────────────────────────────────────

    private void renderMagasins(List<Magasin> magasins) {
        magasinsFlow.getChildren().clear();

        boolean empty = magasins == null || magasins.isEmpty();
        emptyMessage.setVisible(empty);
        emptyMessage.setManaged(empty);
        magasinsFlow.setVisible(!empty);
        magasinsFlow.setManaged(!empty);

        if (!empty) {
            resultCount.setText(magasins.size() + " résultat" + (magasins.size() > 1 ? "s" : ""));
            magasins.forEach(m -> magasinsFlow.getChildren().add(buildMagasinCard(m)));
        } else {
            resultCount.setText("0 résultat");
        }
    }

    /**
     * Construit une carte magasin, au même style que les cartes articles.
     */
    private VBox buildMagasinCard(Magasin magasin) {
        VBox card = new VBox(0);
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.08), 12, 0, 0, 4); "
                + "-fx-cursor: hand;");

        // ── Zone image / couleur ──────────────────────────────────
        StackPane header = new StackPane();
        header.setPrefHeight(140);
        header.setPrefWidth(240);
        String bgColor = magasin.getId() != null
                ? CARD_COLORS[(int)(magasin.getId() % CARD_COLORS.length)]
                : CARD_COLORS[0];
        header.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16 16 0 0;");

        // Icône magasin centrale
        Label icon = new Label("🏪");
        icon.setStyle("-fx-font-size: 52; -fx-text-fill: rgba(255,255,255,0.92);");
        header.getChildren().add(icon);

        // ── Contenu texte ────────────────────────────────────────
        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 14 16 16 16;");

        // Nom du magasin
        Label nom = new Label(magasin.getNom() != null ? magasin.getNom() : "Sans nom");
        nom.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #241197; -fx-wrap-text: true;");
        nom.setWrapText(true);
        nom.setMaxWidth(208);

        // Adresse
        String adr = magasin.getAdresse() != null && !magasin.getAdresse().isEmpty()
                ? magasin.getAdresse() : "—";
        Label adresse = new Label("📍 " + adr);
        adresse.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72; -fx-wrap-text: true;");
        adresse.setWrapText(true);
        adresse.setMaxWidth(208);

        // Téléphone
        String tel = magasin.getTel() != null && !magasin.getTel().isEmpty()
                ? magasin.getTel() : "—";
        Label telephone = new Label("📞 " + tel);
        telephone.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72;");

        // Email
        String email = magasin.getEmail() != null && !magasin.getEmail().isEmpty()
                ? magasin.getEmail() : "—";
        Label emailLabel = new Label("✉ " + email);
        emailLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6c2a90;");
        emailLabel.setWrapText(true);
        emailLabel.setMaxWidth(208);

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(8);

        // Bouton "Voir les articles"
        Button btn = new Button("Voir les articles →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #241197; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9 0; "
                + "-fx-cursor: hand; -fx-font-size: 12;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #6c2a90; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #241197; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand;"));
        btn.setOnAction(e -> openArticlesDeMagasin(magasin));

        content.getChildren().addAll(nom, adresse, telephone, emailLabel, sep, btn);
        card.getChildren().addAll(header, content);

        // Hover sur la carte
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; "
                        + "-fx-effect: dropshadow(three-pass-box, rgba(201,168,73,0.25), 20, 0, 0, 8); "
                        + "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; "
                        + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.08), 12, 0, 0, 4); "
                        + "-fx-cursor: hand;"));

        return card;
    }

    // ── Navigation vers les articles du magasin ───────────────────

    private void openArticlesDeMagasin(Magasin magasin) {
        try {
            // Remonter jusqu'au contentHost dans la scène
            VBox contentHost = (VBox) magasinsFlow.getScene().lookup("#contentHost");
            if (contentHost == null) {
                System.err.println("[FrontListeMagasins] contentHost introuvable dans la scène.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/front/front_magasins.fxml"));
            Node view = loader.load();
            FrontMagasinController controller = loader.getController();

            // Pré-sélectionner le magasin cliqué
            controller.setSelectedMagasin(magasin);

            contentHost.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("[FrontListeMagasins] Erreur chargement front_magasins: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Recherche ────────────────────────────────────────────────

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderMagasins(allMagasins);
            return;
        }
        List<Magasin> filtered = allMagasins.stream()
                .filter(m -> (m.getNom()     != null && m.getNom().toLowerCase().contains(query))
                          || (m.getAdresse() != null && m.getAdresse().toLowerCase().contains(query))
                          || (m.getEmail()   != null && m.getEmail().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        renderMagasins(filtered);
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        renderMagasins(allMagasins);
    }
}
