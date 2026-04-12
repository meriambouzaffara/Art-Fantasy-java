package tn.rouhfan.ui.back;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.services.SponsorService;

import java.sql.SQLException;

public class SponsorFormDialog {

    private Stage stage;
    private Sponsor sponsor;
    private SponsorService sponsorService;
    private boolean approved = false;

    // Contrôles du formulaire
    private TextField nomField;
    private TextField logoField;
    private TextArea descriptionArea;
    private TextField emailField;
    private TextField telField;
    private TextArea adresseArea;
    private Label errorLabel;

    public SponsorFormDialog(Sponsor s) {
        this.sponsor = s;
        this.sponsorService = new SponsorService();
        initUI();
    }

    private void initUI() {
        stage = new Stage();
        stage.setTitle(sponsor == null || sponsor.getId() == 0 ? "Ajouter un sponsor" : "Modifier le sponsor");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(550);
        stage.setHeight(650);

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

        // Nom
        nomField = new TextField();
        nomField.setPromptText("Ex: Artsy Company");
        grid.add(createLabel("Nom *"), 0, 0);
        grid.add(nomField, 1, 0);

        // Logo URL
        logoField = new TextField();
        logoField.setPromptText("URL du logo");
        grid.add(createLabel("Logo"), 0, 1);
        grid.add(logoField, 1, 1);

        // Description
        descriptionArea = new TextArea();
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPromptText("Description du sponsor");
        grid.add(createLabel("Description"), 0, 2);
        grid.add(descriptionArea, 1, 2);

        // Email
        emailField = new TextField();
        emailField.setPromptText("sponsor@example.com");
        grid.add(createLabel("Email *"), 0, 3);
        grid.add(emailField, 1, 3);

        // Téléphone
        telField = new TextField();
        telField.setPromptText("Ex: +216 71 123 456");
        grid.add(createLabel("Téléphone *"), 0, 4);
        grid.add(telField, 1, 4);

        // Adresse
        adresseArea = new TextArea();
        adresseArea.setWrapText(true);
        adresseArea.setPrefRowCount(3);
        adresseArea.setPromptText("Adresse complète");
        grid.add(createLabel("Adresse *"), 0, 5);
        grid.add(adresseArea, 1, 5);

        // Error message
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
        grid.add(errorLabel, 0, 6, 2, 1);

        // Populate if editing
        if (sponsor != null && sponsor.getId() > 0) {
            populateFields();
        }

        GridPane.setColumnSpan(descriptionArea, 1);
        GridPane.setColumnSpan(adresseArea, 1);
        GridPane.setHgrow(nomField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(descriptionArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(adresseArea, javafx.scene.layout.Priority.ALWAYS);

        return grid;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        return label;
    }

    private void populateFields() {
        nomField.setText(sponsor.getNom() != null ? sponsor.getNom() : "");
        logoField.setText(sponsor.getLogo() != null ? sponsor.getLogo() : "");
        descriptionArea.setText(sponsor.getDescription() != null ? sponsor.getDescription() : "");
        emailField.setText(sponsor.getEmail() != null ? sponsor.getEmail() : "");
        telField.setText(sponsor.getTel() != null ? sponsor.getTel() : "");
        adresseArea.setText(sponsor.getAdresse() != null ? sponsor.getAdresse() : "");
    }

    private void handleSave() {
        try {
            errorLabel.setText("");

            // Collect data
            if (sponsor == null) {
                sponsor = new Sponsor();
            }

            sponsor.setNom(nomField.getText());
            sponsor.setLogo(logoField.getText());
            sponsor.setDescription(descriptionArea.getText());
            sponsor.setEmail(emailField.getText());
            sponsor.setTel(telField.getText());
            sponsor.setAdresse(adresseArea.getText());

            // Validate and save
            sponsorService.valider(sponsor);

            if (sponsor.getId() > 0) {
                sponsorService.modifier(sponsor);
            } else {
                sponsorService.ajouter(sponsor);
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

    public Sponsor getSponsor() {
        return sponsor;
    }
}
