package tn.rouhfan.entities;

import java.util.Date;

public class Sponsor {

    private int id;
    private String nom;
    private String logo;
    private String description;
    private String email;
    private String tel;
    private String adresse;
    private Date createdAt;

    public Sponsor() {}

    public Sponsor(String nom, String logo, String description, String email, String tel, String adresse, Date createdAt) {
        this.nom = nom;
        this.logo = logo;
        this.description = description;
        this.email = email;
        this.tel = tel;
        this.adresse = adresse;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Sponsor{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}