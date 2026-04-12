package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UtilisateursController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRoles;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, String> colType;
    @FXML private TextField searchField;

    private UserService userService;
    private ObservableList<User> usersList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        setupColumns();
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
    }

    private void loadUsers() {
        try {
            usersList = FXCollections.observableArrayList(userService.recuperer());
            userTable.setItems(usersList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadUsers();
    }

    @FXML
    private void addUser(ActionEvent event) {
        // Logique pour ajouter un utilisateur (par exemple, ouvrir un dialogue)
        showAlert("Information", "Fonctionnalité d'ajout bientôt disponible.");
    }

    @FXML
    private void editUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un utilisateur");
            return;
        }
        // Logique pour modifier l'utilisateur sélectionné
        showAlert("Information", "Fonctionnalité de modification bientôt disponible.");
    }

    @FXML
    private void deleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un utilisateur");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getNom() + " " + selected.getPrenom() + " ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.supprimer(selected.getId());
                    loadUsers();
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
