package tn.rouhfan.ui.back;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.rouhfan.entities.ActivityLog;
import tn.rouhfan.services.ActivityLogService;
import tn.rouhfan.services.LogExportService;
import tn.rouhfan.services.HuggingFaceService;
import tn.rouhfan.services.UserService;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Contrôleur du Dashboard d'utilisation IA et des logs d'activité.
 *
 * Fonctionnalités :
 * ─────────────────────────────────────
 * 1. Statistiques AI Usage :
 *    - Nombre total d'appels API Hugging Face
 *    - Nombre de succès et d'erreurs
 *    - Utilisateurs les plus actifs
 *
 * 2. Table des logs :
 *    - Affichage de tous les logs d'activité
 *    - Filtrage par date, utilisateur, type d'action
 *
 * 3. Export CSV :
 *    - Export des logs filtrés en fichier CSV
 */
public class AIUsageDashboardController implements Initializable {

    // ═══════════════════════════════════════
    //  Statistiques AI
    // ═══════════════════════════════════════

    @FXML private Label statApiCalls;
    @FXML private Label statApiErrors;
    @FXML private Label statTotalLogs;
    @FXML private Label statTotalErrors;

    // ═══════════════════════════════════════
    //  Table des logs
    // ═══════════════════════════════════════

    @FXML private TableView<ActivityLog> logsTable;
    @FXML private TableColumn<ActivityLog, Integer> colId;
    @FXML private TableColumn<ActivityLog, String> colDate;
    @FXML private TableColumn<ActivityLog, Integer> colUserId;
    @FXML private TableColumn<ActivityLog, String> colAction;
    @FXML private TableColumn<ActivityLog, String> colLevel;
    @FXML private TableColumn<ActivityLog, String> colDetails;

    // ═══════════════════════════════════════
    //  Filtres
    // ═══════════════════════════════════════

    @FXML private ComboBox<String> filterActionType;
    @FXML private TextField filterUserId;
    @FXML private DatePicker filterStartDate;
    @FXML private DatePicker filterEndDate;

    // ═══════════════════════════════════════
    //  Top Utilisateurs
    // ═══════════════════════════════════════

    @FXML private VBox topUsersContainer;

    // ═══════════════════════════════════════
    //  Services
    // ═══════════════════════════════════════

    private final ActivityLogService activityLogService = new ActivityLogService();
    private final LogExportService logExportService = new LogExportService();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private ObservableList<ActivityLog> currentLogs = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ── Initialisation des colonnes de la table ──
        setupTableColumns();

        // ── Charger les types d'actions pour le filtre ──
        setupFilters();

