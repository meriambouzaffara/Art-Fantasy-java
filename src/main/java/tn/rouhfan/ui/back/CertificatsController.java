package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CertificatService;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.UserService;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class CertificatsController implements Initializable {

    @FXML private TableView<Certificat> certificatTable;
    @FXML private TableColumn<Certificat, String> colNom, colNiveau, colScore, colDate, colParticipant, colCours;

    @FXML private TextField tfNom, tfScore, searchField;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<User> cbParticipant;
    @FXML private ComboBox<Cours> cbCours;
    @FXML private DatePicker dpDate;

    @FXML private VBox formPane;

    private final CertificatService certService = new CertificatService();
    private final CoursService coursService = new CoursService();
    private final UserService userService = new UserService();

    private Certificat selectedCertificat = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
    private final ObservableList<Certificat> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupComboBoxes();
        loadData();
        setupSearch();

        if (formPane != null) {
            formPane.setVisible(false);
            formPane.setManaged(false);
        }
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom"));
        colNiveau.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("niveau"));
        colScore.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getScore() != null ? cd.getValue().getScore().toString() + "%" : ""));
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getDateObtention() != null ? dateFormat.format(cd.getValue().getDateObtention()) : ""));
        colParticipant.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getParticipant() != null ? cd.getValue().getParticipant().getNom() : ""));
        colCours.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getCours() != null ? cd.getValue().getCours().getNom() : ""));
    }

    private void setupComboBoxes() {
        // Niveau
        cbNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));

        // Participant - Personnaliser l'affichage
        try {
            ObservableList<User> users = FXCollections.observableArrayList(userService.recuperer());
            cbParticipant.setItems(users);

            // Personnaliser l'affichage du ComboBox Participant
            cbParticipant.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getPrenom() + " " + user.getNom());
                    }
                }
            });

            cbParticipant.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText("Choisir un participant");
                    } else {
                        setText(user.getPrenom() + " " + user.getNom());
                    }
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cours - Personnaliser l'affichage (NE PLUS AFFICHER TOUTE LA BDD)
        try {
            ObservableList<Cours> cours = FXCollections.observableArrayList(coursService.recuperer());
            cbCours.setItems(cours);

            // Personnaliser l'affichage du ComboBox Cours
            cbCours.setCellFactory(param -> new ListCell<Cours>() {
                @Override
                protected void updateItem(Cours cours, boolean empty) {
                    super.updateItem(cours, empty);
                    if (empty || cours == null) {
                        setText(null);
                    } else {
                        // Afficher seulement le nom du cours
                        setText(cours.getNom());
                    }
                }
            });

            cbCours.setButtonCell(new ListCell<Cours>() {
                @Override
                protected void updateItem(Cours cours, boolean empty) {
                    super.updateItem(cours, empty);
                    if (empty || cours == null) {
                        setText("Choisir un cours");
                    } else {
                        setText(cours.getNom());
                    }
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            masterData.setAll(certService.recuperer());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        FilteredList<Certificat> filteredData = new FilteredList<>(masterData, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(certificat -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (certificat.getNom() != null && certificat.getNom().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (certificat.getNiveau() != null && certificat.getNiveau().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (certificat.getParticipant() != null && 
                           ((certificat.getParticipant().getNom() != null && certificat.getParticipant().getNom().toLowerCase().contains(lowerCaseFilter)) || 
                            (certificat.getParticipant().getPrenom() != null && certificat.getParticipant().getPrenom().toLowerCase().contains(lowerCaseFilter)))) {
                    return true;
                } else if (certificat.getCours() != null && certificat.getCours().getNom() != null && certificat.getCours().getNom().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (certificat.getScore() != null && certificat.getScore().toString().contains(lowerCaseFilter)) {
                    return true;
                }
                
                return false;
            });
        });

        SortedList<Certificat> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(certificatTable.comparatorProperty());
        certificatTable.setItems(sortedData);
    }

    @FXML
    private void handleSauvegarder() {
        try {
            Certificat c = (selectedCertificat == null) ? new Certificat() : selectedCertificat;
            c.setNom(tfNom.getText());
            c.setNiveau(cbNiveau.getValue());
            c.setScore(new BigDecimal(tfScore.getText().replace(",", ".")));
            c.setCours(cbCours.getValue());
            c.setParticipant(cbParticipant.getValue());

            if (dpDate.getValue() != null) {
                c.setDateObtention(java.sql.Date.valueOf(dpDate.getValue()));
            }

            if (selectedCertificat == null) certService.ajouter(c);
            else certService.modifier(c);

            loadData();
            handleAnnuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur de saisie ou de connexion.").show();
        }
    }

    @FXML
    private void addCertificat() {
        selectedCertificat = null;
        viderChamps();
        formPane.setVisible(true);
        formPane.setManaged(true);
    }

    @FXML
    private void editCertificat() {
        selectedCertificat = certificatTable.getSelectionModel().getSelectedItem();
        if (selectedCertificat == null) return;

        tfNom.setText(selectedCertificat.getNom());
        tfScore.setText(selectedCertificat.getScore().toString());
        cbNiveau.setValue(selectedCertificat.getNiveau());

        if (selectedCertificat.getDateObtention() != null) {
            dpDate.setValue(new java.sql.Date(
                    selectedCertificat.getDateObtention().getTime()).toLocalDate());
        }

        // ✅ Chercher par ID dans la liste de la ComboBox
        cbParticipant.getItems().stream()
                .filter(u -> u.getId() == selectedCertificat.getParticipant().getId())
                .findFirst()
                .ifPresent(cbParticipant::setValue);

        cbCours.getItems().stream()
                .filter(co -> co.getId() == selectedCertificat.getCours().getId())
                .findFirst()
                .ifPresent(cbCours::setValue);

        formPane.setVisible(true);
        formPane.setManaged(true);
    }

    @FXML
    private void deleteCertificat() {
        Certificat s = certificatTable.getSelectionModel().getSelectedItem();
        if (s != null) {
            try {
                certService.supprimer(s.getId());
                loadData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleAnnuler() {
        formPane.setVisible(false);
        formPane.setManaged(false);
        viderChamps();
    }

    @FXML
    private void refresh() {
        loadData();
    }

    private void viderChamps() {
        tfNom.clear();
        tfScore.clear();
        cbNiveau.setValue(null);
        dpDate.setValue(null);
        cbParticipant.setValue(null);
        cbCours.setValue(null);
    }
}