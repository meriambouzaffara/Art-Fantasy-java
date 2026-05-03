package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReponseReclamationService;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReponseListController {

    @FXML private TableView<ReponseReclamation> table;
    @FXML private TableColumn<ReponseReclamation, Integer> colId;
    @FXML private TableColumn<ReponseReclamation, String> colMessage;
    @FXML private TableColumn<ReponseReclamation, String> colDate;

    private int reclamationId;

    private ReponseReclamationService service = new ReponseReclamationService();
    private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

    public void setReclamationId(int id) {
        this.reclamationId = id;
        load();
    }

    private void load() {
        try {
            List<ReponseReclamation> list = service.getByReclamation(reclamationId);

            colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()).asObject());
            colMessage.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getMessage()));
            colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                    format.format(c.getValue().getCreatedAt())
            ));

            table.setItems(FXCollections.observableArrayList(list));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimer() {
        ReponseReclamation selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        try {
            service.supprimer(selected.getId());
            load();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void modifier() {
        ReponseReclamation selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getMessage());
        dialog.setTitle("Modifier");
        dialog.setHeaderText("Modifier la réponse");

        dialog.showAndWait().ifPresent(newMsg -> {
            try {
                selected.setMessage(newMsg);
                service.modifier(selected);
                load();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}