        // ── Charger les données ──
        loadStatistics();
        loadLogs();
        loadTopUsers();
    }

    // ═══════════════════════════════════════
    //  Configuration de la table
    // ═══════════════════════════════════════

    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colDate.setCellValueFactory(data -> {
            Date ts = data.getValue().getTimestamp();
            return new SimpleStringProperty(ts != null ? sdf.format(ts) : "");
        });
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getUserId()).asObject());
        colAction.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActionType()));
        colLevel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLevel()));
        colDetails.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));

        // ── Colorer les lignes selon le niveau ──
        logsTable.setRowFactory(tv -> new TableRow<ActivityLog>() {
            @Override
            protected void updateItem(ActivityLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    switch (item.getLevel()) {
                        case "ERROR":
                            setStyle("-fx-background-color: rgba(214, 48, 49, 0.08);");
                            break;
                        case "WARN":
                            setStyle("-fx-background-color: rgba(253, 203, 110, 0.15);");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        logsTable.setItems(currentLogs);
    }

    // ═══════════════════════════════════════
    //  Filtres
    // ═══════════════════════════════════════

    private void setupFilters() {
        // Charger les types d'actions distincts
        List<String> actionTypes = activityLogService.getDistinctActionTypes();
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous"); // Option par défaut
        items.addAll(actionTypes);
        filterActionType.setItems(items);
        filterActionType.setValue("Tous");
    }

    /**
     * Applique les filtres sélectionnés et recharge les logs.
     */
    @FXML
    private void applyFilters(ActionEvent event) {
        loadLogs();
    }

    /**
     * Réinitialise tous les filtres.
     */
    @FXML
    private void resetFilters(ActionEvent event) {
        filterActionType.setValue("Tous");
        filterUserId.clear();
        filterStartDate.setValue(null);
        filterEndDate.setValue(null);
        loadLogs();
    }

    // ═══════════════════════════════════════
    //  Chargement des données
    // ═══════════════════════════════════════

    private void loadStatistics() {
        try {
            // Statistiques des logs d'activité en base
            int apiCalls = activityLogService.countHuggingFaceApiCalls();
            int apiErrors = activityLogService.countHuggingFaceErrors();
            int totalLogs = activityLogService.countTotal();
            int totalErrors = activityLogService.countErrors();

            if (statApiCalls != null) statApiCalls.setText(String.valueOf(apiCalls));
            if (statApiErrors != null) statApiErrors.setText(String.valueOf(apiErrors));
            if (statTotalLogs != null) statTotalLogs.setText(String.valueOf(totalLogs));
            if (statTotalErrors != null) statTotalErrors.setText(String.valueOf(totalErrors));

        } catch (Exception e) {
            AppLogger.error("Erreur chargement statistiques IA", e);
        }
    }

    private void loadLogs() {
        try {
            // ── Extraire les valeurs des filtres ──
            String actionType = filterActionType != null ? filterActionType.getValue() : "Tous";
            if ("Tous".equals(actionType)) actionType = null;

            Integer userId = null;
            if (filterUserId != null && !filterUserId.getText().trim().isEmpty()) {
                try {
                    userId = Integer.parseInt(filterUserId.getText().trim());
                } catch (NumberFormatException ignored) {}
            }

            Date startDate = null;
            if (filterStartDate != null && filterStartDate.getValue() != null) {
                startDate = Date.from(filterStartDate.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            Date endDate = null;
            if (filterEndDate != null && filterEndDate.getValue() != null) {
                endDate = Date.from(filterEndDate.getValue()
                        .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            // ── Requête filtrée ──
            List<ActivityLog> logs = activityLogService.searchLogs(actionType, userId, startDate, endDate, 500);
            currentLogs.setAll(logs);

        } catch (Exception e) {
            AppLogger.error("Erreur chargement logs", e);
        }
    }

    private void loadTopUsers() {
        if (topUsersContainer == null) return;

        try {
            Map<Integer, Integer> topUsers = activityLogService.getMostActiveUsers(5);
            UserService userService = new UserService();
            topUsersContainer.getChildren().clear();

            if (topUsers.isEmpty()) {
                Label emptyLabel = new Label("Aucune donnée disponible");
                emptyLabel.setStyle("-fx-text-fill: #9b8fb5; -fx-font-size: 13px; -fx-font-style: italic;");
                topUsersContainer.getChildren().add(emptyLabel);
                return;
            }

            int rank = 1;
            for (Map.Entry<Integer, Integer> entry : topUsers.entrySet()) {
                int uid = entry.getKey();
                int count = entry.getValue();

                // Tenter de récupérer le nom de l'utilisateur
                String userName = "User #" + uid;
                try {
                    // Recherche par ID dans la base
                    User user = userService.findById(uid);
                    if (user != null) {
                        userName = user.getPrenom() + " " + user.getNom();
                    }
                } catch (Exception ignored) {}

                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "  " + rank + ".";

                Label userLabel = new Label(medal + "  " + userName + " — " + count + " actions");
                userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2d1b4e; "
                        + "-fx-padding: 8 12; -fx-background-color: " + (rank <= 3 ? "#f0eef5" : "transparent")
                        + "; -fx-background-radius: 10; -fx-font-weight: " + (rank <= 3 ? "bold" : "normal") + ";");
                userLabel.setMaxWidth(Double.MAX_VALUE);
                topUsersContainer.getChildren().add(userLabel);
                rank++;
            }

        } catch (Exception e) {
            AppLogger.error("Erreur chargement top utilisateurs", e);
        }
    }

    // ═══════════════════════════════════════
    //  Export CSV
    // ═══════════════════════════════════════

    /**
     * Exporte les logs actuellement affichés (avec filtres) en CSV.
     */
    @FXML
    private void exportLogsCSV(ActionEvent event) {
        if (currentLogs.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Export",
                    "Aucun log à exporter. Ajustez vos filtres.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les logs en CSV");
        fileChooser.setInitialFileName("activity_logs_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        File file = fileChooser.showSaveDialog(logsTable.getScene().getWindow());
        if (file != null) {
            try {
                int count = logExportService.exportToCSV(new ArrayList<>(currentLogs), file);
                showAlert(Alert.AlertType.INFORMATION, "Export réussi",
                        "✅ " + count + " logs exportés avec succès !\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AppLogger.error("Erreur export CSV", e);
                showAlert(Alert.AlertType.ERROR, "Erreur d'export",
                        "❌ Erreur lors de l'export : " + e.getMessage());
            }
        }
    }

    /**
     * Rafraîchit toutes les données du dashboard.
     */
    @FXML
    private void refreshDashboard(ActionEvent event) {
        loadStatistics();
        loadLogs();
        loadTopUsers();
        setupFilters(); // Recharger les types d'actions
    }

    // ═══════════════════════════════════════
    //  Utilitaires
    // ═══════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
