package tn.rouhfan.entities;

import java.time.LocalDateTime;

public class Article {

    private Long idArticle;
    private String titre;
    private String reference;
    private double prix;
    private Integer stock;
    private String description;
    private LocalDateTime createdAt;
    private String image;
    private Magasin magasin;

    // ✅ Constructeur sans ID
    public Article(String titre, double prix, Integer stock, String description,
                   LocalDateTime createdAt, String image, Magasin magasin) {
        this.titre = titre;
        this.prix = prix;
        this.stock = stock;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.image = image;
        this.magasin = magasin;
    }

    // ✅ Constructeur avec ID
    public Article(Long idArticle, String titre, double prix, Integer stock, String description,
                   LocalDateTime createdAt, String image, Magasin magasin) {
        this.idArticle = idArticle;
        this.titre = titre;
        this.prix = prix;
        this.stock = stock;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.image = image;
        this.magasin = magasin;
    }

    // ===== Getters & Setters =====

    public Long getIdArticle() {
        return idArticle;
    }

    public void setIdArticle(Long idArticle) {
        this.idArticle = idArticle;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre != null ? titre.trim() : null;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference != null && !reference.isBlank() ? reference.trim() : null;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    // toString()
    @Override
    public String toString() {
        return "Article{" +
                "idArticle=" + idArticle +
                ", titre='" + titre + '\'' +
                ", prix=" + prix +
                ", stock=" + stock +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", image='" + image + '\'' +
                '}';
    }
}