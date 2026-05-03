package tn.rouhfan.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


public class Translationservice {

    // Même URL que le PHP
    private static final String API_URL        = "https://translate.googleapis.com/translate_a/single";
    private static final int    TIMEOUT_SECONDS = 5; // équivalent 'timeout' => 5

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;

    public Translationservice() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── translateText() — équivalent exact de la méthode PHP ─────────────────

    /**
     * @param text       Texte à traduire (peut être null — géré comme en PHP)
     * @param targetLang Code langue cible : "en", "ar", "es", "de", "it", "tr"...
     * @return           Texte traduit, ou texte original si erreur (comportement PHP)
     */
    public String translateText(String text, String targetLang) {

        // Équivalent : if (null === $text || empty(trim($text))) return (string) $text;
        if (text == null || text.trim().isEmpty()) {
            return text != null ? text : "";
        }

        try {
            // Mêmes paramètres que le PHP : client=gtx, sl=auto, tl=..., dt=t, q=...
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = API_URL
                    + "?client=gtx"
                    + "&sl=auto"
                    + "&tl=" + URLEncoder.encode(targetLang, StandardCharsets.UTF_8)
                    + "&dt=t"
                    + "&q=" + encoded;

            log("🔗 URL de traduction: " + url);
            log("📝 Texte source: " + text);
            log("🌐 Langue cible: " + targetLang);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            // Équivalent : if (200 !== $response->getStatusCode()) throw new Exception(...)
            if (response.statusCode() != 200) {
                log("❌ Erreur API Google : Statut " + response.statusCode());
                log("   Réponse: " + response.body());
                return text;
            }

            String result = parseGoogleResponse(response.body(), text);
            log("✅ Traduction réussie!");
            return result;

        } catch (Exception e) {
            // Équivalent : $this->logger->error('Translation Error: ' . $e->getMessage())
            log("❌ Translation Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return text; // retourne le texte original comme en PHP
        }
    }

    /** Surcharge avec langue par défaut "en" (comme PHP : targetLang = 'en') */
    public String translateText(String text) {
        return translateText(text, "en");
    }

    // ── Parsing de la réponse Google ─────────────────────────────────────────

    /**
     * Équivalent du bloc PHP :
     *   foreach ($data[0] as $segment) { $translatedText .= $segment[0] ?? ''; }
     *
     * Réponse brute Google : [[["Bonjour","Hello",null,null,10]], null, "fr", ...]
     */
    private String parseGoogleResponse(String body, String fallback) {
        try {
            // 🔍 DEBUG: Voir la réponse brute pour diagnostiquer
            log("📡 Réponse brute reçue (premiers 500 chars): " +
                    (body.length() > 500 ? body.substring(0, 500) : body));

            JsonNode root     = mapper.readTree(body);
            JsonNode segments = root.get(0); // $data[0]

            if (segments == null) {
                log("❌ ERROR: Impossible de trouver root.get(0)");
                log("   Structure JSON reçue: " + root.toString());
                return fallback;
            }

            if (!segments.isArray()) {
                log("❌ ERROR: root.get(0) n'est pas un tableau");
                log("   Type: " + segments.getNodeType() + ", Valeur: " + segments.toString());
                return fallback;
            }

            log("✅ Nombre de segments trouvés: " + segments.size());

            StringBuilder sb = new StringBuilder();

            // foreach ($data[0] as $segment) { $translatedText .= $segment[0] ?? ''; }
            for (int i = 0; i < segments.size(); i++) {
                JsonNode segment = segments.get(i);
                JsonNode part = segment.get(0); // $segment[0]
                if (part != null && !part.isNull()) {
                    String translated = part.asText();
                    log("  [Segment " + i + "] Texte traduit: " + translated);
                    sb.append(translated);
                }
            }

            String result = sb.toString();
            log("✅ Résultat final: " + result);
            return result.isEmpty() ? fallback : result;

        } catch (Exception e) {
            log("❌ PARSING ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(System.err);
            return fallback;
        }
    }

    // ── Méthode utilitaire pour nom + contenu d'un cours ─────────────────────

    /**
     * Traduit nom et contenu d'un cours — utilisé par Cours2Controller.
     * Effectue strip_tags() avant traduction (comme en PHP).
     */
    public TranslationResult translateCours(String nom, String contenu, String targetLang) {
        String contenuPropre = contenu != null
                ? contenu.replaceAll("<[^>]*>", "").trim()
                : "";
        return new TranslationResult(
                translateText(nom, targetLang),
                translateText(contenuPropre, targetLang)
        );
    }

    // ── Logger (équivalent de LoggerInterface Symfony) ────────────────────────

    /** Remplace par SLF4J/Log4j si tu en as un dans ton projet. */
    private void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
        System.out.println("[" + timestamp + "] [TranslationService] " + message);
        System.err.flush();
    }

    // ── Modèle de résultat ────────────────────────────────────────────────────

    public static class TranslationResult {
        public final String nom;
        public final String contenu;
        public TranslationResult(String nom, String contenu) {
            this.nom     = nom;
            this.contenu = contenu;
        }
    }
}