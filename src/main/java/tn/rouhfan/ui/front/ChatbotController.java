package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.rouhfan.services.ActivityLogService;
import tn.rouhfan.services.HuggingFaceService;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.SessionManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur du chatbot intégré avec mode IA Hugging Face.
 *
 * Deux modes de fonctionnement :
 * 1. Mode FAQ (par défaut) : réponses pré-définies rapides
 * 2. Mode IA : génération de texte via Hugging Face API
 *
 * Le basculement se fait via le bouton "🤖 Mode IA" dans l'interface.
 */
public class ChatbotController {

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField messageInput;
    @FXML private Button closeButton;
    @FXML private Button aiToggleButton;
    @FXML private Label modeLabel;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<String, String> responses = new HashMap<>();

    /** Service d'intégration Hugging Face */
    private final HuggingFaceService huggingFaceService = new HuggingFaceService();

    /** Service de logging des activités */
    private final ActivityLogService activityLogService = new ActivityLogService();

    /** Mode IA activé ou non */
    private boolean aiModeEnabled = false;

    /** Indicateur de chargement affiché pendant les appels API */
    private ProgressIndicator loadingIndicator;

    @FXML
    public void initialize() {
        setupResponses();
        addBotMessage("Bonjour ! 👋 Je suis l'assistant Rouh el Fann.\n"
                + "Comment puis-je vous aider ?\n\n"
                + "💡 Activez le Mode IA pour des réponses intelligentes !");

        // Mettre à jour l'état du bouton IA
        updateAiToggleState();
    }

    private void setupResponses() {
        // Mots de passe
        responses.put("reset password", "🔑 Pour réinitialiser votre mot de passe :\n"
                + "1. Allez sur la page de connexion\n"
                + "2. Cliquez sur \"Mot de passe oublié\"\n"
                + "3. Entrez votre email\n"
                + "4. Saisissez le code reçu par email\n"
                + "5. Choisissez un nouveau mot de passe");
        responses.put("mot de passe", responses.get("reset password"));
        responses.put("password", responses.get("reset password"));
        responses.put("mdp", responses.get("reset password"));
        responses.put("oublié", responses.get("reset password"));

        // Création de compte
        responses.put("create account", "✨ Pour créer un compte :\n"
                + "1. Cliquez sur \"S'inscrire\"\n"
                + "2. Remplissez vos informations\n"
                + "3. Choisissez Artiste ou Participant\n"
                + "4. Vérifiez votre email avec le code reçu\n"
                + "5. Connectez-vous !");
        responses.put("créer compte", responses.get("create account"));
        responses.put("inscription", responses.get("create account"));
        responses.put("inscrire", responses.get("create account"));
        responses.put("signup", responses.get("create account"));
        responses.put("register", responses.get("create account"));

        // Aide
        responses.put("help", "❓ Voici ce que je peux faire :\n"
                + "• 🔑 \"reset password\" — Réinitialiser le mot de passe\n"
                + "• ✨ \"créer compte\" — Créer un nouveau compte\n"
                + "• 📧 \"email\" — Problèmes d'email\n"
                + "• 👤 \"profil\" — Modifier le profil\n"
                + "• 🎨 \"rôles\" — Infos sur les rôles\n"
                + "• 📊 \"stats\" — Statistiques\n"
                + "• 🤖 Activez le Mode IA pour des réponses libres !");
        responses.put("aide", responses.get("help"));
        responses.put("bonjour", "Bonjour ! 😊 Comment puis-je vous aider ?\n"
                + "Tapez \"aide\" pour voir les options.");
        responses.put("salut", responses.get("bonjour"));
        responses.put("hi", responses.get("bonjour"));

        // Email
        responses.put("email", "📧 Problèmes d'email ?\n"
                + "• Vérifiez vos spams\n"
                + "• L'email peut prendre quelques minutes\n"
                + "• Cliquez \"Renvoyer\" sur la page de vérification\n"
                + "• Contactez l'admin si le problème persiste");
        responses.put("mail", responses.get("email"));
        responses.put("vérification", responses.get("email"));

        // Profil
        responses.put("profil", "👤 Pour modifier votre profil :\n"
                + "1. Connectez-vous\n"
                + "2. Allez dans \"Mon Profil\"\n"
                + "3. Modifiez nom, prénom ou email\n"
                + "4. Cliquez \"Enregistrer\"");
        responses.put("profile", responses.get("profil"));
        responses.put("modifier", responses.get("profil"));

        // Rôles
        responses.put("rôles", "🎭 Les rôles disponibles :\n"
                + "• 👑 Admin — Gestion complète\n"
                + "• 🎨 Artiste — Créer des œuvres/cours\n"
                + "• 🎭 Participant — Explorer et apprendre");
        responses.put("roles", responses.get("rôles"));
        responses.put("role", responses.get("rôles"));

        // Stats
        responses.put("stats", "📊 Les statistiques sont dans le Dashboard Admin.\n"
                + "Vous y verrez : nombre d'utilisateurs, actifs, dernières connexions.");
        responses.put("statistiques", responses.get("stats"));
    }

