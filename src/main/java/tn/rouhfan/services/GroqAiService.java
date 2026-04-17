package tn.rouhfan.services;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.rouhfan.entities.Evenement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.cdimascio.dotenv.Dotenv;

public class GroqAiService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = dotenv.get("GROQ_API_KEY");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Classe pour le résultat complet
    public static class AiRecommendationResult {
        private List<AiRecommendationItem> items = new ArrayList<>();
        private String analysis;

        public AiRecommendationResult(List<AiRecommendationItem> items, String analysis) {
            this.items = items;
            this.analysis = analysis;
        }

        public List<AiRecommendationItem> getItems() { return items; }
        public String getAnalysis() { return analysis; }
    }

    // Classe pour un élément recommandé
    public static class AiRecommendationItem {
        private Evenement entity;
        private String aiVision;
        private String reason;

        public AiRecommendationItem(Evenement entity, String aiVision, String reason) {
            this.entity = entity;
            this.aiVision = aiVision;
            this.reason = reason;
        }

        public Evenement getEntity() { return entity; }
        public String getAiVision() { return aiVision; }
        public String getReason() { return reason; }
    }

    public AiRecommendationResult getRecommendations(List<Evenement> allEvents, int limit) {
        if (allEvents == null || allEvents.isEmpty()) {
            return new AiRecommendationResult(new ArrayList<>(), "Aucun événement disponible pour le moment.");
        }

        // Préparation du JSON des événements
        JSONArray eventsMetadata = new JSONArray();
        for (Evenement e : allEvents) {
            JSONObject evt = new JSONObject();
            evt.put("id", e.getId());
            evt.put("titre", e.getTitre());
            evt.put("type", e.getType() != null ? e.getType() : "Inconnu");
            evt.put("lieu", e.getLieu() != null ? e.getLieu() : "Inconnu");
            evt.put("sponsor", e.getSponsor() != null ? e.getSponsor().getNom() : "Aucun");
            String desc = e.getDescription() != null ? e.getDescription() : "";
            evt.put("description", desc.length() > 100 ? desc.substring(0, 100) : desc);
            evt.put("date", e.getDateEvent() != null ? dateFormat.format(e.getDateEvent()) : "Non définie");
            eventsMetadata.put(evt);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("En tant qu'expert en événementiel et recommandation personnalisée, analyse le profil de l'utilisateur et recommande-lui les meilleurs événements à venir.\n\n");
        prompt.append("Profil Utilisateur :\n- Historique de participations : Utilisateur standard régulier de la plateforme artistique Rouh'El Fann.\n\n");
        prompt.append("Liste des événements DISPONIBLES (format JSON) :\n");
        prompt.append(eventsMetadata.toString()).append("\n\n");
        
        prompt.append("Instructions :\n");
        prompt.append("1. Analyse les types d'événements, les sponsors et les lieux pertinents.\n");
        prompt.append("2. Sélectionne les événements qui correspondent le mieux.\n");
        prompt.append("3. Pour chaque recommandation, explique précisément ce que l'IA 'voit' comme point fort (ambiance, opportunité, pertinence) et pourquoi cela correspond à l'utilisateur.\n");
        prompt.append("4. Réponds UNIQUEMENT avec un objet JSON structuré comme suit: {\"recommandations\": [{\"id\": 1, \"analyse_IA\": \"Ce que l'IA détecte...\", \"pourquoi\": \"Raison détaillée...\"}], \"analyse_globale\": \"Analyse courte de vos goûts événementiels...\"}\n");

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("model", "llama-3.3-70b-versatile");
            
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Tu es un assistant expert en recommandation d'événements culturels et artistiques. Tu réponds uniquement en JSON."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt.toString()));
            
            jsonRequest.put("messages", messages);
            jsonRequest.put("temperature", 0.4);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    String resultString = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    
                    // Regex pour extraire le JSON au cas où le LLM ajoute du markdown
                    Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(resultString);
                    
                    JSONObject data = new JSONObject();
                    if (matcher.find()) {
                        data = new JSONObject(matcher.group(0));
                    }

                    String analyse = data.has("analyse_globale") ? data.getString("analyse_globale") : "Basé sur l'ensemble de notre catalogue d'événements.";
                    List<AiRecommendationItem> items = new ArrayList<>();
                    
                    if (data.has("recommandations")) {
                        JSONArray recs = data.getJSONArray("recommandations");
                        List<Integer> seenIds = new ArrayList<>();
                        for (int i = 0; i < recs.length(); i++) {
                            JSONObject rec = recs.getJSONObject(i);
                            if (rec.has("id")) {
                                int id = rec.getInt("id");
                                if (!seenIds.contains(id)) {
                                    // Trouver l'événement correspondant
                                    Evenement matchedEvent = null;
                                    for(Evenement evt : allEvents) {
                                        if (evt.getId() == id) {
                                            matchedEvent = evt;
                                            break;
                                        }
                                    }
                                    
                                    if (matchedEvent != null) {
                                        String aiVision = rec.optString("analyse_IA", "Un événement prometteur qui correspond à votre profil.");
                                        String reason = rec.optString("pourquoi", "Cet événement s'aligne avec vos activités habituelles.");
                                        items.add(new AiRecommendationItem(matchedEvent, aiVision, reason));
                                        seenIds.add(id);
                                        if (items.size() >= limit) break;
                                    }
                                }
                            }
                        }
                    }
                    return new AiRecommendationResult(items, analyse);
                }
            } else {
                System.err.println("Groq API Error " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Erreur technique GroqAiService : " + e.getMessage());
        }
        
        // Fallback: retourne des événements aléatoires ou les derniers ajoutés
        List<AiRecommendationItem> fallbackItems = new ArrayList<>();
        int count = 0;
        for (int i = allEvents.size() - 1; i >= 0 && count < limit; i--) {
            fallbackItems.add(new AiRecommendationItem(
                    allEvents.get(i), 
                    "Découvrez de nouvelles expériences.", 
                    "Événement populaire à ne pas manquer."
            ));
            count++;
        }
        
        return new AiRecommendationResult(
                fallbackItems, 
                "Voici quelques événements de notre catalogue qui pourraient vous intéresser."
        );
    }
}
