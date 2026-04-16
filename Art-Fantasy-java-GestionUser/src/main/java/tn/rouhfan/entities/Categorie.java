package tn.rouhfan.entities;

public class Categorie {

    private int idCategorie;
    private String nomCategorie;
    private String imageCategorie;

    public Categorie() {}

    public Categorie(String nomCategorie, String imageCategorie) {
        this.nomCategorie = nomCategorie;
        this.imageCategorie = imageCategorie;
    }

    public int getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }

    public String getImageCategorie() {
        return imageCategorie;
    }

    public void setImageCategorie(String imageCategorie) {
        this.imageCategorie = imageCategorie;
    }

    @Override
    public String toString() {
        return "Categorie{" +
                "idCategorie=" + idCategorie +
                ", nomCategorie='" + nomCategorie + '\'' +
                "}";
    }
}
