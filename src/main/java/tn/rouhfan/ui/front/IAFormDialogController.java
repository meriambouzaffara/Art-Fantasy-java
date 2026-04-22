package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreIAService;
import tn.rouhfan.tools.SessionManager;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

public class IAFormDialogController {

    @FXML private Label stepLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox step1, step2, step3, step4, step5, previewStep;
    @FXML private VBox typeContainer, styleContainer, colorContainer, moodContainer;
    @FXML private HBox navigationBar;
    @FXML private Button backBtn, nextBtn;
    @FXML private ImageView previewImage;
    @FXML private StackPane loadingOverlay;
    
    @FXML private TextField elementsField;
    @FXML private TextField titleField;
    @FXML private ComboBox<Categorie> categoryCombo;

    private int currentStep = 1;
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ToggleGroup styleGroup = new ToggleGroup();
    private final ToggleGroup colorGroup = new ToggleGroup();
    private final ToggleGroup moodGroup = new ToggleGroup();

    private boolean generated = false;
    private InputStream lastGeneratedStream;
    private String lastPrompt;

    private OeuvreIAService oeuvreIAService = new OeuvreIAService();
    private CategorieService categorieService = new CategorieService();

    @FXML
    public void initialize() {
        setupOptions();
        loadCategories();
        updateStepVisibility();
    }

    private void setupOptions() {
        addRadioOptions(typeContainer, typeGroup, "Tableau classique", "Art numérique abstrait", "Sculpture 3D", "Paysage fantastique", "Portrait stylisé");
        addRadioOptions(styleContainer, styleGroup, "Impressionnisme", "Art cyberpunk", "Style manga/anime", "Art baroque moderne", "Steampunk", "Minimaliste");
        addRadioOptions(colorContainer, colorGroup, "Tons chauds (rouge, orange, or)", "Tons froids (bleu, violet, argent)", "Monochrome élégant", "Couleurs néon vives", "Pastel doux");
        addRadioOptions(moodContainer, moodGroup, "Mystérieuse", "Joyeuse", "Mélancolique", "Énergique", "Paisible");
    }

    private void addRadioOptions(VBox container, ToggleGroup group, String... options) {
        for (String opt : options) {
            RadioButton rb = new RadioButton(opt);
            rb.setToggleGroup(group);
            rb.getStyleClass().add("filter-label");
            rb.setStyle("-fx-font-size: 14; -fx-padding: 10; -fx-cursor: hand;");
            container.getChildren().add(rb);
        }
        if (!container.getChildren().isEmpty()) {
            ((RadioButton) container.getChildren().get(0)).setSelected(true);
        }
    }

    private void loadCategories() {
        try {
            categoryCombo.setItems(FXCollections.observableArrayList(categorieService.recuperer()));
            categoryCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
                @Override public Categorie fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep < 5) {
            currentStep++;
            updateStepVisibility();
        } else if (currentStep == 5) {
            handleGenerate();
        }
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateStepVisibility();
        }
    }

    private void updateStepVisibility() {
        step1.setVisible(currentStep == 1);
        step2.setVisible(currentStep == 2);
        step3.setVisible(currentStep == 3);
        step4.setVisible(currentStep == 4);
        step5.setVisible(currentStep == 5);
        previewStep.setVisible(currentStep == 6);

        stepLabel.setText("Étape " + Math.min(currentStep, 5) + " sur 5");
        progressBar.setProgress(currentStep / 5.0);

        backBtn.setVisible(currentStep > 1 && currentStep <= 5);
        nextBtn.setText(currentStep == 5 ? "✨ Générer l'aperçu" : "Suivant →");
        
        navigationBar.setVisible(currentStep <= 5);
        navigationBar.setManaged(currentStep <= 5);
    }

    @FXML
    private void handleGenerate() {
        if (currentStep == 5 && !validateFinalStep()) return;

        currentStep = 6;
        updateStepVisibility();
        loadingOverlay.setVisible(true);

        String type = ((RadioButton) typeGroup.getSelectedToggle()).getText();
        String style = ((RadioButton) styleGroup.getSelectedToggle()).getText();
        String color = ((RadioButton) colorGroup.getSelectedToggle()).getText();
        String mood = ((RadioButton) moodGroup.getSelectedToggle()).getText();
        String extra = elementsField.getText();

        lastPrompt = String.format("%s, %s style, %s colors, %s mood, %s, digital art, masterpiece, high quality, fantasy", 
                                   type, style, color, mood, extra);

        new Thread(() -> {
            try {
                String encodedPrompt = URLEncoder.encode(lastPrompt, "UTF-8");
                String apiUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=1024&height=1024&nologo=true&seed=" + UUID.randomUUID().hashCode();

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    // On garde le stream en mémoire pour l'affichage et l'enregistrement futur
                    byte[] imageBytes = response.body().readAllBytes();
                    Platform.runLater(() -> {
                        previewImage.setImage(new Image(new java.io.ByteArrayInputStream(imageBytes)));
                        loadingOverlay.setVisible(false);
                    });
                    this.lastGeneratedStream = new java.io.ByteArrayInputStream(imageBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur", "Génération échouée : " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        if (lastGeneratedStream == null) return;

        User user = SessionManager.getInstance().getCurrentUser();
        String title = titleField.getText();
        Categorie cat = categoryCombo.getValue();

        try {
            String fileName = "ia_" + UUID.randomUUID().toString() + ".jpg";
            Path uploadDir = Paths.get("src/main/resources/uploads/misc");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            
            Files.copy(lastGeneratedStream, uploadDir.resolve(fileName));

            OeuvreIA oeuvre = new OeuvreIA(title, "Prompt: " + lastPrompt, "uploads/misc/" + fileName, user, cat);
            oeuvreIAService.ajouter(oeuvre);

            generated = true;
            close();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'enregistrer l'œuvre : " + e.getMessage());
        }
    }

    private boolean validateFinalStep() {
        if (titleField.getText().trim().isEmpty() || categoryCombo.getValue() == null) {
            showError("Champs requis", "Veuillez donner un titre et choisir une catégorie.");
            return false;
        }
        return true;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void shareFacebook() {
        String aiImageUrl = getAIImageUrl();
        share("https://www.facebook.com/sharer/sharer.php?u=" + encode(aiImageUrl) + "&quote=" + 
              encode("Regardez ma nouvelle création IA : " + titleField.getText() + " sur Art Fantasy !"));
    }

    @FXML
    private void shareTwitter() {
        String aiImageUrl = getAIImageUrl();
        share("https://twitter.com/intent/tweet?text=" + 
              encode("Ma nouvelle création IA : " + titleField.getText() + " #ArtFantasy #AIArt") + 
              "&url=" + encode(aiImageUrl));
    }

    private String getAIImageUrl() {
        return "https://image.pollinations.ai/prompt/" + encode(lastPrompt) + "?width=1024&height=1024&nologo=true";
    }

    private void share(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return text;
        }
    }

    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) titleField.getScene().getWindow()).close(); }
    public boolean isGenerated() { return generated; }
}
