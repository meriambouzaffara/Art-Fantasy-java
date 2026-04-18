package tn.rouhfan.ui.back;



import javafx.collections.FXCollections;

import javafx.collections.ObservableList;

import javafx.collections.transformation.FilteredList;

import javafx.collections.transformation.SortedList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;

import javafx.fxml.Initializable;

import javafx.scene.Parent;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;

import javafx.scene.image.ImageView;

import javafx.scene.layout.HBox;

import javafx.stage.Modality;

import javafx.stage.Stage;

import tn.rouhfan.entities.Categorie;

import tn.rouhfan.services.CategorieService;



import java.io.File;

import java.net.URL;

import java.sql.SQLException;

import java.util.ResourceBundle;



public class CategoriesController implements Initializable {



    @FXML private TableView<Categorie> categorieTable;

    @FXML private TableColumn<Categorie, String> colNom;

    @FXML private TableColumn<Categorie, String> colImage;

    @FXML private TableColumn<Categorie, Void> colActions;

    @FXML private TextField searchField;



    private CategorieService categorieService;

    private ObservableList<Categorie> categoriesList;



    @Override

    public void initialize(URL location, ResourceBundle resources) {

        categorieService = new CategorieService();

        setupColumns();

        loadCategories();

        setupSearch();

    }



    private void setupColumns() {
        colNom.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNomCategorie()));
        colNom.setStyle("-fx-alignment: CENTER;");



        // Image Column

        colImage.setCellFactory(param -> new TableCell<Categorie, String>() {

            private final ImageView imageView = new ImageView();

            {

                imageView.setFitHeight(50);

                imageView.setFitWidth(80);

                imageView.setPreserveRatio(true);

                imageView.getStyleClass().add("table-image-view");

            }

            @Override

            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {

                    setGraphic(null);

                } else {

                    Categorie c = getTableRow().getItem();

                    String imagePath = c.getImageCategorie();

                    if (imagePath != null && !imagePath.isEmpty()) {

                        File file = new File(imagePath);

                        if (file.exists()) {

                            Image img = new Image(file.toURI().toString(), true);

                            imageView.setImage(img);

                            setGraphic(imageView);

                        } else {

                            setGraphic(new Label("N/A"));

                        }

                    } else {

                        setGraphic(new Label("N/A"));

                    }

                }

            }

        });



        // Actions Column

        colActions.setCellFactory(param -> new TableCell<Categorie, Void>() {

            private final Button viewBtn = new Button("👁️ Voir");

            private final Button editBtn = new Button("📝 Edit");

            private final Button deleteBtn = new Button("🗑️ Supp");

            private final HBox pane = new HBox(10, viewBtn, editBtn, deleteBtn);



            {

                viewBtn.getStyleClass().add("btn-view-reflet");

                editBtn.getStyleClass().add("btn-edit-reflet");

                deleteBtn.getStyleClass().add("btn-delete-reflet");

                pane.setAlignment(javafx.geometry.Pos.CENTER);

                

                viewBtn.setMinWidth(Button.USE_PREF_SIZE);

                editBtn.setMinWidth(Button.USE_PREF_SIZE);

                deleteBtn.setMinWidth(Button.USE_PREF_SIZE);

                

                viewBtn.setOnAction(e -> {

                    Categorie c = getTableView().getItems().get(getIndex());

                    openCategorieDetails(c);

                });

                

                editBtn.setOnAction(e -> {

                    Categorie c = getTableView().getItems().get(getIndex());

                    openCategorieDialog(c);

                });

                

                deleteBtn.setOnAction(e -> {

                    Categorie c = getTableView().getItems().get(getIndex());

                    handleDelete(c);

                });

            }



            @Override

            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                setGraphic(empty ? null : pane);

            }

        });

    }



    private void loadCategories() {

        try {

            categoriesList = FXCollections.observableArrayList(categorieService.recuperer());

            applyFilters();

        } catch (SQLException e) {

            showAlert("Erreur", "Impossible de charger les catégories: " + e.getMessage());

        }

    }



    private void setupSearch() {

        searchField.textProperty().addListener((obs, old, newValue) -> applyFilters());

    }



    private void applyFilters() {

        if (categoriesList == null) return;



        FilteredList<Categorie> filteredData = new FilteredList<>(categoriesList, p -> {

            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

            return p.getNomCategorie().toLowerCase().contains(searchText);

        });



        SortedList<Categorie> sortedData = new SortedList<>(filteredData);

        sortedData.comparatorProperty().bind(categorieTable.comparatorProperty());

        categorieTable.setItems(sortedData);

    }



    @FXML

    private void refresh(ActionEvent event) {

        loadCategories();

    }



    @FXML

    private void addCategorie(ActionEvent event) {

        openCategorieDialog(null);

    }



    private void openCategorieDetails(Categorie categorie) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/DetailsDialog.fxml"));

            Parent root = loader.load();



            DetailsDialogController controller = loader.getController();

            controller.setCategorie(categorie);



            Stage dialogStage = new Stage();

            dialogStage.setTitle("Détails de la catégorie");

            dialogStage.initModality(Modality.WINDOW_MODAL);

            dialogStage.initOwner(categorieTable.getScene().getWindow());

            dialogStage.setScene(new Scene(root));

            dialogStage.show();

        } catch (Exception e) {

            showAlert("Erreur", "Impossible d'ouvrir les détails: " + e.getMessage());

            e.printStackTrace();

        }

    }



    private void openCategorieDialog(Categorie categorie) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/CategorieForm.fxml"));

            Parent root = loader.load();



            CategorieFormController controller = loader.getController();

            if (categorie != null) {

                controller.setCategorie(categorie);

            }



            Stage dialogStage = new Stage();

            dialogStage.setTitle(categorie == null ? "Ajouter une catégorie" : "Modifier la catégorie");

            dialogStage.initModality(Modality.WINDOW_MODAL);

            dialogStage.initOwner(categorieTable.getScene().getWindow());

            dialogStage.setScene(new Scene(root));

            dialogStage.showAndWait();



            if (controller.isSaved()) {

                loadCategories();

            }

        } catch (Exception e) {

            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());

            e.printStackTrace();

        }

    }



    private void handleDelete(Categorie selected) {

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

