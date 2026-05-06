package tn.rouhfan.services;

import tn.rouhfan.entities.ActivityLog;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Service de gestion des logs d'activité.
 *
 * Enregistre toutes les actions significatives dans la table activity_log :
 * - Connexions, inscriptions, déconnexions
 * - Appels API Hugging Face (sans exposer la clé)
 * - Erreurs et exceptions
 * - Actions utilisateur
 *
 * Fournit des méthodes de requête pour le dashboard admin :
 * - Filtrage par date, utilisateur, type d'action
 * - Statistiques d'utilisation IA
 * - Utilisateurs les plus actifs
 */
public class ActivityLogService {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ═══════════════════════════════════════
    //  Initialisation de la table
    // ═══════════════════════════════════════

    /**
     * Crée la table activity_log si elle n'existe pas encore.
     */
    public void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS activity_log (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL DEFAULT 0, " +
                "action_type VARCHAR(50) NOT NULL, " +
                "details TEXT, " +
                "timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "level VARCHAR(10) NOT NULL DEFAULT 'INFO', " +
                "INDEX idx_action_type (action_type), " +
                "INDEX idx_user_id (user_id), " +
                "INDEX idx_timestamp (timestamp)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement st = getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            AppLogger.error("Erreur création table activity_log", e);
        }
    }

    // ═══════════════════════════════════════
    //  Enregistrement des logs
    // ═══════════════════════════════════════

    /**
     * Enregistre un log d'activité (niveau INFO par défaut).
     *
     * @param userId     ID de l'utilisateur (0 si système)
     * @param actionType Type d'action (LOGIN, REGISTER, HUGGINGFACE_API_CALL, etc.)
     * @param details    Détails de l'action (sans données sensibles !)
     */
    public void log(int userId, String actionType, String details) {
        log(userId, actionType, details, "INFO");
    }

    /**
     * Enregistre un log d'activité avec un niveau spécifique.
     */
    public void log(int userId, String actionType, String details, String level) {
        ensureTableExists();
        String sql = "INSERT INTO activity_log (user_id, action_type, details, timestamp, level) " +
                "VALUES (?, ?, ?, NOW(), ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            // ⚠️ Nettoyage : ne jamais loguer de clé API ou de token
            ps.setString(3, sanitizeDetails(details));
            ps.setString(4, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("Erreur insertion activity_log", e);
        }
    }

    /**
     * Log une erreur.
     */
    public void logError(int userId, String actionType, String details) {
        log(userId, actionType, details, "ERROR");
    }

    /**
     * Log un avertissement.
     */
    public void logWarning(int userId, String actionType, String details) {
        log(userId, actionType, details, "WARN");
    }

    // ═══════════════════════════════════════
    //  Récupération des logs
    // ═══════════════════════════════════════

    /**
     * Récupère tous les logs, triés par date décroissante.
     */
    public List<ActivityLog> getAllLogs(int limit) {
        ensureTableExists();
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur récupération activity_log", e);
        }
        return logs;
    }

    /**
     * Récupère les logs filtrés par type d'action.
     */
    public List<ActivityLog> getLogsByActionType(String actionType, int limit) {
        ensureTableExists();
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_log WHERE action_type = ? ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, actionType);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur récupération logs par type", e);
        }
        return logs;
    }

    /**
     * Récupère les logs filtrés par utilisateur.
     */
    public List<ActivityLog> getLogsByUser(int userId, int limit) {
        ensureTableExists();
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_log WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";

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
     * Récupère les logs entre deux dates.
     */
    public List<ActivityLog> getLogsByDateRange(Date startDate, Date endDate, int limit) {
        ensureTableExists();
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_log WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(startDate.getTime()));
            ps.setTimestamp(2, new Timestamp(endDate.getTime()));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur récupération logs par date", e);
        }
        return logs;
    }

    /**
     * Recherche avancée avec filtres combinés.
     * Tous les paramètres sont optionnels (null = pas de filtre).
     */
    public List<ActivityLog> searchLogs(String actionType, Integer userId, Date startDate, Date endDate, int limit) {
        ensureTableExists();
        List<ActivityLog> logs = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM activity_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (actionType != null && !actionType.isEmpty()) {
            sql.append(" AND action_type = ?");
            params.add(actionType);
        }
        if (userId != null && userId > 0) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (startDate != null) {
            sql.append(" AND timestamp >= ?");
            params.add(new Timestamp(startDate.getTime()));
        }
        if (endDate != null) {
            sql.append(" AND timestamp <= ?");
            params.add(new Timestamp(endDate.getTime()));
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        params.add(limit);

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) ps.setString(i + 1, (String) param);
                else if (param instanceof Integer) ps.setInt(i + 1, (Integer) param);
                else if (param instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur recherche activity_log", e);
        }
        return logs;
    }

    // ═══════════════════════════════════════
    //  Statistiques pour le Dashboard Admin
    // ═══════════════════════════════════════

    /**
     * Compte le nombre total d'appels API Hugging Face.
     */
    public int countHuggingFaceApiCalls() {
        return countByActionType("HUGGINGFACE_API_CALL");
    }

    /**
     * Compte le nombre d'erreurs API Hugging Face.
     */
    public int countHuggingFaceErrors() {
        return countByActionType("HUGGINGFACE_API_ERROR");
    }

    /**
     * Compte le nombre d'actions d'un type donné.
     */
    public int countByActionType(String actionType) {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM activity_log WHERE action_type = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, actionType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur comptage activity_log", e);
        }
        return 0;
    }

    /**
     * Retourne le nombre total de logs.
     */
    public int countTotal() {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM activity_log";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            AppLogger.error("Erreur comptage total activity_log", e);
        }
        return 0;
    }

    /**
     * Retourne les utilisateurs les plus actifs (top N).
     * Renvoie une Map<userId, count>.
     */
    public Map<Integer, Integer> getMostActiveUsers(int limit) {
        ensureTableExists();
        Map<Integer, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT user_id, COUNT(*) as cnt FROM activity_log " +
                "WHERE user_id > 0 " +
                "GROUP BY user_id ORDER BY cnt DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("user_id"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur getMostActiveUsers", e);
        }
        return result;
    }

    /**
     * Retourne les types d'actions distincts (pour les filtres du dashboard).
     */
    public List<String> getDistinctActionTypes() {
        ensureTableExists();
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT action_type FROM activity_log ORDER BY action_type";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                types.add(rs.getString("action_type"));
            }
        } catch (SQLException e) {
            AppLogger.error("Erreur getDistinctActionTypes", e);
        }
        return types;
    }

    /**
     * Compte les erreurs totales (level = ERROR).
     */
    public int countErrors() {
        ensureTableExists();
        String sql = "SELECT COUNT(*) FROM activity_log WHERE level = 'ERROR'";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            AppLogger.error("Erreur countErrors activity_log", e);
        }
        return 0;
    }

    // ═══════════════════════════════════════
    //  Mapping et sécurité
    // ═══════════════════════════════════════

    private ActivityLog mapFromResultSet(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setActionType(rs.getString("action_type"));
        log.setDetails(rs.getString("details"));
        log.setTimestamp(rs.getTimestamp("timestamp"));
        log.setLevel(rs.getString("level"));
        return log;
    }

    /**
     * Nettoie les détails pour ne jamais stocker de données sensibles.
     * Supprime les clés API, tokens, mots de passe qui pourraient être présents.
     */
    private String sanitizeDetails(String details) {
        if (details == null) return null;
        // Supprimer tout ce qui ressemble à une clé API ou un token
        details = details.replaceAll("(hf_[A-Za-z0-9]{20,})", "[API_KEY_REDACTED]");
        details = details.replaceAll("(Bearer\\s+[A-Za-z0-9_\\-\\.]+)", "Bearer [REDACTED]");
        details = details.replaceAll("(password|passwd|pwd|token|secret|key)\\s*[:=]\\s*\\S+",
                "$1=[REDACTED]");
        return details;
    }
}
