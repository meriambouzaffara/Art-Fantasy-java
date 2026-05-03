package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.tools.ImageUtils;

public class IAOeuvreDetailsController {

    @FXML private ImageView oeuvreImage;
    @FXML private Label titreLabel;
    @FXML private Label categorieLabel;
    @FXML private Text promptText;

    private OeuvreIA oeuvre;

    public void setOeuvre(OeuvreIA o) {
        this.oeuvre = o;
        
        titreLabel.setText(o.getTitre());
        categorieLabel.setText(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Art IA");
        
        // Clean prompt display
        String prompt = o.getDescription();
        if (prompt != null && prompt.startsWith("Prompt: ")) {
            prompt = prompt.substring(8);
        }
        promptText.setText(prompt);

        // Image loading
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                oeuvreImage.setImage(new Image(fullPath));
            }
        }
    }

    @FXML
    private void shareFacebook() {
        String prompt = oeuvre.getDescription();
        if (prompt != null && prompt.startsWith("Prompt: ")) prompt = prompt.substring(8);
        String aiImageUrl = "https://image.pollinations.ai/prompt/" + encode(prompt) + "?width=1024&height=1024&nologo=true";
        String shareUrl = "https://www.facebook.com/sharer/sharer.php?u=" + encode(aiImageUrl) + "&quote=" + 
                          encode("Regardez ma création IA : " + oeuvre.getTitre());
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(shareUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void close() {
        ((Stage) titreLabel.getScene().getWindow()).close();
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }
}
