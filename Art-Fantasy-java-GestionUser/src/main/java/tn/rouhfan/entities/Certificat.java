package tn.rouhfan.entities;

import java.util.Date;
import java.math.BigDecimal;

public class Certificat {
    private int id;
    private String nom;
    private String niveau;
    private BigDecimal score;
    private Date dateObtention;

    private Cours cours;
    //  Remplacement de Participant par User
    private User participant;

    public Certificat() {}

    public Certificat(String nom, String niveau, BigDecimal score,
                      Date dateObtention, Cours cours, User participant) {
        this.nom = nom;
        this.niveau = niveau;
        this.score = score;
        this.dateObtention = dateObtention;
        this.cours = cours;
        this.participant = participant;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Date getDateObtention() { return dateObtention; }
    public void setDateObtention(Date dateObtention) { this.dateObtention = dateObtention; }

    public Cours getCours() { return cours; }
    public void setCours(Cours cours) { this.cours = cours; }

    //  Getter/Setter mis à jour
    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    @Override
    public String toString() {
        return "Certificat{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", niveau='" + niveau + '\'' +
                ", score=" + score +
                ", cours=" + (cours != null ? cours.getNom() : "null") +
                ", participant=" + (participant != null ? (participant.getNom() + " " + participant.getPrenom()) : "null") +
                "}";
    }
}