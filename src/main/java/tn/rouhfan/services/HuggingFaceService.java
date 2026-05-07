package tn.rouhfan.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.rouhfan.tools.AppLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service d'intégration Hugging Face Inference API.
 *
 * ═══════════════════════════════════════════════════════════
 *  SÉCURITÉ : La clé API est chargée via variable d'environnement
 *  (HUGGINGFACE_API_KEY dans .env). Elle n'est JAMAIS exposée
 *  dans les logs, le frontend ou le code source.
 * ═══════════════════════════════════════════════════════════
 *
 * Fonctionnalités :
 * - Génération de texte (chatbot)
 * - Mécanisme de retry (3 tentatives)
 * - Gestion des erreurs (timeout, rate limit, réponse invalide)
 * - Validation des requêtes
 * - Logging sécurisé (sans exposer la clé)
 */
public class HuggingFaceService {

    // ═══════════════════════════════════════
    //  Configuration
    // ═══════════════════════════════════════

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    /** Clé API chargée depuis l'environnement — JAMAIS hardcodée */
    private static final String API_KEY = dotenv.get("HUGGINGFACE_API_KEY");

    /** URL de l'API Hugging Face Inference (modèle text-generation) */
    private static final String API_URL =
            "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-v0.1";

    /** Timeout de connexion HTTP */
    private static final int CONNECT_TIMEOUT_SECONDS = 15;

    /** Timeout de lecture HTTP */
    private static final int READ_TIMEOUT_SECONDS = 30;

    /** Nombre maximum de tentatives en cas d'échec */
    private static final int MAX_RETRIES = 3;

    /** Délai de base entre les tentatives (ms) — augmente exponentiellement */
    private static final long BASE_RETRY_DELAY_MS = 1000;

    /** Longueur maximale du prompt utilisateur */
    private static final int MAX_PROMPT_LENGTH = 2000;

    /** Nombre maximal de tokens générés */
    private static final int MAX_NEW_TOKENS = 512;

    // ═══ Compteurs pour les statistiques admin ═══
    private static int totalApiCalls = 0;
    private static int totalErrors = 0;
    private static int totalSuccesses = 0;

    private final ActivityLogService activityLogService;

    public HuggingFaceService() {
        this.activityLogService = new ActivityLogService();
    }

    // ═══════════════════════════════════════
    //  Méthode principale : Génération de texte
    // ═══════════════════════════════════════

