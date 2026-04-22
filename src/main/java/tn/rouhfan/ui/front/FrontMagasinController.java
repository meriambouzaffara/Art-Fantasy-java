package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.Magasin;
import tn.rouhfan.services.ArticleService;
import tn.rouhfan.services.MagasinService;
import tn.rouhfan.services.PanierService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.tools.OpenStreetMapHelper;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur front-office pour la boutique Magasin.
 * Affiche les articles sous forme de cartes avec recherche, filtre et tri.
 */
public class FrontMagasinController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private TextField         searchField;
    @FXML private ComboBox<Magasin> magasinFilter;
    @FXML private ComboBox<String>  sortCombo;
    @FXML private FlowPane          articlesFlow;
    @FXML private Label             totalLabel;
    @FXML private Label             resultCount;
    @FXML private VBox              emptyMessage;
    @FXML private WebView           magasinMap;
    @FXML private Label             selectedMagasinLabel;
    @FXML private Label             selectedMagasinAddress;

    // ── État interne ──────────────────────────────────────────────
    private ArticleService articleService;
    private MagasinService magasinService;
    private final PanierService panierService = PanierService.getInstance();
    private List<Article>  allArticles;
    private List<Magasin>  allMagasins;

    // ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        articleService = new ArticleService();
        magasinService = new MagasinService();

        setupSortCombo();
        loadData();
        setupFilters();
        updateMagasinMap(null);
    }

    private void setupSortCombo() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Par défaut",
                "Prix croissant",
                "Prix décroissant",
                "Titre A → Z",
                "Titre Z → A",
                "Stock disponible"
        ));
        sortCombo.getSelectionModel().selectFirst();
    }

    private void loadData() {
        try {
            allMagasins = magasinService.recuperer();
            allArticles = articleService.recuperer();

            // Enrichir les articles avec le nom du magasin
            for (Article a : allArticles) {
                if (a.getMagasin() != null && a.getMagasin().getId() != null) {
                    allMagasins.stream()
                            .filter(m -> m.getId().equals(a.getMagasin().getId()))
                            .findFirst()
                            .ifPresent(a::setMagasin);
                }
            }

            totalLabel.setText(allArticles.size() + " articles disponibles");
        } catch (SQLException e) {
            allArticles = List.of();
            allMagasins = List.of();
            totalLabel.setText("Erreur de chargement");
        }
        renderArticles(allArticles);
    }

    private void setupFilters() {
        // ComboBox filtre magasin
        Magasin tous = new Magasin();
        tous.setNom("Tous les magasins");

        magasinFilter.setItems(FXCollections.observableArrayList());
        magasinFilter.getItems().add(tous);
        magasinFilter.getItems().addAll(allMagasins);
        magasinFilter.getSelectionModel().selectFirst();

        magasinFilter.setConverter(new javafx.util.StringConverter<Magasin>() {
            @Override public String toString(Magasin m) {
                return m != null ? m.getNom() : "";
            }
            @Override public Magasin fromString(String s) { return null; }
        });
        updateMagasinMap(null);
    }

    // ── Rendu des cartes ──────────────────────────────────────────

    private void renderArticles(List<Article> articles) {
        articlesFlow.getChildren().clear();

        boolean empty = articles == null || articles.isEmpty();
        emptyMessage.setVisible(empty);
        emptyMessage.setManaged(empty);
        articlesFlow.setVisible(!empty);
        articlesFlow.setManaged(!empty);

        if (!empty) {
            resultCount.setText(articles.size() + " résultat" + (articles.size() > 1 ? "s" : ""));
            articles.forEach(a -> articlesFlow.getChildren().add(buildArticleCard(a)));
        } else {
            resultCount.setText("0 résultat");
        }
    }

    /**
     * Construit une carte article au design Art Fantasy.
     */
    private VBox buildArticleCard(Article article) {
        VBox card = new VBox(0);
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.08), 12, 0, 0, 4); "
                + "-fx-cursor: hand;");

        // Image placeholder (couleur selon le magasin)
        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setPrefHeight(160);
        imagePlaceholder.setPrefWidth(240);
        String bgColor = getColorForMagasin(article.getMagasin());
        imagePlaceholder.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16 16 0 0;");

        // Icône centrale
        imagePlaceholder.getChildren().add(createArticleVisual(article, 240, 160, 52));

        // Badge stock
        Label stockBadge = new Label(getStockBadgeText(article.getStock()));
        stockBadge.setStyle("-fx-background-color: " + getStockBadgeColor(article.getStock()) + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;"
                + "-fx-background-radius: 12; -fx-padding: 3 8;");
        StackPane.setAlignment(stockBadge, Pos.TOP_RIGHT);
        stockBadge.setTranslateX(-10);
        stockBadge.setTranslateY(10);
        imagePlaceholder.getChildren().add(stockBadge);

        // Contenu texte
        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 14 16 16 16;");

        // Titre
        Label titre = new Label(article.getTitre() != null ? article.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #241197; -fx-wrap-text: true;");
        titre.setWrapText(true);
        titre.setMaxWidth(208);

        // Description courte
        String descText = article.getDescription() != null && !article.getDescription().isEmpty()
                ? (article.getDescription().length() > 60
                ? article.getDescription().substring(0, 57) + "..."
                : article.getDescription())
                : "—";
        Label description = new Label(descText);
        description.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72; -fx-wrap-text: true;");
        description.setWrapText(true);
        description.setMaxWidth(208);

        // Magasin
        String magNom = article.getMagasin() != null && article.getMagasin().getNom() != null
                ? article.getMagasin().getNom() : "—";
        Label magasin = new Label("🏪 " + magNom);
        magasin.setStyle("-fx-font-size: 11; -fx-text-fill: #6c2a90; -fx-font-weight: 500;");

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(8);

        // Prix
        Label prix = new Label(String.format("%.2f DT", article.getPrix()));
        prix.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #c9a849;");

        // Bouton action
        Button btn = new Button("Voir les détails →");
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
        btn.setOnAction(e -> showArticleDetail(article));

        Button buyBtn = new Button("Acheter");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        buyBtn.setDisable(article.getStock() != null && article.getStock() <= 0);
        buyBtn.setStyle("-fx-background-color: #c9a849; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9 0; "
                + "-fx-cursor: hand; -fx-font-size: 12;");
        buyBtn.setOnAction(e -> addToCart(article));

        HBox actions = new HBox(8, btn, buyBtn);
        HBox.setHgrow(btn, Priority.ALWAYS);
        HBox.setHgrow(buyBtn, Priority.ALWAYS);

        content.getChildren().addAll(titre, description, magasin, sep, prix, actions);

        card.getChildren().addAll(imagePlaceholder, content);

        // Effet hover sur la carte
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

    // ── Actions filtres ───────────────────────────────────────────

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onFilterMagasin(ActionEvent event) {
        updateMagasinMap(magasinFilter.getSelectionModel().getSelectedItem());
        applyFilters();
    }

    @FXML
    private void onSort(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        searchField.clear();
        magasinFilter.getSelectionModel().selectFirst();
        sortCombo.getSelectionModel().selectFirst();
        updateMagasinMap(null);
        renderArticles(allArticles);
    }

    private void applyFilters() {
        String query     = searchField.getText().trim().toLowerCase();
        Magasin magFilter = magasinFilter.getSelectionModel().getSelectedItem();
        String  sort     = sortCombo.getSelectionModel().getSelectedItem();

        List<Article> filtered = allArticles.stream()
                .filter(a -> {
                    boolean matchText = query.isEmpty()
                            || (a.getTitre() != null && a.getTitre().toLowerCase().contains(query))
                            || (a.getDescription() != null && a.getDescription().toLowerCase().contains(query));
                    boolean matchMag = magFilter == null
                            || "Tous les magasins".equals(magFilter.getNom())
                            || (a.getMagasin() != null && magFilter.getId() != null
                            && magFilter.getId().equals(a.getMagasin().getId()));
                    return matchText && matchMag;
                })
                .collect(Collectors.toList());

        // Tri
        if (sort != null) {
            switch (sort) {
                case "Prix croissant":
                    filtered.sort(Comparator.comparingDouble(Article::getPrix));
                    break;
                case "Prix décroissant":
                    filtered.sort(Comparator.comparingDouble(Article::getPrix).reversed());
                    break;
                case "Titre A → Z":
                    filtered.sort(Comparator.comparing(a -> a.getTitre() != null ? a.getTitre() : ""));
                    break;
                case "Titre Z → A":
                    filtered.sort(Comparator.comparing((Article a) -> a.getTitre() != null ? a.getTitre() : "").reversed());
                    break;
                case "Stock disponible":
                    filtered.sort(Comparator.comparingInt(a -> -(a.getStock() != null ? a.getStock() : 0)));
                    break;
                default:
                    break;
            }
        }
        renderArticles(filtered);
        updateMagasinMap(magFilter);
    }

    // ── Détail article ────────────────────────────────────────────

    private void addToCart(Article article) {
        boolean added = panierService.addArticle(article);
        Alert alert = new Alert(added ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(added ? "Panier" : "Stock indisponible");
        alert.setHeaderText(null);
        alert.setContentText(added
                ? "Article ajoute au panier."
                : "Impossible d'ajouter cet article: stock indisponible ou quantite maximale atteinte.");
        alert.showAndWait();
    }

    private void updateMagasinMap(Magasin selected) {
        if (magasinMap == null) {
            return;
        }

        boolean allStores = selected == null || "Tous les magasins".equals(selected.getNom());
        if (allStores) {
            magasinMap.getEngine().loadContent(OpenStreetMapHelper.buildDisplayMap(allMagasins));
            if (selectedMagasinLabel != null) {
                selectedMagasinLabel.setText("Carte des magasins");
            }
            if (selectedMagasinAddress != null) {
                selectedMagasinAddress.setText("Selectionnez un magasin pour voir sa position exacte.");
            }
            return;
        }

        magasinMap.getEngine().loadContent(OpenStreetMapHelper.buildDisplayMap(selected));
        if (selectedMagasinLabel != null) {
            selectedMagasinLabel.setText(selected.getNom() != null ? selected.getNom() : "Magasin");
        }
        if (selectedMagasinAddress != null) {
            String coords = OpenStreetMapHelper.hasCoordinates(selected)
                    ? String.format("Lat %.6f, Lon %.6f", selected.getLatitude(), selected.getLongitude())
                    : "Position non renseignee";
            selectedMagasinAddress.setText((selected.getAdresse() != null ? selected.getAdresse() + " - " : "") + coords);
        }
    }

    private void showArticleDetailLegacy(Article article) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Détail de l'article");
        dialog.setHeaderText(article.getTitre());

        String content = String.format(
                "💰 Prix : %.2f DT\n"
                        + "📦 Stock : %d unités\n"
                        + "🏪 Magasin : %s\n"
                        + "📝 Description : %s",
                article.getPrix(),
                article.getStock() != null ? article.getStock() : 0,
                article.getMagasin() != null && article.getMagasin().getNom() != null
                        ? article.getMagasin().getNom() : "—",
                article.getDescription() != null ? article.getDescription() : "Aucune description"
        );
        dialog.setContentText(content);
        dialog.showAndWait();
    }

    // ── Utilitaires visuels ───────────────────────────────────────

    private void showArticleDetail(Article article) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detail de l'article");
        dialog.setHeaderText(article.getTitre() != null ? article.getTitre() : "Article");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox details = new VBox(10);
        details.setPrefWidth(330);
        details.setPadding(new Insets(8));

        Label price = detailLabel("Prix", String.format("%.2f DT", article.getPrix()));
        Label stock = detailLabel("Stock", (article.getStock() != null ? article.getStock() : 0) + " unite(s)");
        Label store = detailLabel("Magasin", article.getMagasin() != null && article.getMagasin().getNom() != null
                ? article.getMagasin().getNom() : "-");
        Label description = detailLabel("Description", article.getDescription() != null && !article.getDescription().isBlank()
                ? article.getDescription() : "Aucune description");
        description.setWrapText(true);

        Button buyButton = new Button("Acheter");
        buyButton.setMaxWidth(Double.MAX_VALUE);
        buyButton.setDisable(article.getStock() != null && article.getStock() <= 0);
        buyButton.setStyle("-fx-background-color: #c9a849; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 10; -fx-padding: 9 0; -fx-cursor: hand;");
        buyButton.setOnAction(event -> addToCart(article));

        details.getChildren().addAll(price, stock, store, description, buyButton);

        VBox mapBox = new VBox(8);
        mapBox.setPrefWidth(340);
        Label mapTitle = new Label("Position du magasin");
        mapTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #241197;");
        mapBox.getChildren().add(mapTitle);

        WebView detailMap = null;
        if (OpenStreetMapHelper.hasCoordinates(article.getMagasin())) {
            detailMap = new WebView();
            detailMap.setContextMenuEnabled(false);
            detailMap.setPrefSize(320, 220);
            detailMap.setMinSize(320, 220);
            detailMap.setMaxSize(320, 220);
            detailMap.getEngine().loadContent(OpenStreetMapHelper.buildDisplayMap(article.getMagasin()));
            mapBox.getChildren().add(detailMap);
        } else {
            Label noPosition = new Label("Position non renseignee pour ce magasin.");
            noPosition.setWrapText(true);
            noPosition.setStyle("-fx-text-fill: #5a4a72;");
            mapBox.getChildren().add(noPosition);
        }

        HBox content = new HBox(18, details, mapBox);
        content.setPadding(new Insets(6));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(760);

        WebView finalDetailMap = detailMap;
        if (finalDetailMap != null) {
            dialog.setOnShown(event -> Platform.runLater(() ->
                    finalDetailMap.getEngine().executeScript(
                            "if(window.map){map.invalidateSize();setTimeout(function(){map.invalidateSize();},180);}")));
        }

        dialog.showAndWait();
    }

    private Label detailLabel(String label, String value) {
        Label result = new Label(label + " : " + value);
        result.setStyle("-fx-text-fill: #2d1b4e; -fx-font-size: 13;");
        return result;
    }

    private String getColorForMagasin(Magasin m) {
        if (m == null || m.getId() == null) return "linear-gradient(to bottom right, #241197, #6c2a90)";
        String[] colors = {
                "linear-gradient(to bottom right, #241197, #6c2a90)",
                "linear-gradient(to bottom right, #6c2a90, #e84393)",
                "linear-gradient(to bottom right, #c9a849, #e4c76a)",
                "linear-gradient(to bottom right, #0984e3, #74b9ff)",
                "linear-gradient(to bottom right, #00b894, #55efc4)"
        };
        return colors[(int)(m.getId() % colors.length)];
    }

    private String getIconForArticle(Article a) {
        if (a.getTitre() == null) return "🎨";
        String t = a.getTitre().toLowerCase();
        if (t.contains("peinture") || t.contains("tableau")) return "🖼️";
        if (t.contains("sculpture")) return "🗿";
        if (t.contains("dragon")) return "🐉";
        if (t.contains("bijou") || t.contains("bijoux")) return "💎";
        if (t.contains("livre") || t.contains("book")) return "📚";
        if (t.contains("poster") || t.contains("affiche")) return "📜";
        return "🎨";
    }

    private Node createArticleVisual(Article article, double width, double height, double iconSize) {
        String imageUrl = ImageUtils.getAbsolutePath(article.getImage());
        if (imageUrl != null) {
            ImageView imageView = new ImageView(new Image(imageUrl, width, height, false, true, true));
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            return imageView;
        }

        Label icon = new Label(getIconForArticle(article));
        icon.setStyle("-fx-font-size: " + iconSize + "; -fx-text-fill: rgba(255,255,255,0.9);");
        return icon;
    }

    private String getStockBadgeText(Integer stock) {
        if (stock == null || stock == 0) return "Rupture";
        if (stock <= 3) return stock + " restant" + (stock > 1 ? "s" : "");
        return "En stock";
    }

    private String getStockBadgeColor(Integer stock) {
        if (stock == null || stock == 0) return "#d63031";
        if (stock <= 3) return "#e17055";
        return "#00b894";
    }
}
