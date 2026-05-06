package tn.rouhfan.entities;

import java.util.Date;

public class Reclamation {

    private int id;
    private String sujet; // au lieu de titre
    private String description;
    private String statut;
    private Date createdAt; // au lieu de dateCreation
    private int auteurId;
    private String categorie;
    private String imagePath;

    public Reclamation() {}

    public Reclamation(String sujet, String description, String statut, Date createdAt, int auteurId, String categorie) {
        this.sujet = sujet;
        this.description = description;
        this.statut = statut;
        this.createdAt = createdAt;
        this.auteurId = auteurId;
        this.categorie = categorie;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getAuteurId() { return auteurId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getType() {

        if (description == null && sujet == null) return "avis";

        String text = ((sujet != null ? sujet : "") + " " +
                (description != null ? description : "")).toLowerCase();

        // 🔴 mots de réclamation (problèmes)
        String[] reclamationWords = {
                "problème", "probleme", "bug", "erreur", "erreurs",
                "ne fonctionne pas", "cassé", "bloqué", "lent",
                "insatisfait", "plainte", "déçu", "decu",
                "pas marche", "ne marche pas", "crash","je peux pas","j'ai pas ","?"
        };

        // 🟢 mots d'avis positif
        String[] avisWords = {
                "bon", "excellent", "super", "bien", "parfait",
                "satisfait", "j'aime", "j aime", "top", "merci",
                "très bien", "tres bien","mauvaise","j'ai pas aimer"
        };

        // 🔍 check réclamation
        for (String word : reclamationWords) {
            if (text.contains(word)) {
                return "reclamation";
            }
        }

        // 🔍 check avis
        for (String word : avisWords) {
            if (text.contains(word)) {
                return "avis";
            }
        }

        // fallback par défaut
        return "avis";
    }


    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", sujet='" + sujet + '\'' +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                ", createdAt=" + createdAt +
                ", auteurId=" + auteurId +
                ", categorie='" + categorie + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }



}