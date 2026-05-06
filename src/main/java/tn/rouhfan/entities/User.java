package tn.rouhfan.entities;

import java.util.Date;

/**
 * Entité User enrichie avec :
 * - lastLoginAt : date de dernière connexion
 * - loginProvider : local / google / facebook / github
 * - verificationToken : token de vérification email
 * - resetToken : token de réinitialisation mot de passe
 * - resetTokenExpiry : date d'expiration du token de reset
 */
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
    private String photoProfile; // Chemin vers l'image de profil
    private String faceEmbedding; // Vecteur d'embedding facial (JSON)
    private boolean faceEnabled;  // Login facial activé

    // ── Nouveaux champs ──
    private Date lastLoginAt;
    private String loginProvider;       // "local", "google", "facebook", "github"
    private String verificationToken;
    private String resetToken;
    private Date resetTokenExpiry;

    public User() {
        this.loginProvider = "local";
    }

    public User(String nom, String prenom, String email, String password, String roles, String statut, boolean isVerified, String type) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.statut = statut;
        this.isVerified = isVerified;
        this.type = type;
        this.loginProvider = "local";
    }

    // ═══════════════════════════════════════
    //  Getters / Setters existants
    // ═══════════════════════════════════════

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
    
    public String getPhotoProfile() { return photoProfile; }
    public void setPhotoProfile(String photoProfile) { this.photoProfile = photoProfile; }

    public String getFaceEmbedding() { return faceEmbedding; }
    public void setFaceEmbedding(String faceEmbedding) { this.faceEmbedding = faceEmbedding; }

    public boolean isFaceEnabled() { return faceEnabled; }
    public void setFaceEnabled(boolean faceEnabled) { this.faceEnabled = faceEnabled; }

    // ═══════════════════════════════════════
    //  Nouveaux Getters / Setters
    // ═══════════════════════════════════════

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getLoginProvider() { return loginProvider; }
    public void setLoginProvider(String loginProvider) { this.loginProvider = loginProvider; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public Date getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(Date resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", statut='" + statut + '\'' +
                ", type='" + type + '\'' +
                ", provider='" + loginProvider + '\'' +
                '}';
    }
}