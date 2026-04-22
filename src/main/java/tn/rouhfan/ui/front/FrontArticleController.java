package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.Magasin;
import tn.rouhfan.services.ArticleService;
import tn.rouhfan.services.MagasinService;
import tn.rouhfan.services.PanierService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.ui.Router;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontArticleController implements Initializable {

    @FXML private Label magasinTitle;
    @FXML private GridPane articlesGrid;

    private final ArticleService articleService = new ArticleService();
    private final MagasinService magasinService = new MagasinService();
    private final PanierService panierService = PanierService.getInstance();
    private Magasin currentMagasin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renderArticles(loadArticles());
    }

    public void setMagasin(Magasin magasin) {
        this.currentMagasin = magasin;
        magasinTitle.setText(magasin != null && magasin.getNom() != null ? magasin.getNom() : "Articles");
        renderArticles(loadArticles());
    }

    @FXML
    private void goBackToMagasins() {
        if (articlesGrid.getScene() == null) {
            return;
        }
        VBox contentHost = (VBox) articlesGrid.getScene().lookup("#contentHost");
        if (contentHost != null) {
            Router.setContent(contentHost, "/ui/front/front_magasins.fxml");
        }
    }

    private List<Article> loadArticles() {
        try {
            List<Magasin> magasins = magasinService.recuperer();
            List<Article> articles = currentMagasin != null
                    ? articleService.recupererParMagasin(currentMagasin)
                    : articleService.recuperer();
            for (Article article : articles) {
                if (article.getMagasin() != null && article.getMagasin().getId() != null) {
                    magasins.stream()
                            .filter(m -> m.getId().equals(article.getMagasin().getId()))
                            .findFirst()
                            .ifPresent(article::setMagasin);
                }
            }
            if (currentMagasin != null) {
                return articles.stream()
                        .filter(article -> article.getMagasin() != null
                                && currentMagasin.getId() != null
                                && currentMagasin.getId().equals(article.getMagasin().getId()))
                        .collect(Collectors.toList());
            }
            return articles;
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            return List.of();
        }
    }

    private void renderArticles(List<Article> articles) {
        articlesGrid.getChildren().clear();
        if (magasinTitle != null && currentMagasin == null) {
            magasinTitle.setText("Tous les articles");
        }
        int column = 0;
        int row = 0;
        for (Article article : articles) {
            articlesGrid.add(buildCard(article), column, row);
            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
    }

    private VBox buildCard(Article article) {
        StackPane imagePane = createArticleImagePane(article);

        Label title = new Label(article.getTitre() != null ? article.getTitre() : "Article");
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #241197;");

        Label price = new Label(String.format("%.2f DT", article.getPrix()));
        price.setStyle("-fx-font-size: 19; -fx-font-weight: 800; -fx-text-fill: #c9a849;");

        Label store = new Label(article.getMagasin() != null && article.getMagasin().getNom() != null
                ? article.getMagasin().getNom() : "Magasin");
        store.setStyle("-fx-font-size: 12; -fx-text-fill: #5a4a72;");

        Button buy = new Button("Acheter");
        buy.setMaxWidth(Double.MAX_VALUE);
        buy.setDisable(article.getStock() != null && article.getStock() <= 0);
        buy.setStyle("-fx-background-color: #241197; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        buy.setOnAction(event -> {
            boolean added = panierService.addArticle(article);
            showAlert(added ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                    added ? "Panier" : "Stock",
                    added ? "Article ajoute au panier." : "Article indisponible.");
        });

        VBox body = new VBox(8, title, store, price, buy);
        body.setStyle("-fx-padding: 12 14 0 14;");

        VBox card = new VBox(0, imagePane, body);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 0 0 16 0;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.08), 12, 0, 0, 4);");
        return card;
    }

    private StackPane createArticleImagePane(Article article) {
        StackPane imagePane = new StackPane();
        imagePane.setPrefWidth(230);
        imagePane.setPrefHeight(140);
        imagePane.setStyle("-fx-background-color: linear-gradient(to bottom right, #241197, #6c2a90);"
                + "-fx-background-radius: 12 12 0 0;");

        String imageUrl = ImageUtils.getAbsolutePath(article.getImage());
        if (imageUrl != null) {
            ImageView imageView = new ImageView(new Image(imageUrl, 230, 140, false, true, true));
            imageView.setFitWidth(230);
            imageView.setFitHeight(140);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imagePane.getChildren().add(imageView);
        } else {
            Label fallback = new Label("Art");
            fallback.setStyle("-fx-font-size: 26; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.9);");
            imagePane.getChildren().add(fallback);
        }

        return imagePane;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
