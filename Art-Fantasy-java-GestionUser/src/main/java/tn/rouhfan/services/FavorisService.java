package tn.rouhfan.services;

import tn.rouhfan.entities.Favoris;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavorisService {

    Connection cnx;

    public FavorisService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(int userId, int oeuvreId) throws SQLException {
        String sql = "INSERT INTO favoris (id_user, oeuvre_id, created_at) VALUES (?, ?, NOW())";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ps.executeUpdate();
        System.out.println("Favori ajouté en BD");
    }

    public void supprimer(int userId, int oeuvreId) throws SQLException {
        String sql = "DELETE FROM favoris WHERE id_user = ? AND oeuvre_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ps.executeUpdate();
        System.out.println("🗑️ Favori supprimé");
    }

    public boolean exists(int userId, int oeuvreId) throws SQLException {
        String sql = "SELECT 1 FROM favoris WHERE id_user = ? AND oeuvre_id = ? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public List<Favoris> recupererParUser(int userId) throws SQLException {
        List<Favoris> favoris = new ArrayList<>();

        String sql = "SELECT f.id_favoris, f.created_at, " +
                "u.id AS u_id, u.nom AS u_nom, u.prenom AS u_prenom, " +
                "o.id AS o_id, o.titre AS o_titre " +
                "FROM favoris f " +
                "JOIN `user` u ON f.id_user = u.id " +
                "JOIN oeuvre o ON f.oeuvre_id = o.id " +
                "WHERE f.id_user = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Favoris f = new Favoris();
            f.setIdFavoris(rs.getInt("id_favoris"));
            f.setCreatedAt(rs.getTimestamp("created_at"));

            User u = new User();
            u.setId(rs.getInt("u_id"));
            u.setNom(rs.getString("u_nom"));
            u.setPrenom(rs.getString("u_prenom"));
            f.setUser(u);

            Oeuvre o = new Oeuvre();
            o.setId(rs.getInt("o_id"));
            o.setTitre(rs.getString("o_titre"));
            f.setOeuvre(o);

            favoris.add(f);
        }

        return favoris;
    }

    public void toggle(int userId, int oeuvreId) throws SQLException {
        if (exists(userId, oeuvreId)) {
            supprimer(userId, oeuvreId);
        } else {
            ajouter(userId, oeuvreId);
        }
    }
    public void supprimerParUser(int userId) throws SQLException {
        String sql = "DELETE FROM favoris WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }
}
