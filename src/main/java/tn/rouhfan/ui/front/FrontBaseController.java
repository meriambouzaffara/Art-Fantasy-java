package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.Router;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FrontBaseController {

    @FXML private VBox contentHost;
    @FXML private VBox heroSection;

    // Navbar buttons
    @FXML private HBox guestButtons;
    @FXML private HBox userButtons;
    @FXML private Label welcomeLabel;
    @FXML private Button profileBtn;

    @FXML
    public void initialize() {
        showHero(true);
        setupNavbarByRole();
    }

    /**
     * Configure la navbar selon l'utilisateur connecté :
     * - Non connecté : Sign Up + Login
     * - Artiste : Bienvenue + Profil + Déconnexion
     * - Participant : Bienvenue + Profil + Déconnexion
     * - Admin ne devrait jamais arriver ici (redirigé vers Dashboard)
     */
    private void setupNavbarByRole() {
        SessionManager session = SessionManager.getInstance();
        User currentUser = session.getCurrentUser();

        if (currentUser == null) {
            // Non connecté : afficher Sign Up + Login
            guestButtons.setVisible(true);
            guestButtons.setManaged(true);
            userButtons.setVisible(false);
            userButtons.setManaged(false);
        } else {
            // Connecté : afficher Bienvenue + Profil + Déconnexion
            guestButtons.setVisible(false);
            guestButtons.setManaged(false);
            userButtons.setVisible(true);
            userButtons.setManaged(true);

            String role = session.getRole();
            String roleEmoji;
            if (role != null && role.toUpperCase().contains("ARTISTE")) {
                roleEmoji = "🎨";
            } else {
                roleEmoji = "🎭";
            }
            welcomeLabel.setText(roleEmoji + " " + currentUser.getPrenom() + " " + currentUser.getNom());
        }
    }

    // ==================== Navigation ====================

    @FXML
    private void goHome(ActionEvent event) {
        showHero(true);
        contentHost.getChildren().clear();
    }

    @FXML
    private void goCategories(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/GalerieFront.fxml"));
            Parent root = loader.load();
            GalerieFrontController controller = loader.getController();
            controller.setCategoryMode(true);
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Catégories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goOeuvres(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/GalerieFront.fxml"));
            Parent root = loader.load();
            GalerieFrontController controller = loader.getController();
            controller.setCategoryMode(false);
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Oeuvres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goEvenements(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            VBox view = Router.loadView("/ui/front/EvenementsFront.fxml");
            contentHost.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goSponsors(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            VBox view = Router.loadView("/ui/front/SponsorsFront.fxml");
            contentHost.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void goCours(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goMagasin(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }
    @FXML private void goAbout(ActionEvent event) { showHero(false); contentHost.getChildren().clear(); }

    @FXML

    private void goAvis(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();

        // 🔐 Vérifier session
        if (SessionManager.getInstance().getCurrentUser() == null) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez vous connecter pour accéder aux réclamations !");
            alert.showAndWait();

            try {
                // Redirection vers login
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
                stage.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        // ✅ Si connecté → charger page
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/ui/front/ReclamationFront.fxml")
            );
            contentHost.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ==================== Auth ====================

    @FXML
    private void signup(ActionEvent event) {
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/SignUp.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void login(ActionEvent event) {
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openProfile(ActionEvent event) {
        showHero(false);
        contentHost.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/ProfileView.fxml"));
            Parent root = loader.load();
            contentHost.getChildren().add(root);
        } catch (IOException e) {
            System.err.println("[FrontBase] Erreur chargement Profil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionManager.getInstance().logout();
        System.out.println("[FrontBase] Déconnexion effectuée");
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/front/FrontBase.fxml"));
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void contact(ActionEvent event) {
    }

    // ==================== Helpers ====================

    private void showHero(boolean show) {
        heroSection.setVisible(show);
        heroSection.setManaged(show);
        contentHost.setVisible(!show);
        contentHost.setManaged(!show);
    }

    public static class ReclamationFrontController {

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

            // 🔽 TRI PAR DÉFAUT
            colDate.setSortType(TableColumn.SortType.DESCENDING);
            table.getSortOrder().add(colDate);

            // ================= FILTER =================
            statutFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "en_attente", "en_cours", "traitee"
            ));
            statutFilter.setValue("Tous");

            loadData();
        }

        // ================= LOAD =================
        private void loadData() {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                int currentUserId = (currentUser != null) ? currentUser.getId() : -1;
                
                if (currentUserId != -1) {
                    list = FXCollections.observableArrayList(rs.recupererParUser(currentUserId));
                } else {
                    list = FXCollections.observableArrayList();
                }

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

                    // ❌ vide
                    if (sujetTxt.isEmpty() || descTxt.isEmpty()) {
                        showAlert("Erreur", "Champs vides !");
                        return;
                    }

                    // ❌ longueur sujet
                    if (sujetTxt.length() < 3) {
                        showAlert("Erreur", "Sujet trop court (min 3 caractères)");
                        return;
                    }

                    // ❌ longueur description (🔥 TON PROBLÈME ICI)
                    if (descTxt.length() < 5) {
                        showAlert("Erreur", "Description trop courte (min 5 caractères)");
                        return;
                    }

                    // ❌ trop long
                    if (descTxt.length() > 500) {
                        showAlert("Erreur", "Message trop long !");
                        return;
                    }

                    // ❌ validation texte (lettres + espaces)
                    if (!sujetTxt.matches("[a-zA-ZÀ-ÿ\\s]+")) {
                        showAlert("Erreur", "Sujet invalide (lettres seulement)");
                        return;
                    }

                    try {
                        User currentUser = SessionManager.getInstance().getCurrentUser();
                        int currentUserId = (currentUser != null) ? currentUser.getId() : 1;

                        Reclamation r = new Reclamation(
                                sujetTxt,
                                descTxt,
                                "en_attente",
                                new Date(),
                                currentUserId,
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
}
