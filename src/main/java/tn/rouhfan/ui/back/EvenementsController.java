package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.EvenementService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class EvenementsController implements Initializable {

    @FXML private TableView<Evenement> evenementTable;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colDate;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, Integer> colCapacite;
    @FXML private TableColumn<Evenement, String> colStatut;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private EvenementService evenementService;
    private ObservableList<Evenement> evenementsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evenementService = new EvenementService();
        setupColumns();
        setupSortCombo();
        setupSearch();
        loadEvenements();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colTitre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitre()));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getDateEvent() != null ? dateFormat.format(cellData.getValue().getDateEvent()) : ""
        ));
        colLieu.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLieu()));
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType() != null ? cellData.getValue().getType() : ""));
        colCapacite.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCapacite() != null ? cellData.getValue().getCapacite() : 0).asObject());
        colStatut.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut() != null ? cellData.getValue().getStatut() : ""));
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Titre (A-Z)", "Titre (Z-A)", "Date Croissante", "Date Décroissante", "Lieu (A-Z)", "Capacité");
            sortCombo.setValue("Titre (A-Z)");
            sortCombo.setOnAction(e -> handleSort());
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        }
    }

    private void loadEvenements() {
        try {
            evenementsList = FXCollections.observableArrayList(evenementService.recuperer());
            evenementTable.setItems(evenementsList);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger les événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSearch() {
        try {
            String keyword = searchField.getText();
            ObservableList<Evenement> results = FXCollections.observableArrayList(
                evenementService.rechercher(keyword)
            );
            evenementTable.setItems(results);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la recherche: " + e.getMessage());
        }
    }

    private void handleSort() {
        try {
            String sortOption = sortCombo.getValue();
            String keyword = searchField.getText();

            ObservableList<Evenement> results = null;

            switch (sortOption) {
                case "Titre (A-Z)":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "titre", true)
                    );
                    break;
                case "Titre (Z-A)":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "titre", false)
                    );
                    break;
                case "Date Croissante":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "date", true)
                    );
                    break;
                case "Date Décroissante":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "date", false)
                    );
                    break;
                case "Lieu (A-Z)":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "lieu", true)
                    );
                    break;
                case "Capacité":
                    results = FXCollections.observableArrayList(
                        evenementService.rechercherEtTrier(keyword, "capacite", true)
                    );
                    break;
            }

            if (results != null) {
                evenementTable.setItems(results);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors du tri: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        searchField.clear();
        sortCombo.setValue("Titre (A-Z)");
        loadEvenements();
    }

    @FXML
    private void addEvenement(ActionEvent event) {
        EvenementFormDialog dialog = new EvenementFormDialog(null);
        dialog.show();
        
        if (dialog.isApproved()) {
            loadEvenements();
            showAlert("Succès", "✅ Événement ajouté avec succès!");
        }
    }

    @FXML
    private void editEvenement(ActionEvent event) {
        Evenement selected = evenementTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "⚠️ Veuillez sélectionner un événement à modifier");
            return;
        }

        try {
            Evenement fullEvent = evenementService.findById(selected.getId());
            EvenementFormDialog dialog = new EvenementFormDialog(fullEvent);
            dialog.show();
            
            if (dialog.isApproved()) {
                loadEvenements();
                showAlert("Succès", "✅ Événement modifié avec succès!");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void deleteEvenement(ActionEvent event) {
        Evenement selected = evenementTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "⚠️ Veuillez sélectionner un événement à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'événement");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: \"" + selected.getTitre() + "\" ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    evenementService.supprimer(selected.getId());
                    loadEvenements();
                    showAlert("Succès", "✅ Événement supprimé avec succès!");
                } catch (SQLException e) {
                    showAlert("Erreur", "❌ Impossible de supprimer: " + e.getMessage());
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
