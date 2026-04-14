package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.services.SponsorService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class SponsorsController implements Initializable {

    @FXML private TableView<Sponsor> sponsorTable;
    @FXML private TableColumn<Sponsor, Integer> colId;
    @FXML private TableColumn<Sponsor, String> colNom;
    @FXML private TableColumn<Sponsor, String> colEmail;
    @FXML private TableColumn<Sponsor, String> colTelephone;
    @FXML private TableColumn<Sponsor, String> colAdresse;
    @FXML private TableColumn<Sponsor, String> colDate;
    @FXML private TextField searchField;

    private SponsorService sponsorService;
    private ObservableList<Sponsor> sponsorsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sponsorService = new SponsorService();
        setupColumns();
        loadSponsors();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNom()));
        colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        colTelephone.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTel()));
        colAdresse.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAdresse()));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCreatedAt() != null ? dateFormat.format(cellData.getValue().getCreatedAt()) : ""
        ));
    }

    private void loadSponsors() {
        try {
            sponsorsList = FXCollections.observableArrayList(sponsorService.recuperer());
            sponsorTable.setItems(sponsorsList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les sponsors: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadSponsors();
    }

    @FXML
    private void addSponsor(ActionEvent event) {
        showAlert("Info", "Ajout de sponsor - À implémenter");
    }

    @FXML
    private void editSponsor(ActionEvent event) {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un sponsor");
            return;
        }
        showAlert("Info", "Modification de: " + selected.getNom());
    }

    @FXML
    private void deleteSponsor(ActionEvent event) {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un sponsor");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le sponsor");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sponsorService.supprimer(selected.getId());
                    loadSponsors();
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
