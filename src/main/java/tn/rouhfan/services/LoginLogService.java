package tn.rouhfan.services;

import tn.rouhfan.entities.LoginLog;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;

import java.net.InetAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des logs de connexion.
 * Enregistre chaque tentative de connexion (réussie ou échouée)
 * avec date, IP, et raison d'échec éventuelle.
 */
public class LoginLogService {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Crée la table login_log si elle n'existe pas.
     */
    public void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS login_log (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "email VARCHAR(255) NOT NULL, " +
                "login_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "ip_address VARCHAR(45) DEFAULT 'localhost', " +
                "success BOOLEAN NOT NULL DEFAULT TRUE, " +
                "failure_reason VARCHAR(255) DEFAULT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement st = getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            AppLogger.error("Erreur création table login_log", e);
        }
    }

    /**
     * Enregistre un log de connexion réussie.
     */
    public void logSuccess(int userId, String email) {
        LoginLog log = new LoginLog(userId, email, true, getLocalIP());
        insert(log);
        AppLogger.auth("LOGIN_SUCCESS", email + " (ID:" + userId + ") depuis " + log.getIpAddress());
    }

    /**
     * Enregistre un log de connexion échouée.
     */
    public void logFailure(String email, String reason) {
        LoginLog log = new LoginLog(0, email, false, getLocalIP(), reason);
        insert(log);
        AppLogger.auth("LOGIN_FAILED", email + " | Raison: " + reason);
    }

    /**
     * Insère un log de connexion en base.
     */
    private void insert(LoginLog log) {
        ensureTableExists();
        String sql = "INSERT INTO login_log (user_id, email, login_date, ip_address, success, failure_reason) " +
                "VALUES (?, ?, NOW(), ?, ?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getEmail());
            ps.setString(3, log.getIpAddress());
            ps.setBoolean(4, log.isSuccess());
            ps.setString(5, log.getFailureReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("Erreur insertion login_log", e);
        }
    }

    /**
     * Récupère les N derniers logs de connexion.
     */
    public List<LoginLog> getRecentLogs(int limit) {
        ensureTableExists();
        List<LoginLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM login_log ORDER BY login_date DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur récupération login_log", e);
        }
        return logs;
    }

    /**
     * Récupère les logs de connexion d'un utilisateur spécifique.
     */
    public List<LoginLog> getLogsByUser(int userId, int limit) {
        ensureTableExists();
        List<LoginLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM login_log WHERE user_id = ? ORDER BY login_date DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur récupération logs utilisateur", e);
        }
        return logs;
    }

    /**
     * Compte le nombre total de connexions réussies.
     */
    public int countSuccessfulLogins() {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM login_log WHERE success = TRUE";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            AppLogger.error("Erreur comptage login_log", e);
        }
        return 0;
    }

    /**
     * Compte le nombre de tentatives échouées dans les dernières N minutes.
     */
    public int countRecentFailures(String email, int minutes) {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM login_log WHERE email = ? AND success = FALSE " +
                "AND login_date >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, minutes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur comptage échecs récents", e);
        }
        return 0;
    }

    /**
     * Récupère l'adresse IP locale de la machine.
     */
    private String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private LoginLog mapFromResultSet(ResultSet rs) throws SQLException {
        LoginLog log = new LoginLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setEmail(rs.getString("email"));
        log.setLoginDate(rs.getTimestamp("login_date"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setSuccess(rs.getBoolean("success"));
        log.setFailureReason(rs.getString("failure_reason"));
        return log;
    }
}
