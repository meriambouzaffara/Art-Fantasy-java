package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.rouhfan.entities.Categorie;

import java.io.File;

public class CategoryCardController {

    @FXML private ImageView categoryImage;
    @FXML private Label nomLabel;
    @FXML private Label countLabel;

    private Runnable onClick;

    public void setCategory(Categorie c, Runnable onClick) {
        this.onClick = onClick;
        nomLabel.setText(c.getNomCategorie());
        countLabel.setText("Explorer les œuvres"); // TODO: actual count

        if (c.getImageCategorie() != null && !c.getImageCategorie().isEmpty()) {
            String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(c.getImageCategorie());
            if (fullPath != null) {
                categoryImage.setImage(new Image(fullPath));
            }
        }
    }

    private Image imgFromPath(File file) {
        return new Image(file.toURI().toString());
    }

    @FXML
    private void handleClick() {
        if (onClick != null) onClick.run();
    }
}
