package tn.rouhfan.ui.back;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;

import java.io.File;

public class DetailsDialogController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ImageView imageView;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Label categoryLabel;
    @FXML private Label artistLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox extraInfoPane;

    public void setOeuvre(Oeuvre o) {
        titleLabel.setText("🖼️ Détails de l'œuvre");
        subtitleLabel.setText("Informations sur l'œuvre d'art");
        nameLabel.setText(o.getTitre());
        priceLabel.setText(o.getPrix() != null ? o.getPrix().toString() + " DT" : "0.00 DT");
        statusLabel.setText(o.getStatut());
        categoryLabel.setText(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Non classé");
        artistLabel.setText(o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "artiste artist");
        descriptionLabel.setText(o.getDescription());
        
        loadImage(o.getImage());
    }

    public void setCategorie(Categorie c) {
        titleLabel.setText("🏷️ Détails de la catégorie");
        subtitleLabel.setText("Organisation des œuvres");
        nameLabel.setText(c.getNomCategorie());
        
        // Cacher les infos spécifiques aux œuvres
        extraInfoPane.setVisible(false);
        extraInfoPane.setManaged(false);
        
        loadImage(c.getImageCategorie());
    }

    private void loadImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(imagePath);
            if (fullPath != null) {
                Image img = new Image(fullPath, true);
                imageView.setImage(img);
            }
        }
    }

    @FXML
    private void close() {
        ((Stage) nameLabel.getScene().getWindow()).close();
    }
}
