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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.ui.back.EvenementFormDialog;

import tn.rouhfan.entities.User;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.awt.Desktop;

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
        } 
        
        if ("ROLE_PARTICIPANT".equals(userRole)) {
            Button participateBtn = new Button("🎟️ Participer");
            participateBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(participateBtn, Priority.ALWAYS);
            participateBtn.setStyle("-fx-background-color: linear-gradient(to right, #00b894, #00cec9); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 12 20; -fx-font-size: 14; -fx-cursor: hand;");
            participateBtn.setOnAction(e -> handleParticipate(event));
            buttonBox.getChildren().add(participateBtn);
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
            showAlert("Participation confirmée", "✅ Vous participez maintenant à l'événement: " + event.getTitre() + "\nVotre ticket PDF va être généré !");
            
            // Generate PDF Ticket
            generatePdfTicket(event);
            
            loadEvenements(); // Refresh to show updated participant count
        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generatePdfTicket(Evenement event) {
        try {
            String currentUser = SessionManager.getInstance().isLoggedIn() ? 
                                 SessionManager.getInstance().getFullName() : "Participant";
            
            String fileName = "Ticket_Evenement_" + event.getId() + ".pdf";
            File pdfFile = new File(fileName);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Options de style
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, com.itextpdf.text.BaseColor.BLACK);
            Font fontSubTitle = FontFactory.getFont(FontFactory.HELVETICA, 16, com.itextpdf.text.BaseColor.DARK_GRAY);
            Font fontText = FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.BaseColor.BLACK);

            // Ajout du Logo (si présent dans les ressources)
            try {
                URL logoUrl = getClass().getResource("/ui/logo.png");
                if (logoUrl != null) {
                    com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(logoUrl);
                    logo.scaleToFit(150, 150);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                }
            } catch (Exception ex) {
                System.out.println("Logo non trouvé pour le PDF");
            }

            // Titre du Document
            Paragraph title = new Paragraph("TICKET DE PARTICIPATION", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Infos Événement
            document.add(new Paragraph("Événement : " + event.getTitre(), fontSubTitle));
            document.add(new Paragraph("---------------------------------------------------------"));
            document.add(new Paragraph("Lieu : " + event.getLieu(), fontText));
            String dateFormatted = event.getDateEvent() != null ? dateFormat.format(event.getDateEvent()) : "Non définie";
            document.add(new Paragraph("Date : " + dateFormatted, fontText));
            document.add(new Paragraph("Type : " + event.getType(), fontText));
            
            document.add(new Paragraph(" ", fontText)); // Espace
            document.add(new Paragraph("Détails du Participant :", fontSubTitle));
            document.add(new Paragraph("---------------------------------------------------------"));
            document.add(new Paragraph("Nom du Participant : " + currentUser, fontText));
            
            document.add(new Paragraph(" ", fontText));
            Paragraph footer = new Paragraph("Merci de votre participation ! Ce ticket est strictement personnel.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // Ouvrir le PDF généré
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                System.out.println("⚠️ Impossible d'ouvrir le fichier automatiquement. Le fichier est enregistré sous : " + pdfFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de génération du PDF : " + e.getMessage());
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

