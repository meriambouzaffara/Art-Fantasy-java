package tn.rouhfan.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ReclamationOpenRouterAIService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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"; // 🔥 remplace par ta clé

    public String categorize(String text) {

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            // 🔥 recommandé par OpenRouter
            conn.setRequestProperty("HTTP-Referer", "http://localhost");
            conn.setRequestProperty("X-Title", "ReclamationApp");

            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // 🔥 sécuriser texte
            text = text.replace("\"", "'").replace("\n", " ").replace("\r", "");

            String prompt = "Classifie ce texte dans UNE SEULE catégorie parmi : "
                    + "Cours, Oeuvre, Article magasin, Sponsor, Evenement, Probleme technique. "
                    + "Réponds uniquement par UN MOT.\n\nTexte: " + text;

            String jsonInput = "{"
                    + "\"model\": \"openai/gpt-3.5-turbo\","
                    + "\"messages\": ["
                    + "{ \"role\": \"user\", \"content\": \"" + prompt + "\" }"
                    + "]"
                    + "}";

            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            System.out.println("Code HTTP OpenRouter: " + responseCode);

            BufferedReader br;

            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            System.out.println("Réponse OpenRouter: " + response);

            // 🔥 si erreur → fallback
            if (responseCode != 200) {
                return fallback(text);
            }

            return extractCategory(response.toString());

        } catch (Exception e) {
            System.out.println("❌ Erreur OpenRouter: " + e.getMessage());
            return fallback(text);
        }
    }

    // 🔥 extraction sécurisée
    private String extractCategory(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.indexOf("\"", start);

            if (start < 11 || end == -1) {
                return fallback(json);
            }

            String result = json.substring(start, end).trim();

            result = result.replace("\\n", "")
                    .replace("\\", "")
                    .replace("\"", "");

            return cleanCategory(result);

        } catch (Exception e) {
            return fallback(json);
        }
    }

    // 🔥 nettoyage pour éviter réponses bizarres
    private String cleanCategory(String result) {

        result = result.toLowerCase();

        if (result.contains("cours") || result.contains("qcm") || result.contains("certificat")) return "Cours";
        if (result.contains("oeuvre") || result.contains("art") || result.contains("œuvre")) return "Oeuvre";
        if (result.contains("article") || result.contains("produit")) return "Article magasin";
        if (result.contains("sponsor")) return "Sponsor";
        if (result.contains("evenement") || result.contains("événement") || result.contains("evnement")) return "Evenement";
        if (result.contains("bug") || result.contains("erreur") || result.contains("probleme") || result.contains("problème") || result.contains("problme") || result.contains("technique"))
            return "Probleme technique";

        return "Autre";
    }

    // 🔥 fallback intelligent (très important)
    private String fallback(String text) {

        text = text.toLowerCase();

        if (text.contains("cours") || text.contains("qcm") || text.contains("certificat")) return "Cours";
        if (text.contains("oeuvre") || text.contains("art") || text.contains("œuvre")) return "Oeuvre";
        if (text.contains("article") || text.contains("produit")) return "Article magasin";
        if (text.contains("sponsor")) return "Sponsor";
        if (text.contains("evenement") || text.contains("événement") || text.contains("evnement")) return "Evenement";
        if (text.contains("bug") || text.contains("erreur") || text.contains("probleme") || text.contains("problème") || text.contains("problme") || text.contains("technique"))
            return "Probleme technique";

        return "Autre";
    }
}