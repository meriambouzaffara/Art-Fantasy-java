package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;
import tn.rouhfan.tools.SessionManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReclamationFrontController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    @FXML private TableView<Reclamation> table;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colDesc;
    @FXML private TableColumn<Reclamation, Date> colDate;
    @FXML private TableColumn<Reclamation, String> colStatut;

    private ReclamationService rs = new ReclamationService();
    private ObservableList<Reclamation> list;

    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    public void initialize() {

        // ================= TABLE =================
        colSujet.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getSujet() != null ? data.getValue().getSujet() : ""
                ));

        colDesc.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDescription() != null ? data.getValue().getDescription() : ""
                ));

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCreatedAt())
        );

        colDate.setCellFactory(col -> new TableCell<Reclamation, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : sdf.format(item));
            }
        });

        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatut() != null ? data.getValue().getStatut() : ""
                ));

        // 🔽 TRI PAR DÉFAUT (date desc)
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(colDate);

        // ================= FILTER =================
        statutFilter.setItems(FXCollections.observableArrayList(
                "Tous", "en_attente", "en_cours", "traite"
        ));
        statutFilter.setValue("Tous");

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {
        try {
            list = FXCollections.observableArrayList(rs.recuperer());

            FilteredList<Reclamation> filtered = new FilteredList<>(list, b -> true);

            Runnable updateFilter = () -> {
                String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
                String statut = statutFilter.getValue();

                filtered.setPredicate(r -> {

                    boolean matchSearch =
                            (r.getSujet() != null && r.getSujet().toLowerCase().contains(search))
                                    || (r.getDescription() != null && r.getDescription().toLowerCase().contains(search));

                    boolean matchStatut =
                            statut.equals("Tous") ||
                                    (r.getStatut() != null && r.getStatut().equalsIgnoreCase(statut));

                    return matchSearch && matchStatut;
                });
            };

            searchField.textProperty().addListener((obs, o, n) -> updateFilter.run());
            statutFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());

            SortedList<Reclamation> sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(table.comparatorProperty());

            table.setItems(sorted);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= POPUP AJOUT =================
    @FXML
    private void openPopupAjout(ActionEvent event) {

        // 🔐 Vérifier user connecté
        if (SessionManager.getInstance().getCurrentUser() == null) {
            showAlert("Erreur", "Vous devez être connecté !");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter Réclamation");

        TextField sujet = new TextField();
        sujet.setPromptText("Sujet");

        TextField desc = new TextField();
        desc.setPromptText("Description");

        VBox box = new VBox(10, sujet, desc);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {

                String sujetTxt = sujet.getText().trim();
                String descTxt = desc.getText().trim();

                // ================= VALIDATION =================

                if (sujetTxt.isEmpty() || descTxt.isEmpty()) {
                    showAlert("Erreur", "Champs vides !");
                    return;
                }

                if (sujetTxt.length() < 3) {
                    showAlert("Erreur", "Sujet trop court (min 3 caractères)");
                    return;
                }

                if (descTxt.length() < 5) {
                    showAlert("Erreur", "Description trop courte (min 5 caractères)");
                    return;
                }

                if (descTxt.length() > 500) {
                    showAlert("Erreur", "Message trop long !");
                    return;
                }

                if (!sujetTxt.matches("[a-zA-ZÀ-ÿ\\s]+")) {
                    showAlert("Erreur", "Sujet invalide (lettres seulement)");
                    return;
                }

                try {
                    // 🔥 récupérer user connecté
                    int userId = SessionManager
                            .getInstance()
                            .getCurrentUser()
                            .getId();

                    Reclamation r = new Reclamation(
                            sujetTxt,
                            descTxt,
                            "en_attente",
                            new Date(),
                            userId,
                            "autre"
                    );

                    rs.ajouter(r);
                    loadData();

                    showAlert("Succès", "Réclamation ajoutée ✅");

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ================= VOIR REPONSES =================
    @FXML
    private void voirReponses(ActionEvent event) {

        Reclamation selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Erreur", "Sélectionner une réclamation");
            return;
        }

        try {
            ReponseReclamationService service = new ReponseReclamationService();
            List<ReponseReclamation> list = service.recuperer();

            StringBuilder content = new StringBuilder();

            for (ReponseReclamation r : list) {
                if (r.getReclamationId() == selected.getId()) {
                    content.append("• ").append(r.getMessage()).append("\n");
                }
            }

            if (content.length() == 0) {
                content.append("Aucune réponse.");
            }

            showAlert("Réponses", content.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ALERT =================
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}