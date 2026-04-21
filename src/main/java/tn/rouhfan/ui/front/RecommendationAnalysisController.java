package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.OeuvreRecommendationService;
import tn.rouhfan.tools.ImageUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecommendationAnalysisController {

    @FXML private Label profileAnalysisLabel;
    @FXML private VBox analysisContainer;
    @FXML private ScrollPane analysisScrollPane;

    private User currentUser;
    private List<Oeuvre> recommendations;
    private OeuvreRecommendationService recommendationService;

    public void initData(User user, List<Oeuvre> recommendations, OeuvreRecommendationService service) {
        this.currentUser = user;
        this.recommendations = recommendations;
        this.recommendationService = service;

        loadAnalysis();
    }

    private void loadAnalysis() {
        new Thread(() -> {
            JSONObject analysis = recommendationService.getAnalysis(currentUser, recommendations);
            
            Platform.runLater(() -> {
                if (analysis.has("error")) {
                    profileAnalysisLabel.setText("Recommandations IA indisponibles (" + analysis.getString("error") + "). " +
                            "Découvrez ces œuvres sélectionnées pour vous.");
                    renderFallbackAnalysis();
                } else {
                    profileAnalysisLabel.setText(analysis.optString("profil_analysis", "Analyse de profil indisponible."));
                    renderAiAnalysis(analysis.optJSONArray("oeuvres"));
                }
            });
        }).start();
    }

    private void renderAiAnalysis(JSONArray oeuvresAnalysis) {
        analysisContainer.getChildren().clear();
        if (oeuvresAnalysis == null) return;

        Map<Integer, Oeuvre> recMap = recommendations.stream()
                .collect(Collectors.toMap(Oeuvre::getId, Function.identity()));

        for (int i = 0; i < oeuvresAnalysis.length(); i++) {
            JSONObject item = oeuvresAnalysis.getJSONObject(i);
            int id = item.optInt("id");
            Oeuvre o = recMap.get(id);
            
            if (o != null) {
                analysisContainer.getChildren().add(createAnalysisCard(o, item.optString("vision"), item.optString("pourquoi")));
            }
        }
    }

    private void renderFallbackAnalysis() {
        analysisContainer.getChildren().clear();
        for (Oeuvre o : recommendations) {
            analysisContainer.getChildren().add(createAnalysisCard(o, 
                "Analyse visuelle indisponible pour le moment.", 
                "Une œuvre populaire qui pourrait vous plaire."));
        }
    }

    private VBox createAnalysisCard(Oeuvre o, String vision, String pourquoi) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        HBox header = new HBox(20);
        header.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Image Section
        ImageView imgView = new ImageView();
        imgView.setFitWidth(140);
        imgView.setFitHeight(140);
        imgView.setPreserveRatio(true);
        imgView.setStyle("-fx-background-radius: 10;");
        
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                try {
                    imgView.setImage(new Image(fullPath));
                } catch (Exception e) {
                    System.err.println("Error loading image for analysis: " + e.getMessage());
                }
            }
        }

        // Details Section
        VBox details = new VBox(12);
        details.setPrefWidth(480);

        Label title = new Label(o.getTitre());
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #241197;");

        VBox visionBox = new VBox(5);
        Label visionTitle = new Label("👁️ VISION DE L'IA (ANALYSE VISUELLE)");
        visionTitle.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-letter-spacing: 1;");
        Label visionDesc = new Label(vision);
        visionDesc.setWrapText(true);
        visionDesc.setStyle("-fx-font-size: 13; -fx-text-fill: #1e293b;");
        visionBox.getChildren().addAll(visionTitle, visionDesc);

        VBox whyBox = new VBox(5);
        Label whyTitle = new Label("🤝 POURQUOI POUR VOUS ?");
        whyTitle.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-letter-spacing: 1;");
        Label whyDesc = new Label(pourquoi);
        whyDesc.setWrapText(true);
        whyDesc.setStyle("-fx-background-color: #f5f3ff; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 13; -fx-text-fill: #5b21b6;");
        whyBox.getChildren().addAll(whyTitle, whyDesc);

        details.getChildren().addAll(title, visionBox, whyBox);
        header.getChildren().addAll(imgView, details);
        card.getChildren().add(header);

        return card;
    }

    @FXML
    private void handleClose() {
        ((Stage) analysisContainer.getScene().getWindow()).close();
    }
}
