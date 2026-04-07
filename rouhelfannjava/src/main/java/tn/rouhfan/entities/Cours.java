package tn.rouhfan.entities;

public class Cours {
    private int id;
    private String nom;
    private String description;
    private String niveau;
    private String duree;
    private String statut;
    private String contenu;

    // ✅ Remplacement de Artiste par User
    private User artiste;

    public Cours() {}

    public Cours(String nom, String description, String niveau,
                 String duree, String statut, String contenu, User artiste) {
        this.nom = nom;
        this.description = description;
        this.niveau = niveau;
        this.duree = duree;
        this.statut = statut;
        this.contenu = contenu;
        this.artiste = artiste;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public String getDuree() { return duree; }
    public void setDuree(String duree) { this.duree = duree; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    // ✅ Getter/Setter mis à jour
    public User getArtiste() { return artiste; }
    public void setArtiste(User artiste) { this.artiste = artiste; }

    @Override
    public String toString() {
        return "Cours{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", niveau='" + niveau + '\'' +
                ", artiste=" + (artiste != null ? (artiste.getNom() + " " + artiste.getPrenom()) : "null") +
                "}";
    }
}