package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.rouhfan.entities.Oeuvre;

import java.io.File;

public class OeuvreDetailsController {

    @FXML private ImageView oeuvreImage;
    @FXML private Label statusBadge;
    @FXML private Label categorieLabel;
    @FXML private Label titreLabel;
    @FXML private Label artisteLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label prixLabel;

    private Oeuvre oeuvre;

    public void setOeuvre(Oeuvre o) {
        this.oeuvre = o;
        
        titreLabel.setText(o.getTitre());
        descriptionLabel.setText(o.getDescription());
        categorieLabel.setText(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Non classé");
        artisteLabel.setText(o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "Artiste inconnu");
        prixLabel.setText(o.getPrix() != null ? o.getPrix().toString() + " DT" : "0 DT");
        statusBadge.setText(o.getStatut());

        // Status style
        if ("disponible".equalsIgnoreCase(o.getStatut())) {
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        }

        // Image loading via ImageUtils
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                oeuvreImage.setImage(new Image(fullPath));
            }
        }
    }

    @FXML
    private void close() {
        ((Stage) titreLabel.getScene().getWindow()).close();
    }
}
