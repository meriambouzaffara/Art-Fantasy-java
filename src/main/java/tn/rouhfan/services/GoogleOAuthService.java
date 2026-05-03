package tn.rouhfan.services;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.SessionManager;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;

/**
 * Service gérant l'authentification Google OAuth2 via WebView.
 */
public class GoogleOAuthService {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
    private static final String CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final UserService userService = new UserService();

    public interface OAuthCallback {
        void onSuccess(User user);
        void onError(String error);
        void onCancel();
    }

    /**
     * Ouvre une popup JavaFX avec une WebView pour la connexion Google.
     */
    public void openLoginPopup(OAuthCallback callback) {
        if (CLIENT_ID == null || CLIENT_ID.isEmpty()) {
            callback.onError("GOOGLE_CLIENT_ID non configuré dans .env");
            return;
        }

        Platform.runLater(() -> {
            Stage authStage = new Stage();
            authStage.initModality(Modality.APPLICATION_MODAL);
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            String authUrl = AUTH_URL + "?"
                    + "client_id=" + encode(CLIENT_ID)
                    + "&redirect_uri=" + encode(REDIRECT_URI)
                    + "&response_type=code"
                    + "&scope=" + encode("openid email profile");

            final boolean[] isComplete = {false};

            engine.locationProperty().addListener((observable, oldLocation, newLocation) -> {
                System.out.println("[GoogleOAuth] Navigation : " + newLocation);
                
                if (newLocation.startsWith(REDIRECT_URI)) {
                    isComplete[0] = true;
                    authStage.close();
                    
                    if (newLocation.contains("code=")) {
                        String code = newLocation.split("code=")[1].split("&")[0];
                        System.out.println("[GoogleOAuth] Code reçu, échange en cours...");
                        handleCodeExchange(code, callback);
                    } else if (newLocation.contains("error=")) {
                        String error = newLocation.split("error=")[1].split("&")[0];
                        callback.onError(error);
                    } else {
                        callback.onCancel();
                    }
                }
            });

            authStage.setScene(new Scene(webView, 550, 650));
            authStage.setTitle("Se connecter avec Google");
            authStage.setOnCloseRequest(e -> {
                if (!isComplete[0]) {
                    callback.onCancel();
                }
            });
            authStage.show();
            engine.load(authUrl);
        });
    }

    private void handleCodeExchange(String code, OAuthCallback callback) {
        new Thread(() -> {
            try {
                String accessToken = exchangeCodeForToken(code);
                JSONObject profile = fetchGoogleProfile(accessToken);
                
                String email = profile.getString("email");
                String firstName = profile.optString("given_name", "");
                String lastName = profile.optString("family_name", "");

                // Chercher ou créer l'utilisateur
                User user = userService.findByEmail(email);
                if (user == null) {
                    user = new User();
                    user.setEmail(email);
                    user.setPrenom(firstName);
                    user.setNom(lastName);
                    user.setRoles("ROLE_PARTICIPANT");
                    user.setStatut("Actif");
                    user.setType("Participant");
                    user.setVerified(true);
                    user.setLoginProvider("google");
                    
                    userService.ajouter(user);
                    // L'ID est mis automatiquement sur l'objet par ajouter()
                } else {
                    // Mettre à jour le provider si besoin
                    updateLoginProvider(user.getId(), "google");
                    user.setLoginProvider("google");
                }

                // Variable finale pour le lambda
                final User finalUser = user;

                // Connecter l'utilisateur
                SessionManager.getInstance().login(finalUser);
                userService.updateLastLogin(finalUser.getId());
                
                AppLogger.auth("GOOGLE_LOGIN_SUCCESS", email);
                Platform.runLater(() -> callback.onSuccess(finalUser));

            } catch (Exception e) {
                AppLogger.error("Erreur OAuth Google", e);
                Platform.runLater(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String body = "code=" + encode(code)
                + "&client_id=" + encode(CLIENT_ID)
                + "&client_secret=" + encode(CLIENT_SECRET)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        
        if (json.has("error")) {
            throw new Exception(json.getString("error_description"));
        }
        return json.getString("access_token");
    }

    private JSONObject fetchGoogleProfile(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    private void updateLoginProvider(int userId, String provider) throws SQLException {
        ensureColumnExists();
        String sql = "UPDATE `user` SET login_provider = ? WHERE id = ?";
        try (java.sql.Connection cnx = tn.rouhfan.tools.MyDatabase.getInstance().getConnection();
             java.sql.PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, provider);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void ensureColumnExists() {
        try {
            java.sql.Connection cnx = tn.rouhfan.tools.MyDatabase.getInstance().getConnection();
            java.sql.DatabaseMetaData meta = cnx.getMetaData();
            java.sql.ResultSet rs = meta.getColumns(null, null, "user", "login_provider");
            if (!rs.next()) {
                try (java.sql.Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN login_provider VARCHAR(20) DEFAULT 'local'");
                }
            }
        } catch (Exception e) {
            // Ignorer si déjà présent
        }
    }

    /**
     * Vérifie si Google OAuth est configuré (CLIENT_ID et SECRET présents dans .env).
     */
    public boolean isConfigured() {
        return CLIENT_ID != null && !CLIENT_ID.isEmpty()
                && CLIENT_SECRET != null && !CLIENT_SECRET.isEmpty();
    }

    /**
     * Génère l'URL d'autorisation Google OAuth2.
     * Utilisé par ForgotPasswordController pour ouvrir la WebView.
     */
    public String getAuthorizationUrl() {
        return AUTH_URL + "?"
                + "client_id=" + encode(CLIENT_ID)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile");
    }

    /**
     * Vérifie que l'email du compte Google correspond à l'email fourni.
     * Utilisé pour la vérification d'identité lors de la réinitialisation du mot de passe.
     *
     * @param authorizationCode Code d'autorisation reçu de Google
     * @param expectedEmail     Email que l'utilisateur prétend posséder
     * @return true si l'email Google correspond
     */
    public boolean verifyEmailOwnership(String authorizationCode, String expectedEmail) throws GoogleOAuthException {
        try {
            String accessToken = exchangeCodeForToken(authorizationCode);
            JSONObject profile = fetchGoogleProfile(accessToken);
            String googleEmail = profile.getString("email");

            return googleEmail.equalsIgnoreCase(expectedEmail);
        } catch (Exception e) {
            throw new GoogleOAuthException("Erreur de vérification Google: " + e.getMessage(), e);
        }
    }

    /**
     * Exception spécifique aux erreurs Google OAuth.
     */
    public static class GoogleOAuthException extends Exception {
        public GoogleOAuthException(String message) {
            super(message);
        }
        public GoogleOAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
