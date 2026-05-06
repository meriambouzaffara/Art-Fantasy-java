package tn.rouhfan.services;

import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;

import java.security.SecureRandom;
import java.sql.*;

/**
 * Service de vérification d'email à l'inscription.
 *
 * Flux :
 * 1. À l'inscription → generateVerificationToken(userId, email)
 * 2. Le compte est créé avec isVerified=false
 * 3. Un token 6 chiffres est envoyé par email
 * 4. L'utilisateur saisit le token → verifyEmail(email, token)
 * 5. Si valide → isVerified=true, le compte est activé
 */
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserEmailService emailService = new UserEmailService();

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Génère un token de vérification et envoie un email.
     * @return le token généré (pour affichage en debug)
     */
    public String generateVerificationToken(int userId, String email, String userName) throws SQLException {
        String token = generateToken();

        ensureColumnExists();

        // Stocker le token en base
        String sql = "UPDATE `user` SET verification_token = ?, is_verified = FALSE WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }

        // Envoyer l'email
        emailService.sendVerificationEmail(email, userName, token);

        AppLogger.auth("VERIFICATION_EMAIL_SENT", email + " (ID:" + userId + ")");
        return token;
    }

    /**
     * Vérifie le token et active le compte si valide.
     * @return true si la vérification a réussi
     */
    public boolean verifyEmail(String email, String token) throws SQLException {
        ensureColumnExists();

        String sql = "SELECT id, verification_token FROM `user` WHERE email = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedToken = rs.getString("verification_token");
                    int userId = rs.getInt("id");

                    if (token != null && token.equals(storedToken)) {
                        // Activer le compte
                        String updateSql = "UPDATE `user` SET is_verified = TRUE, verification_token = NULL WHERE id = ?";
                        try (PreparedStatement updatePs = getConnection().prepareStatement(updateSql)) {
                            updatePs.setInt(1, userId);
                            updatePs.executeUpdate();
                        }

                        AppLogger.auth("EMAIL_VERIFIED", email + " (ID:" + userId + ")");
                        return true;
                    }
                }
            }
        }

        AppLogger.warn("[EmailVerification] Token invalide pour: " + email);
        return false;
    }

    /**
     * Renvoie un nouveau token de vérification.
     */
    public String resendVerificationToken(String email, String userName) throws SQLException {
        ensureColumnExists();

        // Vérifier que le compte existe et n'est pas déjà vérifié
        String sql = "SELECT id, is_verified FROM `user` WHERE email = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getBoolean("is_verified")) {
                        AppLogger.info("[EmailVerification] Compte déjà vérifié: " + email);
                        return null; // Déjà vérifié
                    }
                    int userId = rs.getInt("id");
                    return generateVerificationToken(userId, email, userName);
                }
            }
        }
        return null;
    }

    /**
     * Génère un token numérique de 6 chiffres.
     */
    private String generateToken() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * S'assure que la colonne verification_token existe.
     */
    private void ensureColumnExists() {
        try {
            Connection cnx = getConnection();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "user", "verification_token");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN verification_token VARCHAR(255) NULL");
                    AppLogger.info("[EmailVerification] Colonne verification_token ajoutée");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Colonne probablement déjà existante
        }
    }
}
