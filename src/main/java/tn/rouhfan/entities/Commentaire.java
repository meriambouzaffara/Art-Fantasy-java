package tn.rouhfan.entities;

import java.util.Date;

public class Commentaire {
    private int id;
    private String contenu;
    private Date dateCommentaire;
    private int oeuvreId;
    private int userId;
    private Integer parentCommentId; // Nullable if it's a top-level comment
    
    // For convenience in UI
    private String userName;
    private String userType;
    private String oeuvreTitre;

    public Commentaire() {}

    public Commentaire(String contenu, int oeuvreId, int userId) {
        this.contenu = contenu;
        this.oeuvreId = oeuvreId;
        this.userId = userId;
        this.dateCommentaire = new Date();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Date getDateCommentaire() { return dateCommentaire; }
    public void setDateCommentaire(Date dateCommentaire) { this.dateCommentaire = dateCommentaire; }

    public int getOeuvreId() { return oeuvreId; }
    public void setOeuvreId(int oeuvreId) { this.oeuvreId = oeuvreId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Integer parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getOeuvreTitre() { return oeuvreTitre; }
    public void setOeuvreTitre(String oeuvreTitre) { this.oeuvreTitre = oeuvreTitre; }
}
