package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.services.GroqAiService;
import tn.rouhfan.services.TicketEmailService;
import tn.rouhfan.services.TicketPdfGenerator;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.back.EvenementFormDialog;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class EvenementsFrontController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button addButton;
    
    @FXML private VBox recommendationsBox;
    @FXML private FlowPane recommendationsPane;

    private List<GroqAiService.AiRecommendationItem> currentAiItems;
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
            sortCombo.getItems().addAll("Titre (A-Z)", "Titre (Z-A)", "Date Croissante", "Date Décroissante", "Lieu (A-Z)", "Capacité", "Statut");
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
            loadRecommendations();
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
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.getStyleClass().add("card");

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("card-image-container");
        imageContainer.setPrefHeight(220.0);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);

        if (event.getImage() != null && !event.getImage().isEmpty()) {
            try {
                String imageUrl = ImageUtils.getImageUrl(event.getImage());
                Image img = new Image(imageUrl);
                imageView.setImage(img);
            } catch (Exception e) {}
        }
        imageContainer.getChildren().add(imageView);

        // Content Container
        VBox contentBox = new VBox(15);
        contentBox.getStyleClass().add("card-content");

        // Sous-conteneur haut (Titre + Auteur)
        VBox headerBox = new VBox(5);

        Label titleLabel = new Label(event.getTitre());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        String creatorName = "Inconnu";
        if (event.getCreateur() != null) {
            String pr = event.getCreateur().getPrenom();
            String no = event.getCreateur().getNom();
            if (pr != null && no != null && !pr.trim().isEmpty() && !no.trim().isEmpty()) {
                creatorName = pr + " " + no;
            } else {
                creatorName = "Administrateur";
            }
        }
        Label creatorLabel = new Label("👤 Créé par: " + creatorName);
        creatorLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        headerBox.getChildren().addAll(titleLabel, creatorLabel);
        
        if (event.getSponsor() != null && event.getSponsor().getNom() != null) {
            Label sponsorLabel = new Label("🤝 Sponsor: " + event.getSponsor().getNom());
            sponsorLabel.setStyle("-fx-text-fill: #16a085; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 2 0 0 0;");
            headerBox.getChildren().add(sponsorLabel);
        }

        // Informations Event (Date/Lieu/Type)
        VBox infoBox = new VBox(5);
        Label dateLabel = new Label("📅 " + (event.getDateEvent() != null ? dateFormat.format(event.getDateEvent()) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #6c2a90; -fx-font-weight: bold; -fx-font-size: 13;");

        Label lieuLabel = new Label("📍 " + (event.getLieu() != null ? event.getLieu() : "N/A"));
        lieuLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        String capaciteText = event.getCapacite() != null ? event.getCapacite().toString() : "∞";
        Label capaciteLabel = new Label("👥 " + event.getNbParticipants() + "/" + capaciteText + " places");
        capaciteLabel.setStyle("-fx-text-fill: #c9a849; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(dateLabel, lieuLabel, capaciteLabel);

        // Statut
        Label statutLabel = new Label(event.getStatut() != null ? event.getStatut() : "");
        statutLabel.setStyle("-fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 11; -fx-font-weight: bold; -fx-text-transform: uppercase; -fx-background-color: #e0f2f1; -fx-text-fill: #009688;");

        HBox statusBox = new HBox();
        statusBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        statusBox.getChildren().add(statutLabel);

        // Region pour pousser les boutons vers le bas
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        String userRole = SessionManager.getInstance().getRole();

        if ("ROLE_ARTISTE".equals(userRole) || "ROLE_ADMIN".equals(userRole)) {
            boolean isCreator = false;
            if (SessionManager.getInstance().getCurrentUser() != null &&
                    event.getCreateurId() == SessionManager.getInstance().getCurrentUser().getId()) {
                isCreator = true;
            }

            if (isCreator || "ROLE_ADMIN".equals(userRole)) {
                Button editBtn = new Button("✏️ Modifier");
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(editBtn, Priority.ALWAYS);
                editBtn.setOnAction(e -> handleEdit(event));

                Button deleteBtn = new Button("🗑️");
                deleteBtn.getStyleClass().add("btn-supprimer-table");
                deleteBtn.setOnAction(e -> handleDelete(event));

                buttonBox.getChildren().addAll(editBtn, deleteBtn);
            }
            
            if (isCreator) {
                Button shareBtn = new Button("🔵 Partager");
                shareBtn.setStyle("-fx-background-color: #1877F2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                shareBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(shareBtn, Priority.ALWAYS);
                shareBtn.setOnAction(e -> handleShareFacebook(event));
                buttonBox.getChildren().add(shareBtn);
            }
        }

        if ("ROLE_PARTICIPANT".equals(userRole)) {
            if ("TERMINÉ".equals(event.getStatut())) {
                Label termineLabel = new Label("Événement terminé");
                termineLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-color: #fce4e4; -fx-background-radius: 5;");
                buttonBox.getChildren().add(termineLabel);
            } else {
                Button participateBtn = new Button("🎟️ Participer");
                participateBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(participateBtn, Priority.ALWAYS);
                participateBtn.setStyle("-fx-background-color: linear-gradient(to right, #00b894, #00cec9); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 12 20; -fx-font-size: 14; -fx-cursor: hand;");
                participateBtn.setOnAction(e -> handleParticipate(event));
                buttonBox.getChildren().add(participateBtn);
            }
        }

        if (buttonBox.getChildren().isEmpty()) {
            Label viewOnlyLabel = new Label("👁️ Consultation");
            viewOnlyLabel.setStyle("-fx-text-fill: #999;");
            buttonBox.getChildren().add(viewOnlyLabel);
        }

        contentBox.getChildren().addAll(headerBox, infoBox, statusBox, spacer, buttonBox);
        card.getChildren().addAll(imageContainer, contentBox);
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
                case "Statut":
                    results = FXCollections.observableArrayList(evenementService.rechercher(keyword));
                    results.sort((e1, e2) -> e1.getStatut().compareToIgnoreCase(e2.getStatut()));
                    break;
            }

            if (results != null) {
                displayCards(results);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors du tri: " + e.getMessage());
        }
    }

    private void loadRecommendations() {
        String userRole = SessionManager.getInstance().getRole();
        if (!"ROLE_PARTICIPANT".equals(userRole) || evenementsList == null || evenementsList.isEmpty()) {
            if (recommendationsBox != null) {
                recommendationsBox.setVisible(false);
                recommendationsBox.setManaged(false);
            }
            return;
        }
        
        if (recommendationsBox != null) {
            recommendationsBox.setVisible(true);
            recommendationsBox.setManaged(true);
            Label loadingLbl = new Label("⏳ Analyse de l'IA en cours...");
            loadingLbl.setStyle("-fx-text-fill: #9b59b6; -fx-font-style: italic;");
            recommendationsPane.getChildren().setAll(loadingLbl);
        }
        
        new Thread(() -> {
            List<Evenement> eventsForAi = new java.util.ArrayList<>(evenementsList);
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    List<Evenement> myHistory = evenementService.getHistoriqueParticipations(currentUser.getId());
                    List<Integer> historyIds = new java.util.ArrayList<>();
                    for (Evenement e : myHistory) historyIds.add(e.getId());
                    eventsForAi.removeIf(e -> historyIds.contains(e.getId()));
                }
                
                // Exclure les événements terminés des recommandations
                eventsForAi.removeIf(e -> "TERMINÉ".equals(e.getStatut()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            GroqAiService aiService = new GroqAiService();
            GroqAiService.AiRecommendationResult result = aiService.getRecommendations(eventsForAi, 3);
            
            Platform.runLater(() -> {
                if (recommendationsPane == null) return;
                recommendationsPane.getChildren().clear();
                
                if (result.getItems().isEmpty()) {
                     Label emptyLbl = new Label("Désolé, aucune recommandation n'a pu être trouvée.");
                     recommendationsPane.getChildren().add(emptyLbl);
                     return;
                }
                
                this.currentAiItems = result.getItems();
                
                for (GroqAiService.AiRecommendationItem item : result.getItems()) {
                    Evenement evt = item.getEntity();
                    VBox rootCard = createEventCard(evt);
                    recommendationsPane.getChildren().add(rootCard);
                }
            });
        }).start();
    }
    
    @FXML
    private void handleViewAnalysis(ActionEvent event) {
        if (currentAiItems == null || currentAiItems.isEmpty()) return;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("✨ Pourquoi ces choix ?");
        dialog.setHeaderText("Analyse de l'Intelligence Artificielle");
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        
        VBox container = new VBox(15);
        container.setPadding(new Insets(15));
        
        for (GroqAiService.AiRecommendationItem item : currentAiItems) {
            Evenement evt = item.getEntity();
            
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
            
            Label titleLbl = new Label("🎯 " + evt.getTitre() + " (" + evt.getType() + ")");
            titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50;");
            
            Label aiVisionLbl = new Label("👁️ Vision de l'IA:\n" + item.getAiVision());
            aiVisionLbl.setWrapText(true);
            aiVisionLbl.setStyle("-fx-text-fill: #8e44ad; -fx-font-style: italic; -fx-font-size: 12;");
            
            Label reasonLbl = new Label("🤝 Pourquoi pour vous ?\n" + item.getReason());
            reasonLbl.setWrapText(true);
            reasonLbl.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12;");
            
            card.getChildren().addAll(titleLbl, aiVisionLbl, reasonLbl);
            container.getChildren().add(card);
        }
        
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setPrefViewportWidth(450);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        dialogPane.setContent(scrollPane);
        dialog.showAndWait();
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
            showAlert("Participation confirmée", "✅ Vous participez maintenant à l'événement: " + event.getTitre() + "\nVotre ticket PDF va être généré et envoyé par email !");

            // Generate PDF Ticket and send email
            generatePdfTicketAndEmail(event);

            loadEvenements(); // Refresh to show updated participant count
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generatePdfTicketAndEmail(Evenement event) {
        try {
            String currentUser = SessionManager.getInstance().isLoggedIn() ?
                    SessionManager.getInstance().getFullName() : "Participant";
            String userEmail = SessionManager.getInstance().isLoggedIn() ?
                    SessionManager.getInstance().getCurrentUser().getEmail() : null;

            TicketPdfGenerator pdfGenerator = new TicketPdfGenerator();
            File pdfFile = pdfGenerator.generateTicket(event, currentUser);

            // Ouvrir le PDF généré
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                System.out.println("⚠️ Impossible d'ouvrir le fichier automatiquement. Le fichier est enregistré sous : " + pdfFile.getAbsolutePath());
            }

            // Envoyer l'email
            if (userEmail != null && !userEmail.isEmpty()) {
                TicketEmailService emailService = new TicketEmailService();
                emailService.sendTicket(userEmail, event.getTitre(), pdfFile);
                System.out.println("✅ Email envoyé à " + userEmail);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération ou l'envoi du ticket : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleShareFacebook(Evenement event) {
        try {
            // URL fictive de l'événement basée sur l'ID (à adapter selon le vrai domaine si existant)
            String eventUrl = "http://rouhelfann.tn/evenements/" + event.getId();
            // Facebook sharer URL
            String facebookShareUrl = "https://www.facebook.com/sharer/sharer.php?u=" + java.net.URLEncoder.encode(eventUrl, "UTF-8");
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new java.net.URI(facebookShareUrl));
            } else {
                System.out.println("⚠️ Impossible d'ouvrir le navigateur web.");
            }
        } catch (Exception e) {
            showAlert("Erreur", "❌ Impossible d'ouvrir la page de partage Facebook: " + e.getMessage());
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
