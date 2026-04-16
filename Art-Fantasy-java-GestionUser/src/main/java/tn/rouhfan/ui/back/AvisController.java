package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.services.ReclamationService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class AvisController implements Initializable {

    @FXML private TableView<Reclamation> avisTable;
    @FXML private TableColumn<Reclamation, Integer> colId;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colMessage;
    @FXML private TableColumn<Reclamation, String> colUtilisateur;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statutFilter;

    private ReclamationService reclamationService;
    private ObservableList<Reclamation> reclamationsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reclamationService = new ReclamationService();
        setupFilters();
        setupColumns();
        loadReclamations();
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList("Tous", "Réclamation", "Avis", "Suggestion"));
        typeFilter.setValue("Tous");
        statutFilter.setItems(FXCollections.observableArrayList("Tous", "En attente", "Traité", "Clôturé"));
        statutFilter.setValue("Tous");
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategorie()));
        colSujet.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSujet()));
        colMessage.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        colUtilisateur.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(cellData.getValue().getAuteurId())
        ));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCreatedAt() != null ? dateFormat.format(cellData.getValue().getCreatedAt()) : ""
        ));
        colStatut.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut()));
    }

    private void loadReclamations() {
        try {
            reclamationsList = FXCollections.observableArrayList(reclamationService.recuperer());
            avisTable.setItems(reclamationsList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les réclamations: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadReclamations();
    }

    @FXML
    private void repondre(ActionEvent event) {
        Reclamation selected = avisTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réclamation");
            return;
        }
        showAlert("Info", "Répondre à: " + selected.getSujet());
    }

    @FXML
    private void deleteAvis(ActionEvent event) {
        Reclamation selected = avisTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réclamation");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la réclamation");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getSujet() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    reclamationService.supprimer(selected.getId());
                    loadReclamations();
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
