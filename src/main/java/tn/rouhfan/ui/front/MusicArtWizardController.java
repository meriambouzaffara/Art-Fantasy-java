package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.AudioAnalyzerService;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreIAService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.tools.SessionManager;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MusicArtWizardController {

    @FXML private VBox step1Container;
    @FXML private VBox step2Container;
    @FXML private VBox step3Container;
    @FXML private VBox step4Container;

    @FXML private Label fileNameLabel;
    @FXML private Button startAnalysisBtn;
    @FXML private HBox visualizerBox;
    @FXML private ProgressBar analysisProgress;
    
    @FXML private ImageView resultImageView;
    @FXML private TextField titreField;
    @FXML private ComboBox<Categorie> categorieCombo;
    @FXML private Label promptLabel;

    private File selectedAudioFile;
    private AudioAnalyzerService audioAnalyzerService = new AudioAnalyzerService();
    private OeuvreIAService oeuvreIAService = new OeuvreIAService();
    private CategorieService categorieService = new CategorieService();
    
    private String generatedImageUrl;
    private String localSavedImagePath;
    private Runnable refreshCallback;
    private Timer visualizerTimer;

    @FXML
    public void initialize() {
        try {
            categorieCombo.setItems(FXCollections.observableArrayList(categorieService.recuperer()));
            categorieCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
                @Override public Categorie fromString(String s) { return null; }
            });
            if (!categorieCombo.getItems().isEmpty()) {
                categorieCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        setupVisualizer();
    }

    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    @FXML
    private void handleBrowseAudio() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier audio");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
        
        File file = fileChooser.showOpenDialog(fileNameLabel.getScene().getWindow());
        if (file != null) {
            selectedAudioFile = file;
            fileNameLabel.setText(file.getName());
            startAnalysisBtn.setDisable(false);
            
            // Pré-remplir le titre avec le nom du fichier embelli
            String name = file.getName();
            if (name.lastIndexOf('.') > 0) name = name.substring(0, name.lastIndexOf('.'));
            
            // Embellir : remplacer - et _ par des espaces, capitaliser
            name = name.replace("-", " ").replace("_", " ");
            String[] words = name.split("\\s+");
            StringBuilder beautified = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    beautified.append(Character.toUpperCase(word.charAt(0)))
                             .append(word.substring(1).toLowerCase())
                             .append(" ");
                }
            }
            titreField.setText(beautified.toString().trim());
        }
    }

    @FXML
    private void handleStartAnalysis() {
        if (selectedAudioFile == null) return;

        step1Container.setVisible(false);
        step1Container.setManaged(false);
        step2Container.setVisible(true);
        step2Container.setManaged(true);
        
        ((Stage) step2Container.getScene().getWindow()).sizeToScene();
        
        startVisualizer();

        // Lancer l'analyse en arrière-plan
        new Thread(() -> {
            audioAnalyzerService.analyze(selectedAudioFile, new AudioAnalyzerService.AnalysisCallback() {
                @Override
                public void onAnalysisComplete(String prompt) {
                    Platform.runLater(() -> {
                        stopVisualizer();
                        step2Container.setVisible(false);
                        step2Container.setManaged(false);
                        
                        promptLabel.setText(prompt);
                        generateImage(prompt);
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        stopVisualizer();
                        showAlert("Erreur d'analyse", error);
                        handleClose();
                    });
                }
            });
        }).start();
    }

    private void generateImage(String prompt) {
        step3Container.setVisible(true);
        step3Container.setManaged(true);
        ((Stage) step3Container.getScene().getWindow()).sizeToScene();

        new Thread(() -> {
            try {
                // Construction de l'URL Pollinations avec seed pour un résultat unique
                String encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8").replace("+", "%20");
                String apiUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=1024&height=1024&nologo=true&seed=" + UUID.randomUUID().hashCode();
                
                generatedImageUrl = apiUrl;
                Image image = new Image(apiUrl, true); // background loading
                
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0 && !image.isError()) {
                        Platform.runLater(() -> showResult(image));
                    }
                });

                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        Platform.runLater(() -> {
                            showAlert("Erreur", "L'image n'a pas pu être générée.");
                            handleClose();
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showResult(Image image) {
        step3Container.setVisible(false);
        step3Container.setManaged(false);
        step4Container.setVisible(true);
        step4Container.setManaged(true);
        
        resultImageView.setImage(image);
        
        // Force the window to resize to fit the large image and form
        Platform.runLater(() -> {
            Stage stage = (Stage) step4Container.getScene().getWindow();
            stage.sizeToScene();
            stage.centerOnScreen();
        });
    }

    @FXML
    private void handleSave() {
        if (titreField.getText().trim().isEmpty() || categorieCombo.getValue() == null) {
            showAlert("Erreur", "Veuillez entrer un titre et choisir une catégorie.");
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Erreur", "Vous devez être connecté.");
            return;
        }

        // Télécharger l'image générée localement avant d'enregistrer
        new Thread(() -> {
            try {
                URL url = new URL(generatedImageUrl);
                String fileName = "music_ia_" + System.currentTimeMillis() + ".jpg";
                String uploadDir = ImageUtils.UPLOADS_DIR + "/oeuvres_ia";
                
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                
                Path targetPath = Paths.get(uploadDir, fileName);
                
                try (InputStream in = url.openStream()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Enregistrer dans la base de données
                OeuvreIA o = new OeuvreIA();
                o.setTitre(titreField.getText().trim());
                o.setDescription("Généré à partir de la musique : " + selectedAudioFile.getName() + "\n\nPrompt : " + promptLabel.getText());
                o.setImage("oeuvres_ia/" + fileName);
                o.setCategorie(categorieCombo.getValue());
                o.setUser(currentUser);
                
                oeuvreIAService.ajouter(o);
                
                Platform.runLater(() -> {
                    if (refreshCallback != null) refreshCallback.run();
                    handleClose();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur de sauvegarde", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancelAnalysis() {
        audioAnalyzerService.stop();
        stopVisualizer();
        handleClose();
    }

    @FXML
    private void handleClose() {
        audioAnalyzerService.stop();
        stopVisualizer();
        ((Stage) step1Container.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Visualizer Faux Animation ---
    private Rectangle[] bars = new Rectangle[15];
    private Random random = new Random();

    private void setupVisualizer() {
        for (int i = 0; i < bars.length; i++) {
            Rectangle r = new Rectangle(8, 5);
            r.setStyle("-fx-fill: linear-gradient(to top, #4c1d95, #db2777); -fx-arc-width: 8; -fx-arc-height: 8;");
            bars[i] = r;
            visualizerBox.getChildren().add(r);
        }
    }

    private void startVisualizer() {
        visualizerTimer = new Timer(true);
        visualizerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    for (Rectangle r : bars) {
                        r.setHeight(10 + random.nextInt(40));
                    }
                    // Update progress bar a bit randomly to simulate progress
                    double current = analysisProgress.getProgress();
                    if (current < 0) current = 0;
                    if (current < 0.95) {
                        analysisProgress.setProgress(current + 0.015);
                    }
                });
            }
        }, 0, 100);
    }

    private void stopVisualizer() {
        if (visualizerTimer != null) {
            visualizerTimer.cancel();
        }
        for (Rectangle r : bars) {
            r.setHeight(5);
        }
    }
}
