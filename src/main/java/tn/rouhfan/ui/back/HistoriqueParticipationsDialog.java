package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.EvenementService;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class HistoriqueParticipationsDialog {

    private Stage stage;
    private int userId;
    private EvenementService evenementService;

    public HistoriqueParticipationsDialog(int userId) {
        this.userId = userId;
        this.evenementService = new EvenementService();
        initUI();
    }

    private void initUI() {
        stage = new Stage();
        stage.setTitle("Mon Historique de Participations");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(700);
        stage.setHeight(500);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label titleLabel = new Label("🎫 Vos participations aux événements");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<Evenement> table = new TableView<>();
        table.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;");

        TableColumn<Evenement, String> colTitre = new TableColumn<>("Événement");
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setPrefWidth(200);

        TableColumn<Evenement, String> colType = new TableColumn<>("Catégorie");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(120);

        TableColumn<Evenement, String> colDate = new TableColumn<>("Date de l'événement");
        colDate.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDateEvent() != null ? sdf.format(cellData.getValue().getDateEvent()) : "N/A"
            );
        });
        colDate.setPrefWidth(150);

        TableColumn<Evenement, String> colLieu = new TableColumn<>("Lieu");
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colLieu.setPrefWidth(150);

        table.getColumns().addAll(colTitre, colType, colDate, colLieu);
        
        try {
            List<Evenement> participations = evenementService.getHistoriqueParticipations(userId);
            ObservableList<Evenement> data = FXCollections.observableArrayList(participations);
            table.setItems(data);
            
            if (participations.isEmpty()) {
                table.setPlaceholder(new Label("Aucune participation trouvée."));
            }
        } catch (SQLException e) {
            table.setPlaceholder(new Label("Erreur lors du chargement des données."));
            e.printStackTrace();
        }

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #6c2a90; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8;");
        closeBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(titleLabel, table, closeBtn);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }
}
