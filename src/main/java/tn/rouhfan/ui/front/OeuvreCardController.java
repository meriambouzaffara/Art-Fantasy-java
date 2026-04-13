package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.ui.back.OeuvreFormController;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class OeuvreCardController {

    @FXML private ImageView oeuvreImage;
    @FXML private Label titreLabel;
    @FXML private Label artisteLabel;
    @FXML private Label categorieLabel;
    @FXML private Label prixLabel;
    @FXML private Label statusLabel;
    @FXML private HBox actionsPane;
    @FXML private Button viewBtn;

    private Oeuvre oeuvre;
    private String userRole;
    private Runnable refreshCallback;
    private OeuvreService oeuvreService = new OeuvreService();

    public void setOeuvre(Oeuvre o, String role, Runnable callback) {
        this.oeuvre = o;
        this.userRole = role;
        this.refreshCallback = callback;
        
        titreLabel.setText(o.getTitre());
        artisteLabel.setText("Publié par: " + (o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "Inconnu"));
        categorieLabel.setText(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Non classé");
        prixLabel.setText(o.getPrix() != null ? o.getPrix().toString() + " DT" : "0 DT");
        statusLabel.setText(o.getStatut());
        
        // Status style
        if ("disponible".equalsIgnoreCase(o.getStatut())) {
            statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
        } else {
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;");
        }

        // Image loading
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            File file = new File(o.getImage());
            if (file.exists()) {
                Image img = new Image(file.toURI().toString());
                oeuvreImage.setImage(img);
            }
        }

        // Show actions only for ARTIST
        if ("ARTIST".equals(role)) {
            actionsPane.setVisible(true);
            actionsPane.setManaged(true);
        } else {
            actionsPane.setVisible(false);
            actionsPane.setManaged(false);
        }
    }

    @FXML
    private void handleView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreDetailsDialog.fxml"));
            Parent root = loader.load();
            OeuvreDetailsController controller = loader.getController();
            controller.setOeuvre(oeuvre);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle("Détails de l'œuvre");
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/OeuvreFormDialog.fxml"));
            Parent root = loader.load();
            OeuvreFormController controller = loader.getController();
            controller.setOeuvre(oeuvre);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier l'œuvre");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            if (controller.isSaved() && refreshCallback != null) refreshCallback.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous supprimer cette œuvre ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    oeuvreService.supprimer(oeuvre.getId());
                    if (refreshCallback != null) refreshCallback.run();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
