package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;
import tn.rouhfan.tools.PasswordUtils;

import java.sql.*;
import java.util.UUID;

/**
 * Simulateur de connexion sociale (OAuth2).
 * Simule l'authentification via Google, Facebook et GitHub.
 *
 * En production, il faudrait intégrer les SDK OAuth2 officiels.
 * Cette classe simule le flux complet :
 * 1. L'utilisateur clique sur "Connexion avec Google/Facebook/GitHub"
 * 2. Un "profil" social est simulé (ou saisi par l'utilisateur)
 * 3. Si le compte existe avec cet email → connexion directe
 * 4. Si le compte n'existe pas → création automatique
 */
public class SocialLoginSimulator {

    private final UserService userService = new UserService();

    /**
     * Simule une connexion sociale.
     * @param provider "google", "facebook", "github"
     * @param email email du profil social
     * @param displayName nom affiché du profil social
     * @return l'utilisateur authentifié (existant ou nouveau)
     */
    public User socialLogin(String provider, String email, String displayName) throws SQLException {
        AppLogger.auth("SOCIAL_LOGIN_ATTEMPT", provider.toUpperCase() + " | " + email);

        // Vérifier si l'utilisateur existe déjà avec cet email
        User existingUser = userService.findByEmail(email);

        if (existingUser != null) {
            // L'utilisateur existe → mettre à jour le provider et connecter
            updateLoginProvider(existingUser.getId(), provider);
            existingUser.setLoginProvider(provider);

            AppLogger.auth("SOCIAL_LOGIN_EXISTING", provider.toUpperCase() + " | " + email + " (ID:" + existingUser.getId() + ")");
            return existingUser;
        }

        // L'utilisateur n'existe pas → créer un nouveau compte
        User newUser = createSocialUser(provider, email, displayName);
        AppLogger.auth("SOCIAL_LOGIN_NEW_ACCOUNT", provider.toUpperCase() + " | " + email + " (ID:" + newUser.getId() + ")");
        return newUser;
    }

    /**
     * Crée un nouvel utilisateur à partir d'un profil social.
     */
    private User createSocialUser(String provider, String email, String displayName) throws SQLException {
        // Extraire nom/prénom du displayName
        String[] parts = displayName.trim().split("\\s+", 2);
        String prenom = parts[0];
        String nom = parts.length > 1 ? parts[1] : provider + "User";

        // Générer un mot de passe aléatoire (l'utilisateur se connecte via social)
        String randomPassword = UUID.randomUUID().toString().substring(0, 12);
        String hashedPassword = PasswordUtils.hashPassword(randomPassword);

        User user = new User(nom, prenom, email, hashedPassword,
                "[\"ROLE_PARTICIPANT\"]", "actif", true, "participant");
        user.setLoginProvider(provider);

        userService.ajouter(user);

        // Mettre à jour le provider en base
        updateLoginProvider(user.getId(), provider);

        return user;
    }

    /**
     * Met à jour le provider de connexion en base.
     */
    private void updateLoginProvider(int userId, String provider) throws SQLException {
        ensureColumnExists();
        String sql = "UPDATE `user` SET login_provider = ? WHERE id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, provider);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Retourne les informations simulées d'un provider.
     */
    public static String getProviderIcon(String provider) {
        switch (provider.toLowerCase()) {
            case "google":   return "🔴";
            case "facebook": return "🔵";
            case "github":   return "⚫";
            default:         return "🔗";
        }
    }

    public static String getProviderLabel(String provider) {
        switch (provider.toLowerCase()) {
            case "google":   return "Google";
            case "facebook": return "Facebook";
            case "github":   return "GitHub";
            default:         return provider;
        }
    }

    /**
     * S'assure que la colonne login_provider existe.
     */
    private void ensureColumnExists() {
        try {
            Connection cnx = MyDatabase.getInstance().getConnection();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "user", "login_provider");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN login_provider VARCHAR(20) DEFAULT 'local'");
                    AppLogger.info("[SocialLogin] Colonne login_provider ajoutée");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Colonne probablement déjà existante
        }
    }
}
