package tn.rouhfan.entities;

import java.util.Date;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String roles;
    private Date createdAt;
    private String statut;
    private boolean isVerified;
    private String type;

    public User() {}

    public User(String nom, String prenom, String email, String password, String roles, String statut, boolean isVerified, String type) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.statut = statut;
        this.isVerified = isVerified;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {

        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }
}