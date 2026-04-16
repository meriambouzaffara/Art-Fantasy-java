package tn.rouhfan.entities;

import java.util.ArrayList;
import java.util.List;

public class Magasin {

    private Long id;
    private String nom;
    private String adresse;
    private String tel;
    private String email;
    private Double latitude;
    private Double longitude;
    private List<Article> articles;

    //  Constructeur sans ID
    public Magasin(String nom, String adresse, String tel, String email, Double latitude, Double longitude) {
        this.nom = nom;
        this.adresse = adresse;
        this.tel = tel;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.articles = new ArrayList<>();
    }

    //  Constructeur avec ID
    public Magasin(Long id, String nom, String adresse, String tel, String email, Double latitude, Double longitude) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.tel = tel;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.articles = new ArrayList<>();
    }

    public Magasin() {

    }

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom != null ? nom.trim() : null;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse != null ? adresse.trim() : null;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel != null ? tel.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : null;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    // toString()
    @Override
    public String toString() {
        return "Magasin{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", adresse='" + adresse + '\'' +
                ", tel='" + tel + '\'' +
                ", email='" + email + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}