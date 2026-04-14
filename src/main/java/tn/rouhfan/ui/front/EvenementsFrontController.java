package tn.rouhfan.ui.front;
import tn.rouhfan.tools.SessionManager;
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
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.ui.back.EvenementFormDialog;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class EvenementsFrontController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button addButton;

    private EvenementService evenementService;
    private ObservableList<Evenement> evenementsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        evenementService = new EvenementService();
        setupSortCombo();
        setupSearch();
        setupRoleBasedUI();
        loadEvenements();
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Titre (A-Z)", "Titre (Z-A)", "Date Croissante", "Date Décroissante", "Lieu (A-Z)", "Capacité");
            sortCombo.setValue("Titre (A-Z)");
            sortCombo.setOnAction(e -> handleSort());
        }
    }
    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        }
    }

    private void setupRoleBasedUI() {
        String userRole = SessionManager.getInstance().getRole();
        if ("ROLE_ARTISTE".equals(userRole) && addButton != null) {
            addButton.setVisible(true);
            addButton.setManaged(true);
        }
    }

    private void loadEvenements() {
        try {
            evenementsList = FXCollections.observableArrayList(evenementService.recuperer());
            displayCards(evenementsList);
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de charger les événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayCards(ObservableList<Evenement> events) {
        cardsContainer.getChildren().clear();
        
        for (Evenement event : events) {
            VBox card = createEventCard(event);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setPrefHeight(450);
        card.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-color: #ffffff; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 8, 0, 0, 2);");

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5;");
        
        if (event.getImage() != null && !event.getImage().isEmpty()) {
            try {
                String imageUrl = ImageUtils.getImageUrl(event.getImage());
                Image img = new Image(imageUrl);
                imageView.setImage(img);
            } catch (Exception e) {
                imageView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-font-size: 50;");
            }
        } else {
            imageView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f5f5f5;");
        }

        // Titre
        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-wrap-text: true;");

        // Description (max 2 lines)
        Label descLabel = new Label(event.getDescription() != null ? event.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666; -fx-wrap-text: true;");
        descLabel.setWrapText(true);

        // Date et Lieu
        HBox dateLocBox = new HBox(15);
        Label dateLabel = new Label("📅 " + (event.getDateEvent() != null ? dateFormat.format(event.getDateEvent()) : "N/A"));
        Label lieuLabel = new Label("📍 " + (event.getLieu() != null ? event.getLieu() : "N/A"));
        dateLabel.setStyle("-fx-font-size: 10;");
        lieuLabel.setStyle("-fx-font-size: 10;");
        dateLocBox.getChildren().addAll(dateLabel, lieuLabel);

        // Type et Capacité
        HBox typeCapBox = new HBox(15);
        Label typeLabel = new Label("🎭 " + (event.getType() != null ? event.getType() : "N/A"));
        String capaciteText = event.getCapacite() != null ? 
            event.getCapacite().toString() : "∞";
        Label capaciteLabel = new Label("👥 " + event.getNbParticipants() + "/" + capaciteText + " places");
        typeLabel.setStyle("-fx-font-size: 10;");
        capaciteLabel.setStyle("-fx-font-size: 10;");
        typeCapBox.getChildren().addAll(typeLabel, capaciteLabel);

        // Statut
        Label statutLabel = new Label(event.getStatut() != null ? "📌 " + event.getStatut() : "");
        statutLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #1976d2;");

        // Spacer
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Buttons based on user role
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        String userRole = SessionManager.getInstance().getRole();

        if ("ROLE_ARTISTE".equals(userRole)) {
            // CRUD buttons for artists
            Button editBtn = new Button("✏️ Modifier");
            editBtn.setStyle("-fx-font-size: 10; -fx-padding: 5 10; -fx-background-color: #ff9800; -fx-text-fill: white; -fx-border-radius: 3;");
            editBtn.setOnAction(e -> handleEdit(event));

            Button deleteBtn = new Button("🗑️ Supprimer");
            deleteBtn.setStyle("-fx-font-size: 10; -fx-padding: 5 10; -fx-background-color: #f44336; -fx-text-fill: white; -fx-border-radius: 3;");
            deleteBtn.setOnAction(e -> handleDelete(event));

            buttonBox.getChildren().addAll(editBtn, deleteBtn);
        } else if ("ROLE_PARTICIPANT".equals(userRole)) {
            // Participate button for participants
            Button participateBtn = new Button("🎟️ Participer");
            participateBtn.setStyle("-fx-font-size: 11; -fx-padding: 6 15; -fx-background-color: #4caf50; -fx-text-fill: white; -fx-border-radius: 5;");
            participateBtn.setPrefWidth(120);
            participateBtn.setOnAction(e -> handleParticipate(event));
            buttonBox.getChildren().add(participateBtn);
        } else {
            // View-only for others
            Label viewOnlyLabel = new Label("👁️ Consultation uniquement");
            viewOnlyLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
            buttonBox.getChildren().add(viewOnlyLabel);
        }

        card.getChildren().addAll(imageView, titleLabel, descLabel, dateLocBox, typeCapBox, statutLabel, spacer, buttonBox);
        return card;
    }

    private void handleSearch() {
        try {
            String keyword = searchField.getText();
            ObservableList<Evenement> results = FXCollections.observableArrayList(
                evenementService.rechercher(keyword)
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
        sortCombo.setValue("Titre (A-Z)");
        loadEvenements();
    }

    @FXML
    private void addEvenement(ActionEvent event) {
        EvenementFormDialog dialog = new EvenementFormDialog(null);
        dialog.show();
        
        if (dialog.isApproved()) {
            loadEvenements();
            showAlert("Succès", "✅ Événement ajouté avec succès!");
        }
    }

    private void handleParticipate(Evenement event) {
        try {
            evenementService.participer(event.getId());
            showAlert("Participation confirmée", "✅ Vous participez maintenant à l'événement: " + event.getTitre());
            loadEvenements(); // Refresh to show updated participant count
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEdit(Evenement event) {
        EvenementFormDialog dialog = new EvenementFormDialog(event);
        dialog.show();
        
        if (dialog.isApproved()) {
            loadEvenements();
            showAlert("Succès", "✅ Événement modifié avec succès!");
        }
    }

    private void handleDelete(Evenement event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer l'événement");
        confirmation.setHeaderText("Êtes-vous sûr de vouloir supprimer cet événement?");
        confirmation.setContentText("Événement: " + event.getTitre() + "\n\nCette action est irréversible.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    evenementService.supprimer(event.getId());
                    showAlert("Succès", "✅ Événement supprimé avec succès!");
                    loadEvenements();
                } catch (SQLException e) {
                    showAlert("Erreur", "❌ Erreur lors de la suppression: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleInscribe(Evenement event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Inscription");
        info.setHeaderText("Événement: " + event.getTitre());
        info.setContentText("Vous êtes intéressé par cet événement.\n\nVous pouvez vous inscrire via le bouton 'S'inscrire' principal.");
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

