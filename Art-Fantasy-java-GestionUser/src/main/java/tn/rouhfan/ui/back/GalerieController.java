package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.OeuvreService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class GalerieController implements Initializable {

    @FXML private TableView<Oeuvre> oeuvreTable;
    @FXML private TableColumn<Oeuvre, Integer> colId;
    @FXML private TableColumn<Oeuvre, String> colTitre;
    @FXML private TableColumn<Oeuvre, String> colDescription;
    @FXML private TableColumn<Oeuvre, String> colPrix;
    @FXML private TableColumn<Oeuvre, String> colCategorie;
    @FXML private TableColumn<Oeuvre, String> colArtiste;
    @FXML private TableColumn<Oeuvre, String> colStatus;
    @FXML private TextField searchField;

    private OeuvreService oeuvreService;
    private ObservableList<Oeuvre> oeuvresList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        setupColumns();
        loadOeuvres();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colTitre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitre()));
        colDescription.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        colPrix.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrix().toString()));
        colCategorie.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCategorie() != null ? cellData.getValue().getCategorie().getNomCategorie() : ""
        ));
        colArtiste.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getUser() != null ? cellData.getValue().getUser().getNom() : ""
        ));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getStatut()
        ));
    }

    private void loadOeuvres() {
        try {
            oeuvresList = FXCollections.observableArrayList(oeuvreService.recuperer());
            oeuvreTable.setItems(oeuvresList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les œuvres: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadOeuvres();
    }

    @FXML
    private void addOeuvre(ActionEvent event) {
        openOeuvreDialog(null);
    }

    @FXML
    private void editOeuvre(ActionEvent event) {
        Oeuvre selected = oeuvreTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une œuvre");
            return;
        }
        openOeuvreDialog(selected);
    }

    private void openOeuvreDialog(Oeuvre oeuvre) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/back/OeuvreFormDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            OeuvreFormController controller = loader.getController();
            if (oeuvre != null) {
                controller.setOeuvre(oeuvre);
            }

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle(oeuvre == null ? "Ajouter une œuvre" : "Modifier l'œuvre");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(oeuvreTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadOeuvres();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteOeuvre(ActionEvent event) {
        Oeuvre selected = oeuvreTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une œuvre");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'œuvre");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getTitre() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    oeuvreService.supprimer(selected.getId());
                    loadOeuvres();
                } catch (SQLException e) {
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
