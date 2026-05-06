package tn.rouhfan.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.rouhfan.entities.Favoris;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class OeuvreRecommendationService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final String API_KEY;
    private final OeuvreService oeuvreService;
    private final FavorisService favorisService;
    private final HttpClient httpClient;

    public OeuvreRecommendationService() {
        this.oeuvreService = new OeuvreService();
        this.favorisService = new FavorisService();
        this.httpClient = HttpClient.newHttpClient();
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.API_KEY = dotenv.get("GROQ_API_KEY");
    }

    /**
     * Calcule et retourne une liste d'œuvres recommandées pour un participant.
     * Basé sur les catégories, artistes et prix des œuvres en favoris.
     */
    public List<Oeuvre> getRecommendations(int userId) {
        try {
            // 1. Récupérer les favoris de l'utilisateur
            List<Favoris> userFavoris = favorisService.recupererParUser(userId);
            Set<Integer> favoritedIds = userFavoris.stream()
                    .map(f -> f.getOeuvre().getId())
                    .collect(Collectors.toSet());

            // 2. Si aucun favori, retourner les oeuvres les plus récentes
            if (userFavoris.isEmpty()) {
                return oeuvreService.recuperer().stream()
                        .filter(o -> "disponible".equalsIgnoreCase(o.getStatut()))
                        .sorted(Comparator.comparing(Oeuvre::getId).reversed())
                        .limit(8)
                        .collect(Collectors.toList());
            }

            // 3. Analyser le profil de préférence
            Map<Integer, Long> categoryFreq = userFavoris.stream()
                    .filter(f -> f.getOeuvre().getCategorie() != null)
                    .collect(Collectors.groupingBy(f -> f.getOeuvre().getCategorie().getIdCategorie(), Collectors.counting()));

            Map<Integer, Long> artistFreq = userFavoris.stream()
                    .filter(f -> f.getOeuvre().getUser() != null)
                    .collect(Collectors.groupingBy(f -> f.getOeuvre().getUser().getId(), Collectors.counting()));

            double avgPrice = userFavoris.stream()
                    .mapToDouble(f -> f.getOeuvre().getPrix() != null ? f.getOeuvre().getPrix().doubleValue() : 0.0)
                    .average().orElse(0.0);

            // 4. Récupérer toutes les oeuvres disponibles non déjà en favoris
            List<Oeuvre> allOeuvres = oeuvreService.recuperer().stream()
                    .filter(o -> "disponible".equalsIgnoreCase(o.getStatut()))
                    .filter(o -> !favoritedIds.contains(o.getId()))
                    .collect(Collectors.toList());

            // 5. Calculer le score de chaque oeuvre
            Map<Oeuvre, Integer> scoredOeuvres = new HashMap<>();
            for (Oeuvre o : allOeuvres) {
                int score = 0;

                // Match catégorie (Poids fort: 5 pts par occurrence en favoris)
                if (o.getCategorie() != null && categoryFreq.containsKey(o.getCategorie().getIdCategorie())) {
                    score += 5 * categoryFreq.get(o.getCategorie().getIdCategorie()).intValue();
                }

                // Match artiste (Poids moyen: 3 pts par occurrence en favoris)
                if (o.getUser() != null && artistFreq.containsKey(o.getUser().getId())) {
                    score += 3 * artistFreq.get(o.getUser().getId()).intValue();
                }

                // Similitude de prix (Poids faible: 2 pts si à +/- 30% de la moyenne)
                if (avgPrice > 0 && o.getPrix() != null) {
                    double p = o.getPrix().doubleValue();
                    if (p >= avgPrice * 0.7 && p <= avgPrice * 1.3) {
                        score += 2;
                    }
                }

                if (score > 0) {
                    scoredOeuvres.put(o, score);
                }
            }

            // 6. Trier par score décroissant et retourner le top 8
            return scoredOeuvres.entrySet().stream()
                    .sorted(Map.Entry.<Oeuvre, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .limit(8)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du calcul des recommandations: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Génère une analyse complète (profil + oeuvres) via Groq Vision.
     */
    public JSONObject getAnalysis(User user, List<Oeuvre> recommendations) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error", "Clé API Groq manquante");
            return error;
        }

        try {
            List<Favoris> userFavoris = favorisService.recupererParUser(user.getId());
            String preferences = userFavoris.isEmpty() 
                    ? "l'art en général (l'utilisateur découvre la galerie)"
                    : userFavoris.stream()
                        .map(f -> f.getOeuvre().getCategorie().getNomCategorie() + " (" + f.getOeuvre().getTitre() + ")")
                        .collect(Collectors.joining(", "));

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "llama-3.3-70b-versatile");
            
            JSONArray messages = new JSONArray();
            
            // Message Système
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "Tu es un expert en art. Tu dois analyser un profil utilisateur et une sélection d'oeuvres. " +
                    "Tu DOIS renvoyer un objet JSON ayant EXACTEMENT cette structure : " +
                    "{ \"profil_analysis\": \"ton analyse globale...\", \"oeuvres\": [ { \"id\": 123, \"vision\": \"...\", \"pourquoi\": \"...\" } ] } " +
                    "Même si le profil est nouveau, donne une analyse inspirante. Langue: Français.");
            messages.put(systemMsg);

            // Message Utilisateur Textuel (Sans Image)
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Analyse ces oeuvres pour l'utilisateur qui aime: ").append(preferences).append(".\n");
            promptBuilder.append("Pour chaque oeuvre, fournis une analyse artistique détaillée (imagine le style visuel à partir du titre, artiste et catégorie) et explique pourquoi elle lui convient.\n\n");

            int count = 0;
            for (Oeuvre o : recommendations) {
                if (count >= 6) break; // On peut en analyser un peu plus sans les images
                String catName = o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Inconnue";
                String artist = (o.getUser() != null) ? (o.getUser().getNom() + " " + o.getUser().getPrenom()) : "Inconnu";
                
                promptBuilder.append("- [Oeuvre ID: ").append(o.getId())
                             .append(", Titre: '").append(o.getTitre())
                             .append("', Artiste: ").append(artist)
                             .append(", Catégorie: ").append(catName).append("]\n");
                count++;
            }
            
            userMsg.put("content", promptBuilder.toString());
            messages.put(userMsg);
            
            requestBody.put("messages", messages);
            requestBody.put("response_format", new JSONObject().put("type", "json_object"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject resJson = new JSONObject(response.body());
                String content = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                return new JSONObject(content);
            } else {
                String errorBody = response.body();
                System.err.println("❌ Groq API Error: " + response.statusCode() + " - " + errorBody);
                
                // Retourner le message d'erreur précis pour aider au debug
                String detail = "Code " + response.statusCode();
                try {
                    JSONObject errorJson = new JSONObject(errorBody);
                    if (errorJson.has("error")) {
                        detail = errorJson.getJSONObject("error").optString("message", detail);
                    }
                } catch (Exception ignored) {}
                
                return new JSONObject().put("error", detail);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("error", e.getMessage());
        }
    }

    private String encodeImageToBase64(String imageName) {
        String fullPath = ImageUtils.getAbsolutePath(imageName);
        if (fullPath == null) return null;
        
        try {
            // Nettoyer l'URL si elle commence par file:/
            String path = fullPath.replace("file:/", "");
            if (path.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
                path = path.substring(1);
            }
            File file = new File(path);
            if (!file.exists()) return null;
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            return null;
        }
    }
}
