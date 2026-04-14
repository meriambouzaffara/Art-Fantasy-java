package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.services.CategorieService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CategoriesController implements Initializable {

    @FXML private TableView<Categorie> categorieTable;
    @FXML private TableColumn<Categorie, Integer> colId;
    @FXML private TableColumn<Categorie, String> colNom;
    @FXML private TableColumn<Categorie, String> colImage;
    @FXML private TableColumn<Categorie, Integer> colNombre;
    @FXML private TextField searchField;

    private CategorieService categorieService;
    private ObservableList<Categorie> categoriesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categorieService = new CategorieService();
        setupColumns();
        loadCategories();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getIdCategorie()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNomCategorie()));
        colImage.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getImageCategorie()));
        colNombre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(0).asObject());
    }

    private void loadCategories() {
        try {
            categoriesList = FXCollections.observableArrayList(categorieService.recuperer());
            categorieTable.setItems(categoriesList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les catégories: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadCategories();
    }

    @FXML
    private void addCategorie(ActionEvent event) {
        showAlert("Info", "Ajout de catégorie - À implémenter");
    }

    @FXML
    private void editCategorie(ActionEvent event) {
        Categorie selected = categorieTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une catégorie");
            return;
        }
        showAlert("Info", "Modification de: " + selected.getNomCategorie());
    }

    @FXML
    private void deleteCategorie(ActionEvent event) {
        Categorie selected = categorieTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une catégorie");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la catégorie");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getNomCategorie() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    categorieService.supprimer(selected.getIdCategorie());
                    loadCategories();
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
