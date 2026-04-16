package tn.rouhfan.entities;

import java.util.Date;

public class ReponseReclamation {

    private int id;
    private String message; // au lieu de contenu
    private Date createdAt;
    private int reclamationId;

    public ReponseReclamation() {}

    public ReponseReclamation(String message, Date createdAt, int reclamationId) {
        this.message = message;
        this.createdAt = createdAt;
        this.reclamationId = reclamationId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getReclamationId() { return reclamationId; }
    public void setReclamationId(int reclamationId) { this.reclamationId = reclamationId; }

    @Override
    public String toString() {
        return "ReponseReclamation{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                ", reclamationId=" + reclamationId +
                '}';
    }
}