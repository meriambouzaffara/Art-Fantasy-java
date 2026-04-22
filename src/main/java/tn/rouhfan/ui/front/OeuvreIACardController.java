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
    @FXML private Label dateLabel;

    private OeuvreIA oeuvre;
    private Runnable onRefresh;
    private OeuvreIAService service = new OeuvreIAService();

    public void setOeuvre(OeuvreIA o, Runnable refreshCallback) {
        this.oeuvre = o;
        this.onRefresh = refreshCallback;

        titreLabel.setText(o.getTitre());
        categorieLabel.setText(o.getCategorie() != null ? "📁 " + o.getCategorie().getNomCategorie() : "Non catégorisé");
        
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
