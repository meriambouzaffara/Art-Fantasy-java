package tn.rouhfan.entities;

import java.util.Date;

public class Favoris {

    private int idFavoris;

    private User user;
    private Oeuvre oeuvre;

    private Date createdAt;

    public Favoris() {}

    public Favoris(User user, Oeuvre oeuvre, Date createdAt) {
        this.user = user;
        this.oeuvre = oeuvre;
        this.createdAt = createdAt;
    }

    public int getIdFavoris() {
        return idFavoris;
    }

    public void setIdFavoris(int idFavoris) {
        this.idFavoris = idFavoris;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Oeuvre getOeuvre() {
        return oeuvre;
    }

    public void setOeuvre(Oeuvre oeuvre) {
        this.oeuvre = oeuvre;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Favoris{" +
                "idFavoris=" + idFavoris +
                ", userId=" + (user != null ? user.getId() : null) +
                ", oeuvreId=" + (oeuvre != null ? oeuvre.getId() : null) +
                "}";
    }
}
