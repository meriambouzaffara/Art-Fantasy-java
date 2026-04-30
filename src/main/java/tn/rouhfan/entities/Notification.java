package tn.rouhfan.entities;

import java.sql.Timestamp;

public class Notification {

    private int id;
    private int userId; // Destinataire (L'artiste)
    private int oeuvreId; // L'œuvre concernée
    private String message;
    private boolean lu;
    private Timestamp dateCreation;

    public Notification() {}

    public Notification(int userId, String message, int oeuvreId) {
        this.userId = userId;
        this.message = message;
        this.oeuvreId = oeuvreId;
        this.lu = false;
        this.dateCreation = new Timestamp(System.currentTimeMillis());
    }

    public Notification(int id, int userId, String message, boolean lu, Timestamp dateCreation, int oeuvreId) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.lu = lu;
        this.dateCreation = dateCreation;
        this.oeuvreId = oeuvreId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getOeuvreId() {
        return oeuvreId;
    }

    public void setOeuvreId(int oeuvreId) {
        this.oeuvreId = oeuvreId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }
}
