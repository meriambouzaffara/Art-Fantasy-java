package tn.rouhfan.ui.shared;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PlaceholderPageController {

    @FXML
    private Label title;

    @FXML
    private Label subtitle;

    @FXML
    public void initialize() {
        title.setText("Bientôt disponible");
        subtitle.setText("Ce template est prêt. Il ne reste plus qu'à brancher les formulaires et les tables (CRUD) sur vos services.");
    }
}
