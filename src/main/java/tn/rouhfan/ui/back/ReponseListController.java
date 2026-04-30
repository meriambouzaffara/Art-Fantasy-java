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

    @FXML private ListView<ReponseReclamation> listReponses;

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

            listReponses.setItems(FXCollections.observableArrayList(list));

            listReponses.setCellFactory(param -> new ListCell<ReponseReclamation>() {
                @Override
                protected void updateItem(ReponseReclamation item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8);
                        box.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 15px;");

                        Label dateLbl = new Label("Ajouté le " + format.format(item.getCreatedAt()));
                        dateLbl.setStyle("-fx-text-fill: #777777; -fx-font-size: 12px;");

                        Label msgLbl = new Label(item.getMessage());
                        msgLbl.setWrapText(true);
                        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-line-spacing: 5px;");

                        box.getChildren().addAll(dateLbl, msgLbl);

                        setGraphic(box);
                        setStyle("-fx-background-color: transparent; -fx-padding: 5px 0px;");
                    }
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimer() {
        ReponseReclamation selected = listReponses.getSelectionModel().getSelectedItem();

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
        ReponseReclamation selected = listReponses.getSelectionModel().getSelectedItem();

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