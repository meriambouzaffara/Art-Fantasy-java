package tn.rouhfan.ui.back;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.File;
import java.io.IOException;

public class AvisController implements Initializable {

    @FXML private TableView<Reclamation> avisTable;
    @FXML private TableColumn<Reclamation, Integer> colId;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colMessage;
    @FXML private TableColumn<Reclamation, String> colUtilisateur;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, String> colCategorie;
    @FXML private TableColumn<Reclamation, Void> colActionImg;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statutFilter;

    private ReclamationService reclamationService;
    private ObservableList<Reclamation> reclamationsList;
    private FilteredList<Reclamation> filteredData;
    private Map<Integer, String> userNamesCache = new HashMap<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        System.out.println("searchField = " + searchField); // DEBUG
        System.out.println("colCategorie = " + colCategorie);

        reclamationService = new ReclamationService();

        setupFilters();
        setupColumns();
        setupSearch();   // 🔥 IMPORTANT
        loadReclamations();
    }

    // =========================
    // FILTERS
    // =========================
    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList("Tous","Reclamation","Avis"));
        typeFilter.setValue("Tous");

        statutFilter.setItems(FXCollections.observableArrayList("Tous", "en_attente", "traite"));
        statutFilter.setValue("Tous");
    }

    // =========================
    // TABLE COLUMNS
    // =========================
    private void setupColumns() {
        colId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());

        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType())
        );

        colSujet.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSujet())
        );

        colMessage.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription())
        );

        colUtilisateur.setCellValueFactory(cellData ->
                new SimpleStringProperty(userNamesCache.getOrDefault(cellData.getValue().getAuteurId(), String.valueOf(cellData.getValue().getAuteurId())))
        );

        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getCreatedAt() != null
                                ? dateFormat.format(cellData.getValue().getCreatedAt())
                                : ""
                )
        );

        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatut())
        );

        colCategorie.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getCategorie() != null
                                ? cellData.getValue().getCategorie()
                                : ""
                )
        );

        colActionImg.setCellFactory(param -> new TableCell<Reclamation, Void>() {
            private final Button btn = new Button("🖼️ Voir image");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-font-weight: bold;");
                btn.setOnAction((ActionEvent event) -> {
                    Reclamation selected = getTableView().getItems().get(getIndex());
                    if (selected.getImagePath() == null || selected.getImagePath().isEmpty()) {
                        showAlert("Info", "Aucune image disponible");
                        return;
                    }

                    File imgFile = new File("uploads/reclamations/" + selected.getImagePath());
                    if (!imgFile.exists()) {
                        showAlert("Info", "Aucune image disponible");
                        return;
                    }

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.initModality(javafx.stage.Modality.NONE); // Bouton minimiser
                    dialog.setResizable(true); // Bouton agrandir
                    dialog.setTitle("Pièce Jointe - " + selected.getSujet());
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.getDialogPane().setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20;");

                    Image image = new Image(imgFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(600);
                    imageView.setFitHeight(500);

                    imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

                    VBox box = new VBox(15, imageView);
                    box.setStyle("-fx-alignment: center; -fx-background-radius: 10; -fx-padding: 10; -fx-background-color: white;");
                    dialog.getDialogPane().setContent(box);

                    // Styliser le bouton fermer en rouge pour être cohérent
                    javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
                    if (closeBtn != null) {
                        closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 20px; -fx-background-radius: 5px;");
                    }

                    dialog.show();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    // =========================
    // SEARCH (PRO)
    // =========================
    private void setupSearch() {

        // Listener commun (search + filters)
        Runnable applyFilter = () -> {
            String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
            String selectedType = typeFilter.getValue();
            String selectedStatut = statutFilter.getValue();

            filteredData.setPredicate(reclamation -> {

                // 🔍 SEARCH
                boolean matchesSearch =
                        searchText.isEmpty()
                                || (reclamation.getSujet() != null && reclamation.getSujet().toLowerCase().contains(searchText))
                                || (reclamation.getDescription() != null && reclamation.getDescription().toLowerCase().contains(searchText))
                                || (reclamation.getCategorie() != null && reclamation.getCategorie().toLowerCase().contains(searchText));

                // 🧾 TYPE
                boolean matchesType =
                        selectedType.equals("Tous")
                                || (reclamation.getType() != null && reclamation.getType().equalsIgnoreCase(selectedType));

                // 📌 STATUT
                boolean matchesStatut =
                        selectedStatut.equals("Tous")
                                || (reclamation.getStatut() != null && reclamation.getStatut().equalsIgnoreCase(selectedStatut));

                return matchesSearch && matchesType && matchesStatut;
            });
        };

        // 🔥 Listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        statutFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
    }
    // =========================
    // LOAD DATA
    // =========================
    private void loadReclamations() {
        try {
            List<Reclamation> list = reclamationService.recuperer();
            UserService userService = new UserService();
            for (Reclamation r : list) {
                if (!userNamesCache.containsKey(r.getAuteurId())) {
                    User u = userService.findById(r.getAuteurId());
                    if (u != null) {
                        userNamesCache.put(r.getAuteurId(), u.getNom() + " " + u.getPrenom());
                    } else {
                        userNamesCache.put(r.getAuteurId(), "Inconnu");
                    }
                }
            }

            reclamationsList = FXCollections.observableArrayList(list);

            // IMPORTANT : injecter les données dans le filtre
            filteredData = new FilteredList<>(reclamationsList, b -> true);

            SortedList<Reclamation> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(avisTable.comparatorProperty());

            avisTable.setItems(sortedData);

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les réclamations: " + e.getMessage());
        }
    }

    // =========================
    // ACTIONS
    // =========================
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/ReponsePopup.fxml"));
            Parent root = loader.load();

            //  envoyer l'id au popup
            ReponsePopupController controller = loader.getController();
            controller.setReclamationId(selected.getId());

            // 🔥 Passer le statut à 'en_cours'
            if ("en_attente".equals(selected.getStatut())) {
                reclamationService.modifierStatut(selected.getId(), "en_cours");
                selected.setStatut("en_cours");
                avisTable.refresh();
            }

            Stage stage = new Stage();
            stage.setTitle("Répondre");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Attendre la fermeture

            // Recharger pour voir le statut 'traite' si une réponse a été envoyée
            loadReclamations();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le popup");
        }
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

    @FXML
    private void voirReponses(ActionEvent event) {

        Reclamation selected = avisTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Attention", "Sélectionnez une réclamation");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/ReponseList.fxml"));
            Parent root = loader.load();

            ReponseListController controller = loader.getController();
            controller.setReclamationId(selected.getId());

            Stage stage = new Stage();
            stage.setTitle("Réponses");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("📊 Dashboard Global Administrateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le dashboard administrateur.");
        }
    }

    // =========================
    // ALERT
    // =========================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
