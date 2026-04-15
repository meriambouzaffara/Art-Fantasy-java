package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.OeuvreService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class GalerieController implements Initializable {

    @FXML private TableView<Oeuvre> oeuvreTable;
    @FXML private TableColumn<Oeuvre, Integer> colId;
    @FXML private TableColumn<Oeuvre, String> colImage;
    @FXML private TableColumn<Oeuvre, String> colTitre;
    @FXML private TableColumn<Oeuvre, String> colArtiste;
    @FXML private TableColumn<Oeuvre, String> colCategorie;
    @FXML private TableColumn<Oeuvre, String> colPrix;
    @FXML private TableColumn<Oeuvre, String> colStatus;
    @FXML private TableColumn<Oeuvre, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;

    private OeuvreService oeuvreService;
    private ObservableList<Oeuvre> oeuvresList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        setupColumns();
        loadOeuvres();
        setupFilters();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());

        // Image Column
        colImage.setCellFactory(param -> new TableCell<Oeuvre, String>() {
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
                    Oeuvre o = getTableRow().getItem();
                    String imagePath = o.getImage();
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

        colTitre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitre()));

        colArtiste.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getUser() != null ?
                        cellData.getValue().getUser().getNom() + " " + cellData.getValue().getUser().getPrenom() : "artiste artist"
        ));

        colCategorie.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCategorie() != null ? cellData.getValue().getCategorie().getNomCategorie() : "Non classé"
        ));

        colPrix.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPrix() != null ? cellData.getValue().getPrix().toString() + " DT" : "0.00 DT"
        ));

        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut()));

        // Custom Cell for Status
        colStatus.setCellFactory(column -> new TableCell<Oeuvre, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("status-purple");
                } else {
                    setText(item.toLowerCase());
                    if (!getStyleClass().contains("status-purple")) {
                        getStyleClass().add("status-purple");
                    }
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<Oeuvre, Void>() {
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
                    Oeuvre o = getTableView().getItems().get(getIndex());
                    openOeuvreDetails(o);
                });

                editBtn.setOnAction(e -> {
                    Oeuvre o = getTableView().getItems().get(getIndex());
                    openOeuvreDialog(o);
                });

                deleteBtn.setOnAction(e -> {
                    Oeuvre o = getTableView().getItems().get(getIndex());
                    handleDelete(o);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadOeuvres() {
        try {
            oeuvresList = FXCollections.observableArrayList(oeuvreService.recuperer());
            applyFilters();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les œuvres: " + e.getMessage());
        }
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, old, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        orderCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());

        statusFilter.setValue("Tous");
    }

    private void applyFilters() {
        if (oeuvresList == null) return;

        FilteredList<Oeuvre> filteredData = new FilteredList<>(oeuvresList, p -> {
            // Filter by search text
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean matchesSearch = p.getTitre().toLowerCase().contains(searchText);

            // Filter by status
            String selectedStatus = statusFilter.getValue();
            boolean matchesStatus = selectedStatus == null || selectedStatus.equals("Tous") || p.getStatut().equalsIgnoreCase(selectedStatus);

            return matchesSearch && matchesStatus;
        });

        SortedList<Oeuvre> sortedData = new SortedList<>(filteredData);

        // Sorting logic
        String sortOption = sortCombo.getValue();
        String orderOption = orderCombo.getValue();
        boolean ascending = orderOption == null || orderOption.equals("Asc");

        if (sortOption != null) {
            Comparator<Oeuvre> comparator;
            switch (sortOption) {
                case "Titre":
                    comparator = Comparator.comparing(Oeuvre::getTitre, String.CASE_INSENSITIVE_ORDER);
                    break;
                case "Prix":
                    comparator = Comparator.comparing(Oeuvre::getPrix);
                    break;
                default:
                    comparator = Comparator.comparing(Oeuvre::getTitre, String.CASE_INSENSITIVE_ORDER);
                    break;
            }

            if (!ascending) {
                comparator = comparator.reversed();
            }
            sortedData.setComparator(comparator);
        }

        oeuvreTable.setItems(sortedData);
    }

    @FXML
    private void refresh(ActionEvent event) {
        loadOeuvres();
    }

    @FXML
    private void addOeuvre(ActionEvent event) {
        openOeuvreDialog(null);
    }

    private void openOeuvreDetails(Oeuvre oeuvre) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/back/DetailsDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            DetailsDialogController controller = loader.getController();
            controller.setOeuvre(oeuvre);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Détails de l'œuvre");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(oeuvreTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.show();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir les détails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openOeuvreDialog(Oeuvre oeuvre) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/back/OeuvreFormDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            OeuvreFormController controller = loader.getController();
            if (oeuvre != null) {
                controller.setOeuvre(oeuvre);
            }

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle(oeuvre == null ? "Ajouter une œuvre" : "Modifier l'œuvre");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(oeuvreTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadOeuvres();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(Oeuvre selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'œuvre");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer: " + selected.getTitre() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    oeuvreService.supprimer(selected.getId());
                    loadOeuvres();
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
