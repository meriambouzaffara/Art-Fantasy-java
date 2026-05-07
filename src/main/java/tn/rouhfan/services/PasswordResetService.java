package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;
import tn.rouhfan.tools.PasswordUtils;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Date;

/**
 * Service de réinitialisation de mot de passe.
 *
 * Flux :
 * 1. L'utilisateur demande un reset → generateResetToken(email)
 * 2. Un token 6 chiffres est généré, stocké en base avec expiration 30 min
 * 3. Un email est envoyé avec le token
 * 4. L'utilisateur saisit le token → validateResetToken(email, token)
 * 5. Si valide → resetPassword(email, token, newPassword)
 */
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserEmailService emailService = new UserEmailService();

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Génère un token de réinitialisation et l'enregistre dans la table dédiée.
     */
    public boolean generateResetToken(String email) throws SQLException {
        UserService userService = new UserService();
        User user = userService.findByEmail(email);
        if (user == null) {
            AppLogger.warn("[PasswordReset] Tentative de reset pour email inconnu: " + email);
            return false;
        }

        // Générer un token 6 chiffres
        String token = generateToken();
        // Hasher le token pour le stockage (sécurité)
        String hashedToken = PasswordUtils.hashPassword(token);

        // Calculer l'expiration
        Timestamp expiry = new Timestamp(System.currentTimeMillis() + (TOKEN_EXPIRY_MINUTES * 60 * 1000L));

        // Nettoyer les anciennes requêtes pour cet utilisateur
        String deleteSql = "DELETE FROM `reset_password_request` WHERE user_id = ?";
        try (PreparedStatement dps = getConnection().prepareStatement(deleteSql)) {
            dps.setInt(1, user.getId());
            dps.executeUpdate();
        }

        // Insérer la nouvelle requête
        String sql = "INSERT INTO `reset_password_request` (selector, hashed_token, requested_at, expires_at, user_id) VALUES (?, ?, NOW(), ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "RESET"); // Sélecteur générique
            ps.setString(2, hashedToken);
            ps.setTimestamp(3, expiry);
            ps.setInt(4, user.getId());
            ps.executeUpdate();
        }

        // Envoyer l'email
        String userName = user.getPrenom() + " " + user.getNom();
        boolean sent = emailService.sendPasswordResetEmail(email, userName, token);

        AppLogger.auth("PASSWORD_RESET_REQUESTED", email + " | Table: reset_password_request");
        return sent;
    }

    /**
     * Valide un token de réinitialisation.
     */
    public boolean validateResetToken(String email, String token) throws SQLException {
        UserService userService = new UserService();
        User user = userService.findByEmail(email);
        if (user == null) return false;

        String sql = "SELECT hashed_token, expires_at FROM `reset_password_request` WHERE user_id = ? ORDER BY requested_at DESC LIMIT 1";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashedToken = rs.getString("hashed_token");
                    Timestamp expiry = rs.getTimestamp("expires_at");

                    // Vérifier si expiré
                    if (expiry.before(new Date())) {
                        return false;
                    }

                    // Vérifier le token (on utilise checkPassword car c'est un hash BCrypt)
                    return PasswordUtils.checkPassword(token, hashedToken);
                }
            }
        }
        return false;
    }

    /**
     * Réinitialise le mot de passe et nettoie la table.
     */
    public boolean resetPassword(String email, String token, String newPassword) throws SQLException {
        if (!validateResetToken(email, token)) {
            return false;
        }

        UserService userService = new UserService();
        User user = userService.findByEmail(email);
        if (user == null) return false;

        // Mettre à jour le mot de passe
        String hashedPwd = PasswordUtils.hashPassword(newPassword);
        userService.updatePasswordHash(user.getId(), hashedPwd);

        // Supprimer la requête utilisée
        String sql = "DELETE FROM `reset_password_request` WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }

        AppLogger.auth("PASSWORD_RESET_SUCCESS", email);
        return true;
    }

    /**
     * Génère un token numérique de 6 chiffres.
     */
    private String generateToken() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
