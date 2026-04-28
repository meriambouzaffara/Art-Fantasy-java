package tn.rouhfan.entities;

import java.util.Date;

public class Evenement {

    private int id;
    private String titre;
    private String description;
    private String image;
    private String type;
    private String statut;
    private Date dateEvent;
    private String lieu;
    private Integer capacite;
    private int nbParticipants;
    private String googleEventId;

    private Sponsor sponsor;

    private int createurId;
    private User createur;

    public Evenement() {}

    public Evenement(String titre, String description, String image, String type,
                     String statut, Date dateEvent, String lieu, Integer capacite,
                     int nbParticipants, String googleEventId, Sponsor sponsor) {

        this.titre = titre;
        this.description = description;
        this.image = image;
        this.type = type;
        this.statut = statut;
        this.dateEvent = dateEvent;
        this.lieu = lieu;
        this.capacite = capacite;
        this.nbParticipants = nbParticipants;
        this.googleEventId = googleEventId;
        this.sponsor = sponsor;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatut() {
        if (dateEvent == null) return "PLANIFIÉ";
        
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate eventDate = dateEvent.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        
        if (eventDate.isAfter(today)) {
            return "PLANIFIÉ";
        } else if (eventDate.isEqual(today)) {
            return "EN COURS";
        } else {
            return "TERMINÉ";
        }
    }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getDateEvent() { return dateEvent; }
    public void setDateEvent(Date dateEvent) { this.dateEvent = dateEvent; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }

    public int getNbParticipants() { return nbParticipants; }
    public void setNbParticipants(int nbParticipants) { this.nbParticipants = nbParticipants; }

    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }

    public Sponsor getSponsor() { return sponsor; }
    public void setSponsor(Sponsor sponsor) { this.sponsor = sponsor; }

    public int getCreateurId() { return createurId; }
    public void setCreateurId(int createurId) { this.createurId = createurId; }

    public User getCreateur() { return createur; }
    public void setCreateur(User createur) { this.createur = createur; }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                ", lieu='" + lieu + '\'' +
                '}';
    }
}