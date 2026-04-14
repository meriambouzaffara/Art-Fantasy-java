package tn.rouhfan.entities;

import java.math.BigDecimal;
import java.util.Date;

public class Oeuvre {

    private int id;
    private String description;
    private String titre;
    private BigDecimal prix;
    private String statut;
    private String image;
    private boolean favori;
    private Date dateVente;

    // relations
    private User user;
    private Categorie categorie;

    public Oeuvre() {}

    public Oeuvre(String description, String titre, BigDecimal prix, String statut, String image, boolean favori, Date dateVente, User user, Categorie categorie) {
        this.description = description;
        this.titre = titre;
        this.prix = prix;
        this.statut = statut;
        this.image = image;
        this.favori = favori;
        this.dateVente = dateVente;
        this.user = user;
        this.categorie = categorie;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isFavori() {
        return favori;
    }

    public void setFavori(boolean favori) {
        this.favori = favori;
    }

    public Date getDateVente() {
        return dateVente;
    }

    public void setDateVente(Date dateVente) {
        this.dateVente = dateVente;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    @Override
    public String toString() {
        return "Oeuvre{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                "}";
    }
}
