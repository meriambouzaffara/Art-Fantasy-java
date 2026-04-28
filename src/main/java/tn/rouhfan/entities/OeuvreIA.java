package tn.rouhfan.entities;

import java.util.Date;

public class OeuvreIA {
    private int id;
    private String titre;
    private String description;
    private String image;
    private Date dateCreation;
    private User user;
    private Categorie categorie;

    public OeuvreIA() {}

    public OeuvreIA(String titre, String description, String image, User user, Categorie categorie) {
        this.titre = titre;
        this.description = description;
        this.image = image;
        this.user = user;
        this.categorie = categorie;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    @Override
    public String toString() {
        return "OeuvreIA{" + "id=" + id + ", titre='" + titre + '\'' + '}';
    }
}
