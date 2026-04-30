package tn.rouhfan.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ReclamationSMSService {

    private static final String API_KEY = "b5bbbc24";
    private static final String API_SECRET = "a97UPfxyqlJOT5Nz";

    public void sendSMS(String to, String messageText) {

        try {
            String textEncoded = URLEncoder.encode(messageText, "UTF-8");

            String urlString = "https://rest.nexmo.com/sms/json"
                    + "?api_key=" + API_KEY
                    + "&api_secret=" + API_SECRET
                    + "&to=" + to
                    + "&from=RouhFan"
                    + "&text=" + textEncoded;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            System.out.println("Code HTTP SMS: " + responseCode);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            System.out.println("Réponse SMS: " + response);

        } catch (Exception e) {
            System.out.println("❌ Erreur SMS: " + e.getMessage());
        }
    }
}