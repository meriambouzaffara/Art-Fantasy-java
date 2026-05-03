package tn.rouhfan.entities;
import java.util.Date;

public class Progression {
    private int id;
    private float score;
    private boolean valide;
    private Date createdAt;
    private int niveau;

    // Remplacement de Participant par User
    private User participant;
    private Cours cours;

    public Progression() {
        this.score = 0.0f;
        this.createdAt = new Date();
    }

    public Progression(float score, boolean valide, Date createdAt,
                       int niveau, User participant, Cours cours) {
        this.score = score;
        this.valide = valide;
        this.createdAt = createdAt;
        this.niveau = niveau;
        this.participant = participant;
        this.cours = cours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public boolean isValide() { return valide; }
    public void setValide(boolean valide) { this.valide = valide; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }

    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    public Cours getCours() { return cours; }
    public void setCours(Cours cours) { this.cours = cours; }

    @Override
    public String toString() {
        return "Progression{" +
                "id=" + id +
                ", score=" + score +
                ", valide=" + valide +
                ", niveau=" + niveau +
                ", participant=" + (participant != null ? participant.getNom() : "null") +
                ", cours=" + (cours != null ? cours.getNom() : "null") +
                "}";
    }
}