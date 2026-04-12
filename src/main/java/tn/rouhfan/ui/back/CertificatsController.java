package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Certificat;
import tn.rouhfan.services.CertificatService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class CertificatsController implements Initializable {

    @FXML private TableView<Certificat> certificatTable;
    @FXML private TableColumn<Certificat, Integer> colId;
    @FXML private TableColumn<Certificat, String> colNom;
    @FXML private TableColumn<Certificat, String> colNiveau;
    @FXML private TableColumn<Certificat, String> colScore;
    @FXML private TableColumn<Certificat, String> colDate;
    @FXML private TableColumn<Certificat, String> colParticipant;
    @FXML private TableColumn<Certificat, String> colCours;
    @FXML private TextField searchField;

    private CertificatService certificatService;
    private ObservableList<Certificat> certificatsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        certificatService = new CertificatService();
        setupColumns();
        loadCertificats();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNom()));
        colNiveau.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNiveau()));
        colScore.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getScore() != null ? cellData.getValue().getScore().toString() : ""
        ));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getDateObtention() != null ? dateFormat.format(cellData.getValue().getDateObtention()) : ""
        ));
        colParticipant.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getParticipant() != null ? cellData.getValue().getParticipant().getNom() : ""
        ));
        colCours.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCours() != null ? cellData.getValue().getCours().getNom() : ""
        ));
    }

    private void loadCertificats() {
        try {
            certificatsList = FXCollections.observableArrayList(certificatService.recuperer());
            certificatTable.setItems(certificatsList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les certificats: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadCertificats();
    }

    @FXML
    private void addCertificat(ActionEvent event) {
        showAlert("Info", "Ajout de certificat - À implémenter");
    }

    @FXML
    private void editCertificat(ActionEvent event) {
        Certificat selected = certificatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un certificat");
            return;
        }
        showAlert("Info", "Modification de: " + selected.getNom());
    }

    @FXML
    private void deleteCertificat(ActionEvent event) {
        Certificat selected = certificatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un certificat");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le certificat");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    certificatService.supprimer(selected.getId());
                    loadCertificats();
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
