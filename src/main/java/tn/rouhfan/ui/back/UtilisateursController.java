package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Contrôleur complet de la gestion des utilisateurs.
 * CRUD + Recherche + Filtrage + Tri + Statistiques + Export CSV.
 */
public class UtilisateursController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRoles;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, String> colType;
    @FXML private TableColumn<User, Date> colDate;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRoleCombo;
    @FXML private ComboBox<String> filterStatutCombo;

    @FXML private Label statTotal;
    @FXML private Label statAdmins;
    @FXML private Label statArtistes;
    @FXML private Label statParticipants;
    @FXML private Label statActifs;
    @FXML private Label statusLabel;

    private UserService userService;
    private ObservableList<User> usersList;
    private FilteredList<User> filteredUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        setupColumns();
        setupFilters();
        loadUsers();
        setupSearch();
        setStatus("Prêt — " + (usersList != null ? usersList.size() : 0) + " utilisateurs chargés");
    }

    // ════════════════════════════════════════
    //  COLONNES
    // ════════════════════════════════════════

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<User, Integer>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<User, String>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<User, String>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<User, String>("email"));
        colRoles.setCellValueFactory(new PropertyValueFactory<User, String>("roles"));
        colStatut.setCellValueFactory(new PropertyValueFactory<User, String>("statut"));
        colType.setCellValueFactory(new PropertyValueFactory<User, String>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<User, Date>("createdAt"));

        // Rôle : affichage lisible
        colRoles.setCellFactory(new Callback<TableColumn<User, String>, TableCell<User, String>>() {
            @Override
            public TableCell<User, String> call(TableColumn<User, String> param) {
                return new TableCell<User, String>() {
                    @Override
                    protected void updateItem(String roles, boolean empty) {
                        super.updateItem(roles, empty);
                        if (empty || roles == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            String clean = roles.replace("[", "").replace("]", "")
                                    .replace("\"", "").replace("'", "").trim();
                            setText(clean);
                            if (clean.contains("ADMIN")) {
                                setStyle("-fx-text-fill: #d63031; -fx-font-weight: bold;");
                            } else if (clean.contains("ARTISTE")) {
                                setStyle("-fx-text-fill: #6c5ce7; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                            }
                        }
                    }
                };
            }
        });

        // Statut : badge coloré
        colStatut.setCellFactory(new Callback<TableColumn<User, String>, TableCell<User, String>>() {
            @Override
            public TableCell<User, String> call(TableColumn<User, String> param) {
                return new TableCell<User, String>() {
                    @Override
                    protected void updateItem(String statut, boolean empty) {
                        super.updateItem(statut, empty);
                        if (empty || statut == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(statut);
                            if ("actif".equalsIgnoreCase(statut)) {
                                setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                            } else if ("inactif".equalsIgnoreCase(statut)) {
                                setStyle("-fx-text-fill: #636e72; -fx-font-weight: bold;");
                            } else if ("banni".equalsIgnoreCase(statut)) {
                                setStyle("-fx-text-fill: #d63031; -fx-font-weight: bold;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                };
            }
        });

        // Date : formatage dd/MM/yyyy
        colDate.setCellFactory(new Callback<TableColumn<User, Date>, TableCell<User, Date>>() {
            @Override
            public TableCell<User, Date> call(TableColumn<User, Date> param) {
                return new TableCell<User, Date>() {
                    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    @Override
                    protected void updateItem(Date date, boolean empty) {
                        super.updateItem(date, empty);
                        if (empty || date == null) {
                            setText(null);
                        } else {
                            setText(sdf.format(date));
                        }
                    }
                };
            }
        });
    }

    // ════════════════════════════════════════
    //  FILTRES
    // ════════════════════════════════════════

    private void setupFilters() {
        filterRoleCombo.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "ROLE_ADMIN", "ROLE_ARTISTE", "ROLE_PARTICIPANT"
        ));
        filterRoleCombo.getSelectionModel().selectFirst();
        filterRoleCombo.setOnAction(new javafx.event.EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                applyFilters();
            }
        });

        filterStatutCombo.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "actif", "inactif", "banni"
        ));
        filterStatutCombo.getSelectionModel().selectFirst();
        filterStatutCombo.setOnAction(new javafx.event.EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                applyFilters();
            }
        });
    }

    // ════════════════════════════════════════
    //  RECHERCHE
    // ════════════════════════════════════════

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener(new javafx.beans.value.ChangeListener<String>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends String> obs, String oldVal, String newVal) {
                    applyFilters();
                }
            });
        }
    }

    private void applyFilters() {
        if (filteredUsers == null) return;

        final String searchText = searchField != null ? searchField.getText() : "";
        final String selectedRole = filterRoleCombo.getValue();
        final String selectedStatut = filterStatutCombo.getValue();

        filteredUsers.setPredicate(new java.util.function.Predicate<User>() {
            @Override
            public boolean test(User user) {
                boolean matchesText = true;
                if (searchText != null && !searchText.isEmpty()) {
                    String lower = searchText.toLowerCase();
                    matchesText = (user.getNom() != null && user.getNom().toLowerCase().contains(lower))
                            || (user.getPrenom() != null && user.getPrenom().toLowerCase().contains(lower))
                            || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lower))
                            || String.valueOf(user.getId()).contains(lower);
                }

                boolean matchesRole = true;
                if (selectedRole != null && !selectedRole.equals("Tous les rôles")) {
                    matchesRole = user.getRoles() != null && user.getRoles().contains(selectedRole);
                }

                boolean matchesStatut = true;
                if (selectedStatut != null && !selectedStatut.equals("Tous les statuts")) {
                    matchesStatut = selectedStatut.equalsIgnoreCase(user.getStatut());
                }

                return matchesText && matchesRole && matchesStatut;
            }
        });

        setStatus(filteredUsers.size() + " utilisateur(s) affiché(s) sur " + usersList.size());
    }

    // ════════════════════════════════════════
    //  CHARGEMENT & STATISTIQUES
    // ════════════════════════════════════════

    private void loadUsers() {
        try {
            usersList = FXCollections.observableArrayList(userService.recuperer());
            filteredUsers = new FilteredList<User>(usersList);

            SortedList<User> sortedUsers = new SortedList<User>(filteredUsers);
            sortedUsers.comparatorProperty().bind(userTable.comparatorProperty());
            userTable.setItems(sortedUsers);

            updateStatistics();
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les utilisateurs:\n" + e.getMessage());
        }
    }

    private void updateStatistics() {
        if (usersList == null) return;

        int total = usersList.size();
        int admins = 0, artistes = 0, participants = 0, actifs = 0;
        for (User u : usersList) {
            String roles = u.getRoles();
            if (roles != null) {
                if (roles.contains("ROLE_ADMIN")) admins++;
                if (roles.contains("ROLE_ARTISTE")) artistes++;
                if (roles.contains("ROLE_PARTICIPANT")) participants++;
            }
            if ("actif".equalsIgnoreCase(u.getStatut())) actifs++;
        }

        statTotal.setText(String.valueOf(total));
        statAdmins.setText(String.valueOf(admins));
        statArtistes.setText(String.valueOf(artistes));
        statParticipants.setText(String.valueOf(participants));
        statActifs.setText(String.valueOf(actifs));
    }

    // ════════════════════════════════════════
    //  CRUD
    // ════════════════════════════════════════

    @FXML
    private void refresh(ActionEvent event) {
        searchField.clear();
        filterRoleCombo.getSelectionModel().selectFirst();
        filterStatutCombo.getSelectionModel().selectFirst();
        loadUsers();
        setStatus("🔄 Liste rafraîchie — " + usersList.size() + " utilisateurs");
    }

    @FXML
    private void addUser(ActionEvent event) {
        User newUser = UserFormDialog.showAddDialog();
        if (newUser != null) {
            try {
                // Contrôle d'unicité email
                User existing = userService.findByEmail(newUser.getEmail());
                if (existing != null) {
                    showError("Email déjà utilisé",
                            "Un utilisateur avec l'email \"" + newUser.getEmail() + "\" existe déjà (ID: " + existing.getId() + ").\n" +
                                    "Chaque utilisateur doit avoir un email unique.");
                    return;
                }

                userService.ajouter(newUser);
                loadUsers();
                applyFilters();
                showSuccess("Utilisateur ajouté",
                        newUser.getPrenom() + " " + newUser.getNom() + " a été ajouté avec succès !");
                setStatus("Utilisateur ajouté : " + newUser.getPrenom() + " " + newUser.getNom());
            } catch (SQLException e) {
                showError("Erreur d'ajout", "Impossible d'ajouter l'utilisateur:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void editUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner un utilisateur dans le tableau avant de cliquer sur Modifier.");
            return;
        }

        String originalPassword = selected.getPassword();

        User modified = UserFormDialog.showEditDialog(selected);
        if (modified != null) {
            try {
                // Contrôle d'unicité email (sauf pour l'utilisateur lui-même)
                User existing = userService.findByEmail(modified.getEmail());
                if (existing != null && existing.getId() != modified.getId()) {
                    showError("Email déjà utilisé",
                            "L'email \"" + modified.getEmail() + "\" est déjà utilisé par " +
                                    existing.getPrenom() + " " + existing.getNom() + " (ID: " + existing.getId() + ").\n" +
                                    "Chaque utilisateur doit avoir un email unique.");
                    return;
                }

                userService.modifier(modified);

                if (modified.getPassword() != null && !modified.getPassword().equals(originalPassword)) {
                    userService.updatePasswordHash(modified.getId(), modified.getPassword());
                }

                loadUsers();
                applyFilters();
                showSuccess("Utilisateur modifié",
                        "Les informations de " + modified.getPrenom() + " " + modified.getNom() + " ont été mises à jour.");
                setStatus("Utilisateur modifié : " + modified.getPrenom() + " " + modified.getNom());
            } catch (SQLException e) {
                showError("Erreur de modification", "Impossible de modifier l'utilisateur:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void deleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Aucune sélection", "Veuillez sélectionner un utilisateur dans le tableau avant de cliquer sur Supprimer.");
            return;
        }

        // Empêcher la suppression de son propre compte
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() == selected.getId()) {
            showError("Action interdite", "Vous ne pouvez pas supprimer votre propre compte !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'utilisateur #" + selected.getId());
        confirm.setContentText(
                "Êtes-vous sûr de vouloir supprimer :\n\n" +
                        selected.getPrenom() + " " + selected.getNom() + "\n" +
                        selected.getEmail() + "\n\n" +
                        "Cette action est irréversible !"
        );

        confirm.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
            @Override
            public void accept(ButtonType response) {
                if (response == ButtonType.OK) {
                    try {
                        String name = selected.getPrenom() + " " + selected.getNom();
                        userService.supprimer(selected.getId());
                        loadUsers();
                        applyFilters();
                        showSuccess("Utilisateur supprimé", name + " a été supprimé.");
                        setStatus("Utilisateur supprimé : " + name);
                    } catch (SQLException e) {
                        showError("Erreur de suppression",
                                "Impossible de supprimer l'utilisateur:\n" + e.getMessage() +
                                        "\n\nL'utilisateur a peut-être des données liées (cours, favoris, etc.).");
                    }
                }
            }
        });
    }

    // ════════════════════════════════════════
    //  EXPORT CSV
    // ════════════════════════════════════════

    @FXML
    private void exportCSV(ActionEvent event) {
        if (usersList == null || usersList.isEmpty()) {
            showWarning("Export impossible", "Aucun utilisateur à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les utilisateurs en CSV");
        fileChooser.setInitialFileName("utilisateurs_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        Stage stage = (Stage) userTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                tn.rouhfan.services.UserExportService exportService = new tn.rouhfan.services.UserExportService();
                int count = exportService.exportToCSV(new java.util.ArrayList<>(filteredUsers), file);
                showSuccess("Export réussi", count + " utilisateur(s) exporté(s) vers :\n" + file.getAbsolutePath());
                setStatus("📤 Export CSV : " + count + " utilisateurs");
            } catch (Exception e) {
                showError("Erreur d'export", "Impossible d'exporter :\n" + e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════
    //  ALERTES & STATUT
    // ════════════════════════════════════════

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}