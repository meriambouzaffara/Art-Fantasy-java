package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.services.SponsorService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.ui.back.SponsorFormDialog;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class SponsorsFrontController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private SponsorService sponsorService;
    private ObservableList<Sponsor> sponsorsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sponsorService = new SponsorService();
        setupSortCombo();
        setupSearch();
        loadSponsors();
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Nom (A-Z)", "Nom (Z-A)", "Email (A-Z)", "Date Récente", "Date Ancienne");
            sortCombo.setValue("Nom (A-Z)");
            sortCombo.setOnAction(e -> handleSort());
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        }
    }

    private void loadSponsors() {
        try {
            sponsorsList = FXCollections.observableArrayList(sponsorService.recuperer());
            displayCards(sponsorsList);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger les sponsors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayCards(ObservableList<Sponsor> sponsors) {
        cardsContainer.getChildren().clear();
        
        for (Sponsor sponsor : sponsors) {
            VBox card = createSponsorCard(sponsor);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createSponsorCard(Sponsor sponsor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setPrefHeight(420);
        card.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-color: #ffffff; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 8, 0, 0, 2);");

        // Logo
        ImageView logoView = new ImageView();
        logoView.setFitWidth(280);
        logoView.setFitHeight(120);
        logoView.setPreserveRatio(true);
        logoView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5;");
        
        if (sponsor.getLogo() != null && !sponsor.getLogo().isEmpty()) {
            try {
                String imageUrl = ImageUtils.getImageUrl(sponsor.getLogo());
                Image img = new Image(imageUrl);
                logoView.setImage(img);
            } catch (Exception e) {
                logoView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-font-size: 50;");
            }
        } else {
            logoView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f5f5f5;");
        }

        // Nom
        Label nameLabel = new Label(sponsor.getNom() != null ? sponsor.getNom() : "");
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-wrap-text: true;");

        // Description
        Label descLabel = new Label(sponsor.getDescription() != null ? sponsor.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666; -fx-wrap-text: true;");
        descLabel.setWrapText(true);

        // Email
        Label emailLabel = new Label("📧 " + (sponsor.getEmail() != null ? sponsor.getEmail() : "N/A"));
        emailLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #1976d2;");
        emailLabel.setWrapText(true);

        // Téléphone
        Label telLabel = new Label("📞 " + (sponsor.getTel() != null ? sponsor.getTel() : "N/A"));
        telLabel.setStyle("-fx-font-size: 10;");

        // Adresse
        Label addressLabel = new Label("📍 " + (sponsor.getAdresse() != null ? sponsor.getAdresse() : "N/A"));
        addressLabel.setStyle("-fx-font-size: 10;");
        addressLabel.setWrapText(true);

        // Date
        Label dateLabel = new Label("📅 " + (sponsor.getCreatedAt() != null ? dateFormat.format(sponsor.getCreatedAt()) : "N/A"));
        dateLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #999;");

        // Spacer
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Button Contact
        Button contactBtn = new Button("Contacter");
        contactBtn.setStyle("-fx-font-size: 11; -fx-padding: 6 15; -fx-background-color: #4caf50; -fx-text-fill: white; -fx-border-radius: 5;");
        contactBtn.setPrefWidth(Double.MAX_VALUE);
        contactBtn.setOnAction(e -> handleContact(sponsor));

        card.getChildren().addAll(logoView, nameLabel, descLabel, emailLabel, telLabel, addressLabel, dateLabel, spacer, contactBtn);
        return card;
    }

    private void handleSearch() {
        try {
            String keyword = searchField.getText();
            ObservableList<Sponsor> results = FXCollections.observableArrayList(
                sponsorService.rechercher(keyword)
            );
            displayCards(results);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la recherche: " + e.getMessage());
        }
    }

    private void handleSort() {
        try {
            String sortOption = sortCombo.getValue();
            String keyword = searchField.getText();

            ObservableList<Sponsor> results = null;

            switch (sortOption) {
                case "Nom (A-Z)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "nom", true)
                    );
                    break;
                case "Nom (Z-A)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "nom", false)
                    );
                    break;
                case "Email (A-Z)":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "email", true)
                    );
                    break;
                case "Date Récente":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "date", false)
                    );
                    break;
                case "Date Ancienne":
                    results = FXCollections.observableArrayList(
                        sponsorService.rechercherEtTrier(keyword, "date", true)
                    );
                    break;
            }

            if (results != null) {
                displayCards(results);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors du tri: " + e.getMessage());
        }
    }

    @FXML
    private void refresh(ActionEvent event) {
        searchField.clear();
        sortCombo.setValue("Nom (A-Z)");
        loadSponsors();
    }

    @FXML
    private void addSponsor(ActionEvent event) {
        SponsorFormDialog dialog = new SponsorFormDialog(null);
        dialog.show();
        
        if (dialog.isApproved()) {
            loadSponsors();
            showAlert("Succès", "✅ Merci de votre intérêt en tant que sponsor!");
        }
    }

    private void handleContact(Sponsor sponsor) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Contact");
        info.setHeaderText("Sponsor: " + sponsor.getNom());
        info.setContentText("Email: " + sponsor.getEmail() + "\nTéléphone: " + sponsor.getTel());
        info.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