    /**
     * Génère une réponse textuelle via Hugging Face Inference API.
     * Inclut un mécanisme de retry automatique (3 tentatives).
     *
     * @param userPrompt Le message de l'utilisateur
     * @param userId     L'ID de l'utilisateur (pour le logging)
     * @return La réponse générée par l'IA
     * @throws HuggingFaceException En cas d'erreur irrécupérable
     */
    public String generateText(String userPrompt, int userId) throws HuggingFaceException {
        // ── Validation de la requête ──
        validateRequest(userPrompt);

        // ── Vérification de la clé API ──
        if (API_KEY == null || API_KEY.isBlank()) {
            AppLogger.error("[HuggingFace] Clé API manquante. Définir HUGGINGFACE_API_KEY dans .env");
            throw new HuggingFaceException("Clé API Hugging Face non configurée.");
        }

        totalApiCalls++;
        AppLogger.info("[HuggingFace] Appel API #" + totalApiCalls
                + " | User ID: " + userId
                + " | Prompt length: " + userPrompt.length() + " chars");

        // ── Log de l'activité (sans exposer la clé ni le contenu complet) ──
        activityLogService.log(userId, "HUGGINGFACE_API_CALL",
                "Appel API text-generation | Prompt: " + userPrompt.substring(0, Math.min(50, userPrompt.length())) + "...");

        // ── Retry loop ──
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String result = executeApiCall(userPrompt);
                totalSuccesses++;
                AppLogger.info("[HuggingFace] Réponse reçue avec succès (tentative " + attempt + ")");
                return result;
            } catch (HuggingFaceRateLimitException e) {
                // Rate limit : attente exponentielle avant retry
                lastException = e;
                long delay = BASE_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                AppLogger.warn("[HuggingFace] Rate limit atteint (tentative " + attempt + "/" + MAX_RETRIES
                        + "). Attente " + delay + "ms...");
                sleepSafely(delay);
            } catch (HuggingFaceException e) {
                // Autre erreur API : retry immédiat
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_RETRY_DELAY_MS * attempt;
                    AppLogger.warn("[HuggingFace] Erreur (tentative " + attempt + "/" + MAX_RETRIES
                            + "): " + e.getMessage() + ". Retry dans " + delay + "ms...");
                    sleepSafely(delay);
                }
            }
        }

        // ── Toutes les tentatives ont échoué ──
        totalErrors++;
        String errorMsg = "Échec après " + MAX_RETRIES + " tentatives.";
        if (lastException != null) {
            errorMsg += " Dernière erreur: " + lastException.getMessage();
        }
        activityLogService.log(userId, "HUGGINGFACE_API_ERROR", errorMsg);
        AppLogger.error("[HuggingFace] " + errorMsg);
        throw new HuggingFaceException(errorMsg);
    }

    // ═══════════════════════════════════════
    //  Exécution de l'appel HTTP
    // ═══════════════════════════════════════

    /**
     * Exécute un appel HTTP vers l'API Hugging Face.
     *
     * @param prompt Le prompt formaté
     * @return La réponse textuelle de l'IA
     */
    private String executeApiCall(String prompt) throws HuggingFaceException {
        try {
            // ── Construction du payload JSON ──
            // Format Mistral : <s>[INST] {system} {user} [/INST]
            String formattedPrompt = "<s>[INST] Tu es un assistant intelligent pour la plateforme artistique Rouh el Fann. "
                    + "Réponds de manière concise en français. " + prompt + " [/INST]";

            JSONObject parameters = new JSONObject();
            parameters.put("max_new_tokens", MAX_NEW_TOKENS);
            parameters.put("temperature", 0.7);
            parameters.put("top_p", 0.9);
            parameters.put("return_full_text", false);

            JSONObject payload = new JSONObject();
            payload.put("inputs", formattedPrompt);
            payload.put("parameters", parameters);

            // ── Construction de la requête HTTP ──
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // ── Envoi et traitement de la réponse ──
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return processResponse(response);

        } catch (HuggingFaceException e) {
            throw e; // Re-throw HuggingFace exceptions directement
        } catch (java.net.http.HttpTimeoutException e) {
            throw new HuggingFaceException("Timeout : l'API met trop de temps à répondre.");
        } catch (Exception e) {
            throw new HuggingFaceException("Erreur de communication avec l'API : " + e.getMessage());
        }
    }

    /**
     * Traite la réponse HTTP de l'API Hugging Face.
     */
    private String processResponse(HttpResponse<String> response) throws HuggingFaceException {
        int statusCode = response.statusCode();
        String body = response.body();

        // ── Gestion des codes d'erreur HTTP ──
        switch (statusCode) {
            case 200:
                break; // OK — continuer le traitement
            case 401:
                AppLogger.error("[HuggingFace] Erreur 401 : Clé API invalide");
                throw new HuggingFaceException("Authentification échouée. Vérifiez votre clé API.");
            case 429:
                AppLogger.warn("[HuggingFace] Erreur 429 : Rate limit atteint");
                throw new HuggingFaceRateLimitException("Trop de requêtes. Veuillez patienter.");
            case 503:
                // Modèle en cours de chargement — peut être retryé
                AppLogger.warn("[HuggingFace] Erreur 503 : Modèle en cours de chargement");
                throw new HuggingFaceException("Le modèle est en cours de chargement. Réessayez dans quelques secondes.");
            default:
                AppLogger.error("[HuggingFace] Erreur HTTP " + statusCode + " : " + body);
                throw new HuggingFaceException("Erreur serveur (HTTP " + statusCode + ").");
        }

        // ── Extraction de la réponse du JSON ──
        try {
            JSONArray jsonArray = new JSONArray(body);
            if (jsonArray.length() > 0) {
                JSONObject firstResult = jsonArray.getJSONObject(0);
                String generatedText = firstResult.optString("generated_text", "").trim();

                if (generatedText.isEmpty()) {
                    throw new HuggingFaceException("Réponse vide de l'API.");
                }
                return generatedText;
            }
            throw new HuggingFaceException("Format de réponse inattendu.");
        } catch (org.json.JSONException e) {
            // Peut-être un objet JSON d'erreur au lieu d'un tableau
            try {
                JSONObject errorObj = new JSONObject(body);
                String errorMsg = errorObj.optString("error", "Erreur inconnue");
                // Vérifier si c'est un modèle en chargement
                if (errorMsg.contains("loading")) {
                    throw new HuggingFaceException("Le modèle est en cours de chargement. Réessayez dans quelques secondes.");
                }
                throw new HuggingFaceException("Erreur API : " + errorMsg);
            } catch (org.json.JSONException e2) {
                throw new HuggingFaceException("Réponse invalide de l'API.");
            }
        }
    }

    // ═══════════════════════════════════════
    //  Validation
    // ═══════════════════════════════════════

    /**
     * Valide la requête utilisateur avant envoi à l'API.
     */
    private void validateRequest(String prompt) throws HuggingFaceException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new HuggingFaceException("Le message ne peut pas être vide.");
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new HuggingFaceException("Le message est trop long (max " + MAX_PROMPT_LENGTH + " caractères).");
        }
    }

    // ═══════════════════════════════════════
    //  Statistiques (pour le dashboard admin)
    // ═══════════════════════════════════════

    /** Retourne le nombre total d'appels API depuis le démarrage */
    public static int getTotalApiCalls() { return totalApiCalls; }

    /** Retourne le nombre d'appels réussis */
    public static int getTotalSuccesses() { return totalSuccesses; }

    /** Retourne le nombre d'erreurs */
    public static int getTotalErrors() { return totalErrors; }

    /** Réinitialise les compteurs */
    public static void resetCounters() {
        totalApiCalls = 0;
        totalSuccesses = 0;
        totalErrors = 0;
    }

    // ═══════════════════════════════════════
    //  Utilitaires
    // ═══════════════════════════════════════

    private void sleepSafely(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Vérifie si le service est correctement configuré.
     * @return true si la clé API est présente
     */
    public boolean isConfigured() {
        return API_KEY != null && !API_KEY.isBlank();
    }

    // ═══════════════════════════════════════
    //  Exceptions personnalisées
    // ═══════════════════════════════════════

    /** Exception générale Hugging Face */
    public static class HuggingFaceException extends Exception {
        public HuggingFaceException(String message) { super(message); }
    }

    /** Exception spécifique au rate limiting */
    public static class HuggingFaceRateLimitException extends HuggingFaceException {
        public HuggingFaceRateLimitException(String message) { super(message); }
    }
}
