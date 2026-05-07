package tn.rouhfan.entities;

import java.util.Date;

/**
 * Entité représentant un log de connexion.
 * Chaque tentative de connexion (réussie ou échouée) est enregistrée.
 */
public class LoginLog {

    private int id;
    private int userId;
    private String email;
    private Date loginDate;
    private String ipAddress;
    private boolean success;
    private String failureReason;

    public LoginLog() {}

    public LoginLog(int userId, String email, boolean success, String ipAddress) {
        this.userId = userId;
        this.email = email;
        this.success = success;
        this.ipAddress = ipAddress;
        this.loginDate = new Date();
    }

    public LoginLog(int userId, String email, boolean success, String ipAddress, String failureReason) {
        this(userId, email, success, ipAddress);
        this.failureReason = failureReason;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getLoginDate() { return loginDate; }
    public void setLoginDate(Date loginDate) { this.loginDate = loginDate; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    @Override
    public String toString() {
        return "LoginLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", loginDate=" + loginDate +
                ", success=" + success +
                '}';
    }
}
