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
    @FXML private ComboBox<String> sortCombo;

    @FXML private Label statTotalSponsors;
    @FXML private Label statRecentSponsors;

    private SponsorService sponsorService;
    private ObservableList<Sponsor> sponsorsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sponsorService = new SponsorService();
        setupColumns();
        setupSortCombo();
        setupSearch();
        loadSponsors();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNom()));
        colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail() != null ? cellData.getValue().getEmail() : ""));
        colTelephone.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTel() != null ? cellData.getValue().getTel() : ""));
        colAdresse.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAdresse() != null ? cellData.getValue().getAdresse() : ""));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getCreatedAt() != null ? dateFormat.format(cellData.getValue().getCreatedAt()) : ""
        ));
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Nom (A-Z)", "Nom (Z-A)", "Email (A-Z)", "Date Récente", "Date Ancienne");
            sortCombo.setValue("Nom (A-Z)");
            sortCombo.setOnAction(e -> handleSort());
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        }
    }

    private void loadSponsors() {
        try {
            sponsorsList = FXCollections.observableArrayList(sponsorService.recuperer());
            sponsorTable.setItems(sponsorsList);
            updateStats(sponsorsList);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger les sponsors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats(ObservableList<Sponsor> list) {
        if (statTotalSponsors == null) return;
        statTotalSponsors.setText(String.valueOf(list.size()));
        
        long now = System.currentTimeMillis();
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        int recent = 0;
        for (Sponsor s : list) {
            if (s.getCreatedAt() != null) {
                if (now - s.getCreatedAt().getTime() <= thirtyDays) {
                    recent++;
                }
            }
        }
        statRecentSponsors.setText(String.valueOf(recent));
    }

    private void handleSearch() {
        try {
            String keyword = searchField.getText();
            ObservableList<Sponsor> results = FXCollections.observableArrayList(
                sponsorService.rechercher(keyword)
            );
            sponsorTable.setItems(results);
            updateStats(results);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la recherche: " + e.getMessage());
        }
    }

    private void handleSort() {
        try {
            String sortOption = sortCombo.getValue();
            String keyword = searchField.getText();

            ObservableList<Sponsor> results = null;

            switch (sortOption) {
                case "Nom (A-Z)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "nom", true)
                    );
                    break;
                case "Nom (Z-A)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "nom", false)
                    );
                    break;
                case "Email (A-Z)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "email", true)
                    );
                    break;
                case "Date Récente":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "date", false)
                    );
                    break;
                case "Date Ancienne":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "date", true)
                    );
                    break;
            }

            if (results != null) {
                sponsorTable.setItems(results);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors du tri: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        searchField.clear();
        sortCombo.setValue("Nom (A-Z)");
        loadSponsors();
    }

    @FXML
    private void addSponsor(ActionEvent event) {
        SponsorFormDialog dialog = new SponsorFormDialog(null);
        dialog.show();
        
        if (dialog.isApproved()) {
            loadSponsors();
            showAlert("Succès", "✅ Sponsor ajouté avec succès!");
        }
    }

    @FXML
    private void editSponsor(ActionEvent event) {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "⚠️ Veuillez sélectionner un sponsor à modifier");
            return;
        }

        try {
            Sponsor fullSponsor = sponsorService.findById(selected.getId());
            SponsorFormDialog dialog = new SponsorFormDialog(fullSponsor);
            dialog.show();
            
            if (dialog.isApproved()) {
                loadSponsors();
                showAlert("Succès", "✅ Sponsor modifié avec succès!");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger le sponsor: " + e.getMessage());
        }
    }

    @FXML
    private void deleteSponsor(ActionEvent event) {
        Sponsor selected = sponsorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "⚠️ Veuillez sélectionner un sponsor à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer le sponsor");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: \"" + selected.getNom() + "\" ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sponsorService.supprimer(selected.getId());
                    loadSponsors();
                    showAlert("Succès", "✅ Sponsor supprimé avec succès!");
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
