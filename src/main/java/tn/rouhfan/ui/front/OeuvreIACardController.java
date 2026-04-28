package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.services.OeuvreIAService;
import tn.rouhfan.tools.ImageUtils;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class OeuvreIACardController {

    @FXML private ImageView oeuvreImage;
    @FXML private Label titreLabel;
    @FXML private Label categorieLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label dateLabel;

    private OeuvreIA oeuvre;
    private Runnable onRefresh;
    private OeuvreIAService service = new OeuvreIAService();

    public void setOeuvre(OeuvreIA o, Runnable refreshCallback) {
        this.oeuvre = o;
        this.onRefresh = refreshCallback;

        titreLabel.setText(o.getTitre());
        categorieLabel.setText(o.getCategorie() != null ? "📁 " + o.getCategorie().getNomCategorie() : "Non catégorisé");
        descriptionLabel.setText(o.getDescription());
        
        if (o.getDateCreation() != null) {
            dateLabel.setText("Créé le " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(o.getDateCreation()));
        }

        // Chargement de l'image
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                oeuvreImage.setImage(new Image(fullPath));
            }
        }
    }

    @FXML
    private void handleView() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/front/IAOeuvreDetailsDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            IAOeuvreDetailsController controller = loader.getController();
            controller.setOeuvre(oeuvre);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Pour un look premium sans bordures Windows
            
            // Permettre le déplacement de la fenêtre car UNDECORATED
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cette création IA ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.supprimer(oeuvre.getId());
                    if (onRefresh != null) onRefresh.run();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void shareFacebook() {
        String aiImageUrl = getAIImageUrl();
        share("https://www.facebook.com/sharer/sharer.php?u=" + encode(aiImageUrl) + "&quote=" + 
              encode("Regardez ma nouvelle création IA : " + oeuvre.getTitre() + " sur Art Fantasy !"));
    }

    @FXML
    private void shareTwitter() {
        String aiImageUrl = getAIImageUrl();
        share("https://twitter.com/intent/tweet?text=" + 
              encode("Ma nouvelle création IA : " + oeuvre.getTitre() + " #ArtFantasy #AIArt") + 
              "&url=" + encode(aiImageUrl));
    }

    private String getAIImageUrl() {
        // Extraire le prompt de la description (format: "Prompt: ...")
        String prompt = oeuvre.getDescription();
        if (prompt != null && prompt.startsWith("Prompt: ")) {
            prompt = prompt.substring(8);
        } else {
            prompt = oeuvre.getTitre(); // Fallback
        }
        return "https://image.pollinations.ai/prompt/" + encode(prompt) + "?width=1024&height=1024&nologo=true";
    }

    private void share(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return text;
        }
    }
}