    // ═══════════════════════════════════════
    //  Gestion de l'envoi de messages
    // ═══════════════════════════════════════

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String msg = messageInput.getText().trim();
        if (msg.isEmpty()) return;

        addUserMessage(msg);
        messageInput.clear();

        if (aiModeEnabled) {
            // ── Mode IA : appel Hugging Face ──
            handleAiMessage(msg);
        } else {
            // ── Mode FAQ : réponse pré-définie ──
            String response = findResponse(msg.toLowerCase());
            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> addBotMessage(response));
            }).start();
        }
    }

    /**
     * Traite un message en mode IA via Hugging Face.
     * Affiche un indicateur de chargement pendant l'appel API.
     */
    private void handleAiMessage(String userMessage) {
        // ── Afficher l'indicateur de chargement ──
        showLoadingIndicator();

        // ── Récupérer l'ID utilisateur pour le logging ──
        int userId = 0;
        if (SessionManager.getInstance().isLoggedIn()) {
            userId = SessionManager.getInstance().getCurrentUser().getId();
        }
        final int finalUserId = userId;

        // ── Appel API dans un thread séparé (ne pas bloquer l'UI) ──
        new Thread(() -> {
            try {
                String aiResponse = huggingFaceService.generateText(userMessage, finalUserId);

                Platform.runLater(() -> {
                    hideLoadingIndicator();
                    addBotMessage("🤖 " + aiResponse);
                });

            } catch (HuggingFaceService.HuggingFaceException e) {
                AppLogger.error("[Chatbot] Erreur Hugging Face: " + e.getMessage());
                activityLogService.logError(finalUserId, "CHATBOT_ERROR", e.getMessage());

                Platform.runLater(() -> {
                    hideLoadingIndicator();
                    addBotMessage("⚠️ Désolé, je n'ai pas pu obtenir une réponse IA.\n"
                            + "Erreur : " + e.getMessage() + "\n\n"
                            + "💡 Essayez en mode FAQ ou réessayez plus tard.");
                });
            }
        }).start();
    }

    private String findResponse(String input) {
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (aiModeEnabled) {
            return "🤖 Traitement de votre demande via l'IA...";
        }
        return "🤔 Je ne comprends pas votre demande.\n"
                + "Tapez \"aide\" pour voir ce que je peux faire.\n"
                + "💡 Ou activez le Mode IA pour des réponses libres !";
    }

    // ═══════════════════════════════════════
    //  Toggle Mode IA
    // ═══════════════════════════════════════

    /**
     * Bascule entre le mode FAQ et le mode IA (Hugging Face).
     */
    @FXML
    private void toggleAiMode(ActionEvent event) {
        aiModeEnabled = !aiModeEnabled;
        updateAiToggleState();

        if (aiModeEnabled) {
            if (!huggingFaceService.isConfigured()) {
                addBotMessage("⚠️ La clé API Hugging Face n'est pas configurée.\n"
                        + "Veuillez ajouter HUGGINGFACE_API_KEY dans le fichier .env");
                aiModeEnabled = false;
                updateAiToggleState();
                return;
            }
            addBotMessage("🤖 Mode IA activé !\n"
                    + "Je suis maintenant connecté à Hugging Face.\n"
                    + "Posez-moi n'importe quelle question !");

            // Log l'activation du mode IA
            int userId = SessionManager.getInstance().isLoggedIn()
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0;
            activityLogService.log(userId, "CHATBOT_AI_ENABLED", "Mode IA activé");
        } else {
            addBotMessage("📋 Mode FAQ activé.\n"
                    + "Je réponds avec des réponses pré-définies.\n"
                    + "Tapez \"aide\" pour voir les options.");
        }
    }

    /**
     * Met à jour l'apparence du bouton de toggle IA.
     */
    private void updateAiToggleState() {
        if (aiToggleButton != null) {
            aiToggleButton.getStyleClass().remove("chat-quick-btn-active");
            if (aiModeEnabled) {
                aiToggleButton.setText("🧠 Mode IA ON");
                aiToggleButton.getStyleClass().add("chat-quick-btn-active");
                aiToggleButton.setStyle(""); // Vider le style inline
            } else {
                aiToggleButton.setText("🤖 Mode IA");
                aiToggleButton.setStyle(""); // Vider le style inline
            }
        }
        if (modeLabel != null) {
            modeLabel.setText(aiModeEnabled ? "Mode IA • Hugging Face" : "En ligne • Réponse instantanée");
        }
    }

    // ═══════════════════════════════════════
    //  Indicateur de chargement
    // ═══════════════════════════════════════

    /**
     * Affiche un indicateur de chargement dans le chat pendant l'appel API.
     */
    private void showLoadingIndicator() {
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);
        loadingIndicator.setStyle("-fx-progress-color: #241197;");

        Label loadingLabel = new Label("L'IA réfléchit...");
        loadingLabel.setStyle("-fx-text-fill: #9b8fb5; -fx-font-size: 12px; -fx-font-style: italic;");

        HBox loadingBox = new HBox(10, loadingIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER_LEFT);
        loadingBox.setPadding(new Insets(5, 60, 5, 10));
        loadingBox.setId("loadingIndicator");

        chatContainer.getChildren().add(loadingBox);
        scrollToBottom();
    }

    /**
     * Masque l'indicateur de chargement.
     */
    private void hideLoadingIndicator() {
        chatContainer.getChildren().removeIf(node ->
                "loadingIndicator".equals(node.getId()));
    }

    // ═══ Quick Actions ═══

    @FXML
    private void quickResetPassword(ActionEvent e) {
        addUserMessage("reset password");
        addBotMessage(responses.get("reset password"));
    }

    @FXML
    private void quickCreateAccount(ActionEvent e) {
        addUserMessage("créer compte");
        addBotMessage(responses.get("create account"));
    }

    @FXML
    private void quickHelp(ActionEvent e) {
        addUserMessage("aide");
        addBotMessage(responses.get("help"));
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ═══ UI Helpers ═══

    private void addBotMessage(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.getStyleClass().addAll("chat-bubble", "chat-bubble-bot");

        Label time = new Label("🤖 " + LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-time");

        VBox bubble = new VBox(4, msg, time);
        bubble.setAlignment(Pos.CENTER_LEFT);
        bubble.setPadding(new Insets(0, 60, 0, 0));

        chatContainer.getChildren().add(bubble);
        scrollToBottom();
    }

    private void addUserMessage(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.getStyleClass().addAll("chat-bubble", "chat-bubble-user");

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-time");

        VBox bubble = new VBox(4, msg, time);
        bubble.setAlignment(Pos.CENTER_RIGHT);
        bubble.setPadding(new Insets(0, 0, 0, 60));

        chatContainer.getChildren().add(bubble);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Ouvre le chatbot dans une fenêtre flottante.
     */
    public static void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChatbotController.class.getResource("/ui/front/Chatbot.fxml"));
            Parent root = loader.load();

            Stage chatStage = new Stage();
            chatStage.initStyle(StageStyle.UNDECORATED);
            chatStage.initModality(Modality.NONE);
            chatStage.setTitle("Chatbot Rouh el Fann");
            chatStage.setScene(new Scene(root, 380, 550));
            chatStage.setAlwaysOnTop(true);
            chatStage.setResizable(false);
            chatStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
