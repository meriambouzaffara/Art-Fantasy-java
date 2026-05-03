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
import tn.rouhfan.entities.User;
import tn.rouhfan.services.FavorisService;
import tn.rouhfan.services.OeuvrePaymentService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.back.OeuvreFormController;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
    @FXML private Button buyBtn;
    @FXML private Button favBtn;
    @FXML private Label favIcon;

    private Oeuvre oeuvre;
    private String userRole;
    private Runnable refreshCallback;
    private OeuvreService oeuvreService = new OeuvreService();
    private FavorisService favorisService = new FavorisService();
    private OeuvrePaymentService paymentService = new OeuvrePaymentService();

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
            statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 11; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 11; -fx-font-weight: bold;");
        }

        // Image loading via ImageUtils
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                oeuvreImage.setImage(new Image(fullPath));
            }
        }

        // Déterminer les droits
        boolean isAdmin = role != null && role.toUpperCase().contains("ADMIN");
        boolean isArtiste = role != null && (role.toUpperCase().contains("ARTIST") || role.toUpperCase().contains("ARTISTE"));
        boolean isParticipant = role != null && role.toUpperCase().contains("PARTICIPANT");
        
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isProprietaire = isArtiste && currentUser != null && 
                                 o.getUser() != null && 
                                 o.getUser().getId() == currentUser.getId();
        
        // Boutons Modifier/Supprimer : Admin ou propriétaire artiste
        boolean canModify = isAdmin || isProprietaire;
        actionsPane.setVisible(canModify);
        actionsPane.setManaged(canModify);

        // Bouton Acheter : visible UNIQUEMENT pour les participants, si oeuvre disponible
        boolean isDisponible = "disponible".equalsIgnoreCase(o.getStatut());
        boolean canBuy = isParticipant && isDisponible;
        buyBtn.setVisible(canBuy);
        buyBtn.setManaged(canBuy);

        // Initialiser l'état du favori
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (favBtn != null) {
                favBtn.setVisible(false);
                favBtn.setManaged(false);
            }
            return;
        }
        try {
            if (favorisService.exists(currentUser.getId(), oeuvre.getId())) {
                favIcon.setText("⭐");
                favIcon.setStyle("-fx-text-fill: #fac62d; -fx-font-size: 22; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 0);");
            } else {
                favIcon.setText("☆");
                favIcon.setStyle("-fx-text-fill: #241197; -fx-font-size: 22; -fx-font-weight: bold;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleFavorite() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            favorisService.toggle(currentUser.getId(), oeuvre.getId());
            updateFavoriteIcon();
            // Optionnel : rafraîchir la liste si on est dans la vue favoris
            if (refreshCallback != null) refreshCallback.run();
        } catch (SQLException e) {
            e.printStackTrace();
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
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setHeaderText("Impossible d'ouvrir les détails");
            alert.setContentText("Erreur : " + e.getMessage() + "\nVérifiez la console pour plus de détails.");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBuy() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreStripePaymentDialog.fxml"));
            Parent root = loader.load();
            OeuvreStripePaymentDialogController controller = loader.getController();
            controller.setOeuvre(oeuvre);

            Stage paymentStage = new Stage();
            paymentStage.initModality(Modality.APPLICATION_MODAL);
            paymentStage.setTitle("Paiement Sécurisé");
            paymentStage.setScene(new Scene(root));
            paymentStage.showAndWait();

            if (controller.isSuccess()) {
                if (refreshCallback != null) refreshCallback.run();
            }
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible d'ouvrir le paiement");
            error.setContentText(e.getMessage());
            error.showAndWait();
            e.printStackTrace();
        }
    }

    private void finaliserAchat() throws SQLException {
        oeuvre.setStatut("vendue");
        oeuvre.setDateVente(new java.util.Date());
        oeuvreService.modifier(oeuvre);
        if (refreshCallback != null) refreshCallback.run();
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
