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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.SponsorService;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.ui.back.SponsorFormDialog;
import java.util.List;

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
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.getStyleClass().add("card");

        // Logo Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("card-image-container");
        imageContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12 12 0 0;");
        imageContainer.setPrefHeight(150.0);

        ImageView logoView = new ImageView();
        logoView.setFitWidth(280);
        logoView.setFitHeight(150);
        logoView.setPreserveRatio(true);

        if (sponsor.getLogo() != null && !sponsor.getLogo().isEmpty()) {
            try {
                String imageUrl = ImageUtils.getImageUrl(sponsor.getLogo());
                Image img = new Image(imageUrl);
                logoView.setImage(img);
            } catch (Exception e) {}
        }
        imageContainer.getChildren().add(logoView);

        // Content
        VBox contentBox = new VBox(12);
        contentBox.getStyleClass().add("card-content");

        // Header (Nom)
        Label nameLabel = new Label(sponsor.getNom() != null ? sponsor.getNom() : "");
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 16;");
        nameLabel.setWrapText(true);

        // Email
        Label emailLabel = new Label("📧 " + (sponsor.getEmail() != null ? sponsor.getEmail() : "N/A"));
        emailLabel.setStyle("-fx-text-fill: #6c2a90; -fx-font-weight: bold; -fx-font-size: 12;");
        emailLabel.setWrapText(true);

        // Details Box (Phone / Address)
        VBox detailsBox = new VBox(5);
        Label telLabel = new Label("📞 " + (sponsor.getTel() != null ? sponsor.getTel() : "N/A"));
        telLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        Label addressLabel = new Label("📍 " + (sponsor.getAdresse() != null ? sponsor.getAdresse() : "N/A"));
        addressLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
        addressLabel.setWrapText(true);

        detailsBox.getChildren().addAll(telLabel, addressLabel);

        // Date
        HBox footerBox = new HBox();
        Label dateLabel = new Label("Partenaire depuis le " + (sponsor.getCreatedAt() != null ? dateFormat.format(sponsor.getCreatedAt()) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-style: italic;");
        footerBox.getChildren().add(dateLabel);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Boutons HBox
        HBox buttonBox = new HBox(10);
        
        Button contactBtn = new Button("✉️ Contacter");
        contactBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contactBtn, Priority.ALWAYS);
        contactBtn.setStyle("-fx-background-color: linear-gradient(to right, #6c2a90, #9c4dcc); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 13; -fx-cursor: hand;");
        contactBtn.setOnAction(e -> handleContact(sponsor));

        Button viewEventsBtn = new Button("📅 Événements");
        viewEventsBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(viewEventsBtn, Priority.ALWAYS);
        viewEventsBtn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 13; -fx-cursor: hand;");
        viewEventsBtn.setOnAction(e -> handleViewEvents(sponsor));
        
        buttonBox.getChildren().addAll(contactBtn, viewEventsBtn);

        contentBox.getChildren().addAll(nameLabel, emailLabel, detailsBox, footerBox, spacer, buttonBox);
        card.getChildren().addAll(imageContainer, contentBox);
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

    private void handleViewEvents(Sponsor sponsor) {
        try {
            EvenementService evenementService = new EvenementService();
            List<Evenement> events = evenementService.getEvenementsBySponsor(sponsor.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Événements sponsorisés par " + sponsor.getNom());
            dialog.setHeaderText("Liste des événements");
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);

            if (events.isEmpty()) {
                dialogPane.setContent(new Label("Aucun événement sponsorisé pour le moment."));
            } else {
                VBox container = new VBox(15);
                container.setPadding(new Insets(15));
                container.setStyle("-fx-background-color: #f0f2f5;");
                for (Evenement event : events) {
                    HBox card = new HBox(15);
                    card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");
                    
                    ImageView imgView = new ImageView();
                    imgView.setFitWidth(80);
                    imgView.setFitHeight(80);
                    imgView.setPreserveRatio(true);
                    if (event.getImage() != null && !event.getImage().isEmpty()) {
                        try {
                            String imageUrl = tn.rouhfan.tools.ImageUtils.getImageUrl(event.getImage());
                            imgView.setImage(new Image(imageUrl));
                        } catch (Exception e) {}
                    } else {
                        // Fallback image styling or placeholder could go here
                        imgView.setStyle("-fx-background-color: #e2e8f0;");
                    }
                    
                    VBox content = new VBox(5);
                    HBox.setHgrow(content, Priority.ALWAYS);
                    
                    Label title = new Label("🎯 " + event.getTitre());
                    title.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #2c3e50;");
                    title.setWrapText(true);
                    
                    Label type = new Label(event.getType() != null ? event.getType() : "Événement");
                    type.setStyle("-fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #e67e22; -fx-padding: 2 8; -fx-background-radius: 10;");
                    
                    Label details = new Label("📍 " + event.getLieu() + " | 📅 " + (event.getDateEvent() != null ? dateFormat.format(event.getDateEvent()) : "N/A"));
                    details.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
                    
                    content.getChildren().addAll(type, title, details);
                    card.getChildren().addAll(imgView, content);
                    container.getChildren().add(card);
                }
                ScrollPane scrollPane = new ScrollPane(container);
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefViewportHeight(400);
                scrollPane.setPrefViewportWidth(450);
                dialogPane.setContent(scrollPane);
            }
            dialog.showAndWait();
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible de charger les événements: " + ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
