package tn.rouhfan.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Équivalent Java de AiQcmGeneratorService.php
 * Utilise l'API Groq (LLaMA 3.3) pour générer des questions QCM.
 *
 * Lit la clé API depuis le fichier .env (variable GROQ_API_KEY).
 */
public class AiQcmGeneratorService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    // Clé API Groq
    private static final String API_KEY = "VOTRE_CLE_API_GROQ";

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;

    public AiQcmGeneratorService() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper     = new ObjectMapper();
    }

    // ── Modèles de données ────────────────────────────────────────────────────

    public static class ReponseData {
        public String  texte;
        public boolean correcte;
        public ReponseData(String texte, boolean correcte) {
            this.texte    = texte;
            this.correcte = correcte;
        }
    }

    public static class QuestionData {
        public String            question;
        public List<ReponseData> reponses;
        public QuestionData(String question, List<ReponseData> reponses) {
            this.question = question;
            this.reponses = reponses;
        }
    }

    // ── Méthode principale ────────────────────────────────────────────────────

    public List<QuestionData> generateQuestions(String contenu, int nombre) throws Exception {
        String contenuNettoye = contenu.replaceAll("<[^>]*>", "").trim();

        Map<String, Object> systemMessage = Map.of(
                "role",    "system",
                "content", "Tu es un expert pédagogique. Tu génères des QCM au format JSON strict " +
                        "basé uniquement sur le texte fourni. " +
                        "Structure OBLIGATOIRE : " +
                        "{\"questions\": [{\"question\": \"...\", \"reponses\": " +
                        "[{\"texte\": \"...\", \"correcte\": true/false}]}]}. " +
                        "Génère exactement 4 réponses par question dont UNE SEULE correcte."
        );

        Map<String, Object> userMessage = Map.of(
                "role",    "user",
                "content", "Contenu du cours : '" + contenuNettoye + "'. " +
                        "Génère " + nombre + " questions pertinentes à partir de ce texte."
        );

        Map<String, Object> payload = Map.of(
                "model",           MODEL,
                "messages",        List.of(systemMessage, userMessage),
                "response_format", Map.of("type", "json_object"),
                "temperature",     0.6
        );

        String payloadJson = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type",  "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new Exception("Erreur API Groq (" + response.statusCode() + ") : " + response.body());
        }

        return parseResponse(response.body());
    }

    public List<QuestionData> generateQuestions(String contenu) throws Exception {
        return generateQuestions(contenu, 5);
    }

    private List<QuestionData> parseResponse(String responseBody) throws Exception {
        List<QuestionData> result = new ArrayList<>();

        JsonNode root      = mapper.readTree(responseBody);
        String   content   = root.path("choices").get(0).path("message").path("content").asText();
        JsonNode decoded   = mapper.readTree(content);
        JsonNode questions = decoded.path("questions");

        if (questions.isArray()) {
            for (JsonNode qNode : questions) {
                String            questionText = qNode.path("question").asText();
                List<ReponseData> reponses     = new ArrayList<>();
                for (JsonNode rNode : qNode.path("reponses")) {
                    reponses.add(new ReponseData(
                            rNode.path("texte").asText(),
                            rNode.path("correcte").asBoolean()
                    ));
                }
                result.add(new QuestionData(questionText, reponses));
            }
        }

        return result;
    }
}