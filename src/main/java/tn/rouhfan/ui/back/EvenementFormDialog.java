package tn.rouhfan.ui.back;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.services.EvenementService;
import tn.rouhfan.services.SponsorService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class EvenementFormDialog {

    private Stage stage;
    private Evenement evenement;
    private EvenementService evenementService;
    private SponsorService sponsorService;
    private boolean approved = false;

    // Contrôles du formulaire
    private TextField titreField;
    private TextArea descriptionArea;
    private TextField imageField;
    private TextField typeField;
    private ComboBox<String> statutCombo;
    private DatePicker dateEvenementPicker;
    private TextField lieuField;
    private Spinner<Integer> capaciteSpinner;
    private Spinner<Integer> participantsSpinner;
    private TextField googleEventIdField;
    private ComboBox<Sponsor> sponsorCombo;
    private Label errorLabel;

    public EvenementFormDialog(Evenement event) {
        this.evenement = event;
        this.evenementService = new EvenementService();
        this.sponsorService = new SponsorService();
        initUI();
    }

    private void initUI() {
        stage = new Stage();
        stage.setTitle(evenement == null || evenement.getId() == 0 ? "Ajouter un événement" : "Modifier l'événement");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(600);
        stage.setHeight(700);

        // Form Content
        GridPane grid = createFormGrid();
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setStyle("-fx-alignment: center-right;");

        Button saveBtn = new Button("💾 Enregistrer");
        saveBtn.setStyle("-fx-font-size: 12; -fx-padding: 8 20;");
        saveBtn.setOnAction(e -> handleSave());

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-font-size: 12; -fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        // Main Layout
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10));
        mainBox.getChildren().addAll(grid, buttonBox);

        Scene scene = new Scene(mainBox);
        stage.setScene(scene);
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-border-width: 1;");

        // Titre
        titreField = new TextField();
        titreField.setPromptText("Ex: Concert Jazz");
        grid.add(createLabel("Titre *"), 0, 0);
        grid.add(titreField, 1, 0);

        // Description
        descriptionArea = new TextArea();
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPromptText("Description de l'événement");
        grid.add(createLabel("Description"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        // Image URL
        imageField = new TextField();
        imageField.setPromptText("URL de l'image");
        grid.add(createLabel("Image"), 0, 2);
        grid.add(imageField, 1, 2);

        // Type
        typeField = new TextField();
        typeField.setPromptText("Ex: CONCERT, EXPOSITION");
        grid.add(createLabel("Type"), 0, 3);
        grid.add(typeField, 1, 3);

        // Statut
        statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("PLANIFIÉ", "EN COURS", "ANNULÉ", "TERMINÉ");
        grid.add(createLabel("Statut *"), 0, 4);
        grid.add(statutCombo, 1, 4);

        // Date
        dateEvenementPicker = new DatePicker();
        grid.add(createLabel("Date *"), 0, 5);
        grid.add(dateEvenementPicker, 1, 5);

        // Lieu
        lieuField = new TextField();
        lieuField.setPromptText("Ex: Palais des congrès");
        grid.add(createLabel("Lieu *"), 0, 6);
        grid.add(lieuField, 1, 6);

        // Capacité
        capaciteSpinner = new Spinner<>(1, 10000, 100, 10);
        capaciteSpinner.setEditable(true);
        grid.add(createLabel("Capacité"), 0, 7);
        grid.add(capaciteSpinner, 1, 7);

        // Participants
        participantsSpinner = new Spinner<>(0, 10000, 0, 1);
        participantsSpinner.setEditable(true);
        grid.add(createLabel("Participants"), 0, 8);
        grid.add(participantsSpinner, 1, 8);

        // Google Event ID
        googleEventIdField = new TextField();
        googleEventIdField.setPromptText("ID Google Calendar (optionnel)");
        grid.add(createLabel("Google Event ID"), 0, 9);
        grid.add(googleEventIdField, 1, 9);

        // Sponsor
        sponsorCombo = new ComboBox<>();
        grid.add(createLabel("Sponsor"), 0, 10);
        grid.add(sponsorCombo, 1, 10);

        // Load sponsors
        try {
            List<Sponsor> sponsors = sponsorService.recuperer();
            sponsorCombo.getItems().addAll(sponsors);
        } catch (SQLException e) {
            System.err.println("Erreur chargement sponsors: " + e.getMessage());
        }

        // Error message
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
        grid.add(errorLabel, 0, 11, 2, 1);

        // Populate if editing
        if (evenement != null && evenement.getId() > 0) {
            populateFields();
        }

        GridPane.setColumnSpan(descriptionArea, 1);
        GridPane.setHgrow(titreField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(descriptionArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(lieuField, javafx.scene.layout.Priority.ALWAYS);

        return grid;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        return label;
    }

    private void populateFields() {
        titreField.setText(evenement.getTitre() != null ? evenement.getTitre() : "");
        descriptionArea.setText(evenement.getDescription() != null ? evenement.getDescription() : "");
        imageField.setText(evenement.getImage() != null ? evenement.getImage() : "");
        typeField.setText(evenement.getType() != null ? evenement.getType() : "");
        statutCombo.setValue(evenement.getStatut() != null ? evenement.getStatut() : "PLANIFIÉ");

        if (evenement.getDateEvent() != null) {
            java.time.LocalDate ld = evenement.getDateEvent().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            dateEvenementPicker.setValue(ld);
        }

        lieuField.setText(evenement.getLieu() != null ? evenement.getLieu() : "");
        capaciteSpinner.getValueFactory().setValue(evenement.getCapacite() != null ? evenement.getCapacite() : 100);
        participantsSpinner.getValueFactory().setValue(evenement.getNbParticipants());
        googleEventIdField.setText(evenement.getGoogleEventId() != null ? evenement.getGoogleEventId() : "");

        if (evenement.getSponsor() != null) {
            sponsorCombo.setValue(evenement.getSponsor());
        }
    }

    private void handleSave() {
        try {
            errorLabel.setText("");

            // Collect data
            if (evenement == null) {
                evenement = new Evenement();
            }

            evenement.setTitre(titreField.getText());
            evenement.setDescription(descriptionArea.getText());
            evenement.setImage(imageField.getText());
            evenement.setType(typeField.getText());
            evenement.setStatut(statutCombo.getValue() != null ? statutCombo.getValue() : "PLANIFIÉ");

            if (dateEvenementPicker.getValue() != null) {
                java.time.LocalDate ld = dateEvenementPicker.getValue();
                evenement.setDateEvent(java.sql.Date.valueOf(ld));
            }

            evenement.setLieu(lieuField.getText());
            evenement.setCapacite(capaciteSpinner.getValue());
            evenement.setNbParticipants(participantsSpinner.getValue());
            evenement.setGoogleEventId(googleEventIdField.getText());
            evenement.setSponsor(sponsorCombo.getValue());

            // Validate and save
            evenementService.valider(evenement);

            if (evenement.getId() > 0) {
                evenementService.modifier(evenement);
            } else {
                evenementService.ajouter(evenement);
            }

            approved = true;
            stage.close();

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (SQLException ex) {
            errorLabel.setText("❌ Erreur BD: " + ex.getMessage());
        }
    }

    public void show() {
        stage.showAndWait();
    }

    public boolean isApproved() {
        return approved;
    }

    public Evenement getEvenement() {
        return evenement;
    }
}
