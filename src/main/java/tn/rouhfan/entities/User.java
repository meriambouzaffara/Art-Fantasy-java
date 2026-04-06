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
    private String departement;
    private Date derniereConnexion;
    private String niveau;
    private String permissions;
    private String specialite;
    private String biographie;
    private String nationalite;
    private Date dateNaissance;
    private String siteWeb;
    private String reseauxSociaux;
    private Integer experience;
    private String role;
    private String evenement;
    private String description;
    private Date dateParticipation;
    private String categorie;
    private String production;
    private Double remuneration;
    private String statutParticipation;
    private String profileImage;
    private Date updatedAt;


    public User() {}

    // Constructeur principal
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

    // Getters & Setters

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

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public Date getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(Date derniereConnexion) { this.derniereConnexion = derniereConnexion; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getBiographie() { return biographie; }
    public void setBiographie(String biographie) { this.biographie = biographie; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public Date getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(Date dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getReseauxSociaux() { return reseauxSociaux; }
    public void setReseauxSociaux(String reseauxSociaux) { this.reseauxSociaux = reseauxSociaux; }

    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEvenement() { return evenement; }
    public void setEvenement(String evenement) { this.evenement = evenement; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDateParticipation() { return dateParticipation; }
    public void setDateParticipation(Date dateParticipation) { this.dateParticipation = dateParticipation; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getProduction() { return production; }
    public void setProduction(String production) { this.production = production; }

    public Double getRemuneration() { return remuneration; }
    public void setRemuneration(Double remuneration) { this.remuneration = remuneration; }

    public String getStatutParticipation() { return statutParticipation; }
    public void setStatutParticipation(String statutParticipation) { this.statutParticipation = statutParticipation; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // toString
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", statut='" + statut + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}