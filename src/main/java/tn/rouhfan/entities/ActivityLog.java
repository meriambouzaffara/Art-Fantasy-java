package tn.rouhfan.entities;

import java.util.Date;

/**
 * Entité représentant un log d'activité dans l'application.
 *
 * Chaque action significative est enregistrée :
 * - Connexions / déconnexions (LOGIN, LOGOUT)
 * - Inscriptions (REGISTER)
 * - Appels API Hugging Face (HUGGINGFACE_API_CALL, HUGGINGFACE_API_ERROR)
 * - Erreurs système (SYSTEM_ERROR)
 * - Actions utilisateur (USER_ACTION)
 *
 * ⚠️ Les informations sensibles (clés API, tokens) ne sont JAMAIS stockées.
 */
public class ActivityLog {

    private int id;
    private int userId;
    private String actionType;     // LOGIN, REGISTER, HUGGINGFACE_API_CALL, etc.
    private String details;        // Description de l'action (sans données sensibles)
    private Date timestamp;
    private String level;          // INFO, WARN, ERROR

    public ActivityLog() {
        this.timestamp = new Date();
        this.level = "INFO";
    }

    public ActivityLog(int userId, String actionType, String details) {
        this();
        this.userId = userId;
        this.actionType = actionType;
        this.details = details;
    }

    public ActivityLog(int userId, String actionType, String details, String level) {
        this(userId, actionType, details);
        this.level = level;
    }

    // ═══════════════════════════════════════
    //  Getters / Setters
    // ═══════════════════════════════════════

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", actionType='" + actionType + '\'' +
                ", details='" + details + '\'' +
                ", timestamp=" + timestamp +
                ", level='" + level + '\'' +
                '}';
    }
}
