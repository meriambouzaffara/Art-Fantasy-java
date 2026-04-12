package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.services.CoursService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CoursController implements Initializable {

    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, Integer> colId;
    @FXML private TableColumn<Cours, String> colNom;
    @FXML private TableColumn<Cours, String> colDescription;
    @FXML private TableColumn<Cours, String> colNiveau;
    @FXML private TableColumn<Cours, String> colDuree;
    @FXML private TableColumn<Cours, String> colStatut;
    @FXML private TableColumn<Cours, String> colArtiste;
    @FXML private TextField searchField;

    private CoursService coursService;
    private ObservableList<Cours> coursList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        coursService = new CoursService();
        setupColumns();
        loadCours();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNom()));
        colDescription.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        colNiveau.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNiveau()));
        colDuree.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDuree()));
        colStatut.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut()));
        colArtiste.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getArtiste() != null ? cellData.getValue().getArtiste().getNom() : ""
        ));
    }

    private void loadCours() {
        try {
            coursList = FXCollections.observableArrayList(coursService.recuperer());
            coursTable.setItems(coursList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les cours: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadCours();
    }

    @FXML
    private void addCours(ActionEvent event) {
        showAlert("Info", "Ajout de cours - À implémenter");
    }

    @FXML
    private void editCours(ActionEvent event) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un cours");
            return;
        }
        showAlert("Info", "Modification de: " + selected.getNom());
    }

    @FXML
    private void deleteCours(ActionEvent event) {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un cours");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le cours");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    coursService.supprimer(selected.getId());
                    loadCours();
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
