package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.EvenementService;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EvenementsController implements Initializable {

    @FXML private TableView<Evenement> evenementTable;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colDate;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, Integer> colCapacite;
    @FXML private TableColumn<Evenement, String> colSponsor;
    @FXML private TableColumn<Evenement, Integer> colNbParticipants;
    @FXML private TableColumn<Evenement, String> colStatut;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    // ✅ Stats
    @FXML private Label statTotalEvents;
    @FXML private Label statTotalCapacity;
    @FXML private Label statTotalParticipants;
    @FXML private PieChart typePieChart;
    @FXML private BarChart<String, Number> participantsBarChart;

    // ✅ Calendar
    @FXML private GridPane calendarGrid;
    @FXML private Label monthYearLabel;
    private YearMonth currentYearMonth;

    private EvenementService evenementService;
    private ObservableList<Evenement> evenementsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evenementService = new EvenementService();
        currentYearMonth = YearMonth.now();
        setupColumns();
        setupSortCombo();
        setupSearch();
        loadEvenements();
    }

    private void setupColumns() {
        colTitre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitre()));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDateEvent() != null ? dateFormat.format(cellData.getValue().getDateEvent()) : ""
        ));
        colLieu.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLieu()));
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getType() != null ? cellData.getValue().getType() : ""
        ));
        colCapacite.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getCapacite() != null ? cellData.getValue().getCapacite() : 0
        ).asObject());
        colSponsor.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSponsor() != null ? cellData.getValue().getSponsor().getNom() : "Aucun"
        ));
        colNbParticipants.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getNbParticipants()
        ).asObject());
        colStatut.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatut() != null ? cellData.getValue().getStatut() : ""
        ));
    }

    private void setupSortCombo() {
        sortCombo.getItems().addAll(
                "Titre (A-Z)", "Titre (Z-A)",
                "Date Croissante", "Date Décroissante",
                "Lieu (A-Z)", "Capacité", "Statut"
        );
        sortCombo.setValue("Titre (A-Z)");
        sortCombo.setOnAction(e -> handleSort());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
    }

    private void loadEvenements() {
        try {
            evenementsList = FXCollections.observableArrayList(evenementService.recuperer());
            evenementTable.setItems(evenementsList);
            updateStats(evenementsList); // ✅ stats
            drawCalendar(); // ✅ calendar
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger les événements: " + e.getMessage());
        }
    }

    private void updateStats(ObservableList<Evenement> list) {
        if (statTotalEvents == null) return;

        statTotalEvents.setText(String.valueOf(list.size()));

        int capacity = 0;
        int participants = 0;

        for (Evenement e : list) {
            if (e.getCapacite() != null) capacity += e.getCapacite();
            participants += e.getNbParticipants();
        }

        statTotalCapacity.setText(String.valueOf(capacity));
        statTotalParticipants.setText(String.valueOf(participants));

        // Update Charts
        if (typePieChart != null && participantsBarChart != null) {
            typePieChart.getData().clear();
            participantsBarChart.getData().clear();

            Map<String, Long> countByType = list.stream()
                    .filter(e -> e.getType() != null)
                    .collect(Collectors.groupingBy(Evenement::getType, Collectors.counting()));

            for (Map.Entry<String, Long> entry : countByType.entrySet()) {
                typePieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }

            Map<String, Integer> participantsByType = list.stream()
                    .filter(e -> e.getType() != null)
                    .collect(Collectors.groupingBy(Evenement::getType, Collectors.summingInt(Evenement::getNbParticipants)));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Participants");
            for (Map.Entry<String, Integer> entry : participantsByType.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            participantsBarChart.getData().add(series);
        }
    }

    private void handleSearch() {
        try {
            String keyword = searchField.getText();
            ObservableList<Evenement> results = FXCollections.observableArrayList(
                    evenementService.rechercher(keyword)
            );
            evenementTable.setItems(results);
            updateStats(results); // ✅ stats
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
                case "Statut":
                    results = FXCollections.observableArrayList(evenementService.rechercher(keyword));
                    results.sort((e1, e2) -> e1.getStatut().compareToIgnoreCase(e2.getStatut()));
                    break;
            }

            if (results != null) {
                evenementTable.setItems(results);
                updateStats(results); // ✅ IMPORTANT
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

    // ✅ GOOGLE CALENDAR
    @FXML
    private void openGoogleCalendar(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://calendar.google.com/"));
        } catch (Exception e) {
            showAlert("Erreur", "❌ Impossible d'ouvrir Google Calendar");
        }
    }

    @FXML
    private void addEvenement(ActionEvent event) {
        EvenementFormDialog dialog = new EvenementFormDialog(null);
        dialog.show();

        if (dialog.isApproved()) {
            loadEvenements();
            showAlert("Succès", "✅ Événement ajouté !");
        }
    }

    @FXML
    private void editEvenement(ActionEvent event) {
        Evenement selected = evenementTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Attention", "⚠️ Sélectionnez un événement");
            return;
        }

        try {
            Evenement fullEvent = evenementService.findById(selected.getId());
            EvenementFormDialog dialog = new EvenementFormDialog(fullEvent);
            dialog.show();

            if (dialog.isApproved()) {
                loadEvenements();
                showAlert("Succès", "✅ Modifié !");
            }

        } catch (SQLException e) {
            showAlert("Erreur", "❌ " + e.getMessage());
        }
    }

    @FXML
    private void deleteEvenement(ActionEvent event) {
        Evenement selected = evenementTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Attention", "⚠️ Sélectionnez un événement");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer \"" + selected.getTitre() + "\" ?");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    evenementService.supprimer(selected.getId());
                    loadEvenements();
                    showAlert("Succès", "✅ Supprimé !");
                } catch (SQLException e) {
                    showAlert("Erreur", "❌ " + e.getMessage());
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

    // --- CALENDAR LOGIC ---
    @FXML
    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        drawCalendar();
    }

    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        drawCalendar();
    }

    private void drawCalendar() {
        if (calendarGrid == null || monthYearLabel == null) return;

        calendarGrid.getChildren().clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
        String formattedMonth = currentYearMonth.format(formatter);
        monthYearLabel.setText(formattedMonth.substring(0, 1).toUpperCase() + formattedMonth.substring(1));

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int daysInMonth = currentYearMonth.lengthOfMonth();
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Lundi, 7 = Dimanche

        int row = 0;
        int col = dayOfWeek - 1; // 0-indexé pour GridPane (0 = Lundi)

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            VBox dayBox = new VBox(2);
            dayBox.setPadding(new Insets(5));
            dayBox.setStyle("-fx-border-color: #e2e8f0; -fx-background-color: white;");
            dayBox.setPrefWidth(120);
            dayBox.setPrefHeight(100);

            if (date.equals(LocalDate.now())) {
                dayBox.setStyle("-fx-border-color: #6c2a90; -fx-background-color: #f3e5f5; -fx-border-width: 2;");
            }

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
            dayBox.getChildren().add(dayLabel);

            // Trouver les événements pour ce jour
            if (evenementsList != null) {
                for (Evenement e : evenementsList) {
                    if (e.getDateEvent() != null) {
                        LocalDate eventDate = e.getDateEvent().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (eventDate.equals(date)) {
                            Label eventLabel = new Label("• " + e.getTitre());
                            eventLabel.setStyle("-fx-font-size: 10; -fx-text-fill: white; -fx-background-color: #6c2a90; -fx-background-radius: 4; -fx-padding: 2 4;");
                            eventLabel.setWrapText(true);
                            eventLabel.setMaxWidth(110);
                            eventLabel.setOnMouseClicked(event -> {
                                evenementTable.getSelectionModel().select(e);
                                editEvenement(null);
                            });
                            eventLabel.setStyle(eventLabel.getStyle() + "-fx-cursor: hand;");
                            dayBox.getChildren().add(eventLabel);
                        }
                    }
                }
            }

            calendarGrid.add(dayBox, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }
}