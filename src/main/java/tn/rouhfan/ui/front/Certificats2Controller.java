package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.rouhfan.entities.Certificat;
import tn.rouhfan.services.CertificatService;
import tn.rouhfan.services.CertificatPdfGenerator;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Certificats2Controller implements Initializable {

    @FXML private TableView<Certificat> certificatTable;
    @FXML private TableColumn<Certificat, String> colNom, colNiveau, colScore, colDate, colCours, colParticipant;
    @FXML private Label lblInfo;
    @FXML private TextField searchField;
    @FXML private Button btnViewPdf;
    @FXML private Button btnDownloadPdf;

    private final CertificatService certService = new CertificatService();
    private final CertificatPdfGenerator pdfGenerator = new CertificatPdfGenerator();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private List<Certificat> allCertificats;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadCertificats();
        setupSearch();
        setupSelectionListener();
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom"));
        colNiveau.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("niveau"));
        colScore.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getScore() != null ? cd.getValue().getScore().toString() + "%" : ""));
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getDateObtention() != null ? dateFormat.format(cd.getValue().getDateObtention()) : ""));
        colCours.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getCours() != null ? cd.getValue().getCours().getNom() : ""));
        colParticipant.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getParticipant() != null ?
                        cd.getValue().getParticipant().getNom() + " " +
                                (cd.getValue().getParticipant().getPrenom() != null ? cd.getValue().getParticipant().getPrenom() : "") : ""));
    }

    private void setupSelectionListener() {
        certificatTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            if (btnViewPdf != null) btnViewPdf.setDisable(!hasSelection);
            if (btnDownloadPdf != null) btnDownloadPdf.setDisable(!hasSelection);
        });
    }

    private void loadCertificats() {
        try {
            allCertificats = certService.recuperer();
            certificatTable.setItems(FXCollections.observableArrayList(allCertificats));

            if (allCertificats.isEmpty()) {
                lblInfo.setText("Aucun certificat disponible");
            } else {
                lblInfo.setText(allCertificats.size() + " certificat(s) trouvé(s)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblInfo.setText("Erreur lors du chargement des certificats");
        }
    }

    private void setupSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                certificatTable.setItems(FXCollections.observableArrayList(allCertificats));
                lblInfo.setText(allCertificats.size() + " certificat(s) trouvé(s)");
            } else {
                String searchText = newValue.toLowerCase().trim();
                List<Certificat> filtered = new ArrayList<>();

                for (Certificat c : allCertificats) {
                    boolean match = false;

                    if (c.getNom() != null && c.getNom().toLowerCase().contains(searchText)) {
                        match = true;
                    }
                    else if (c.getNiveau() != null && c.getNiveau().toLowerCase().contains(searchText)) {
                        match = true;
                    }
                    else if (c.getCours() != null && c.getCours().getNom() != null &&
                            c.getCours().getNom().toLowerCase().contains(searchText)) {
                        match = true;
                    }
                    else if (c.getParticipant() != null && c.getParticipant().getNom() != null &&
                            c.getParticipant().getNom().toLowerCase().contains(searchText)) {
                        match = true;
                    }
                    else if (c.getParticipant() != null && c.getParticipant().getPrenom() != null &&
                            c.getParticipant().getPrenom().toLowerCase().contains(searchText)) {
                        match = true;
                    }

                    if (match) {
                        filtered.add(c);
                    }
                }

                certificatTable.setItems(FXCollections.observableArrayList(filtered));

                if (filtered.isEmpty()) {
                    lblInfo.setText("Aucun certificat ne correspond à \"" + newValue + "\"");
                } else {
                    lblInfo.setText(filtered.size() + " certificat(s) trouvé(s) pour \"" + newValue + "\"");
                }
            }
        });
    }

    @FXML
    private void refresh() {
        loadCertificats();
        if (searchField != null) {
            searchField.clear();
        }
    }

    @FXML
    private void viewPdf() {
        Certificat selected = certificatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un certificat");
            return;
        }

        try {
            // Créer un fichier temporaire
            String tempPath = System.getProperty("java.io.tmpdir") + "/certificat_" + selected.getId() + ".pdf";
            pdfGenerator.generateCertificat(selected, tempPath);

            // Ouvrir le PDF avec l'application par défaut
            java.awt.Desktop.getDesktop().open(new File(tempPath));

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le certificat: " + e.getMessage());
        }
    }

    @FXML
    private void downloadPdf() {
        Certificat selected = certificatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un certificat");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le certificat");
        fileChooser.setInitialFileName("certificat_" + selected.getCours().getNom().replaceAll(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                pdfGenerator.generateCertificat(selected, file.getAbsolutePath());
                showAlert("Succès", "Certificat enregistré avec succès !\n" + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible d'enregistrer le certificat: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}