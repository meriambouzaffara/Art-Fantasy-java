package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Article;
import tn.rouhfan.services.ArticleService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MagasinController implements Initializable {

    @FXML private TableView<Article> articleTable;
    @FXML private TableColumn<Article, Integer> colId;
    @FXML private TableColumn<Article, String> colNom;
    @FXML private TableColumn<Article, String> colPrix;
    @FXML private TableColumn<Article, Integer> colQuantite;
    @FXML private TableColumn<Article, String> colDescription;
    @FXML private TableColumn<Article, String> colMagasin;
    @FXML private TextField searchField;

    private ArticleService articleService;
    private ObservableList<Article> articlesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        articleService = new ArticleService();
        setupColumns();
        loadArticles();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getIdArticle().intValue()).asObject());
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitre()));
        colPrix.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(cellData.getValue().getPrix())
        ));
        colQuantite.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getStock()).asObject());
        colDescription.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        colMagasin.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getMagasin() != null ? cellData.getValue().getMagasin().getNom() : ""
        ));
    }

    private void loadArticles() {
        try {
            articlesList = FXCollections.observableArrayList(articleService.recuperer());
            articleTable.setItems(articlesList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les articles: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadArticles();
    }

    @FXML
    private void addArticle(ActionEvent event) {
        showAlert("Info", "Ajout d'article - À implémenter");
    }

    @FXML
    private void editArticle(ActionEvent event) {
        Article selected = articleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un article");
            return;
        }
        showAlert("Info", "Modification de: " + selected.getTitre());
    }

    @FXML
    private void deleteArticle(ActionEvent event) {
        Article selected = articleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner un article");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'article");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getTitre() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    articleService.supprimer(selected.getIdArticle().intValue());
                    loadArticles();
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
