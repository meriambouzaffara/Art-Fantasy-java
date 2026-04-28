package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Notification;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.NotificationService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.tools.SessionManager;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsFrontController implements Initializable {

    @FXML private VBox notificationsBox;
    @FXML private VBox emptyState;

    private NotificationService service = new NotificationService();
    private OeuvreService oeuvreService = new OeuvreService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        notificationsBox.getChildren().clear();
        
        List<Notification> list = service.recupererParUser(currentUser.getId());
        
        if (list.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            notificationsBox.setVisible(false);
            notificationsBox.setManaged(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            notificationsBox.setVisible(true);
            notificationsBox.setManaged(true);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            for (Notification n : list) {
                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-padding: 15; -fx-background-radius: 10; -fx-background-color: " + (n.isLu() ? "#f8fafc" : "#e0e7ff") + "; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");

                // Utilisation de caractères unicode simples pour éviter les carrés (Variation Selectors)
                String iconChar = n.isLu() ? "\u2709" : "\uD83D\uDD14"; 
                Label icon = new Label(iconChar);
                icon.setStyle("-fx-font-size: 22; -fx-text-fill: " + (n.isLu() ? "#cbd5e1" : "#fac62d") + ";");

                VBox textBox = new VBox(5);
                Label message = new Label(n.getMessage());
                message.setStyle("-fx-font-size: 14; -fx-text-fill: #1e293b; " + (n.isLu() ? "" : "-fx-font-weight: bold;"));
                message.setWrapText(true);

                Label date = new Label(sdf.format(n.getDateCreation()));
                date.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");

                textBox.getChildren().addAll(message, date);
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Button viewBtn = new Button("Voir l'œuvre");
                viewBtn.setStyle("-fx-background-color: #241197; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 6 15; -fx-font-weight: bold; -fx-cursor: hand;");
                viewBtn.setOnAction(e -> openOeuvreDetails(n.getOeuvreId()));

                Button deleteBtn = new Button("🗑️");
                deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 20; -fx-padding: 6 12; -fx-font-weight: bold; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> handleSupprimer(n.getId()));

                card.getChildren().addAll(icon, textBox, spacer, viewBtn, deleteBtn);

                notificationsBox.getChildren().add(card);
            }

            // Mark all as read when opening
            service.markAllAsRead(currentUser.getId());
        }
    }

    private void handleSupprimer(int id) {
        service.supprimer(id);
        loadData(); // Refresh list
    }

    private void openOeuvreDetails(int oeuvreId) {
        if (oeuvreId <= 0) return;
        try {
            Oeuvre o = oeuvreService.findById(oeuvreId);
            if (o == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreDetailsDialog.fxml"));
            Parent root = loader.load();
            OeuvreDetailsController controller = loader.getController();
            controller.setOeuvre(o);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle("Détails de l'œuvre");
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
