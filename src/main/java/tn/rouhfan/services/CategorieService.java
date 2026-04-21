package tn.rouhfan.services;

import tn.rouhfan.entities.Categorie;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {

    Connection cnx;

    public CategorieService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Categorie c) throws SQLException {
        // Test d'unicité sur le nom
        if (isNomExiste(c.getNomCategorie(), 0)) {
            throw new SQLException("Une catégorie avec ce nom existe déjà.");
        }

        String sql = "INSERT INTO categorie (nom_categorie, image_categorie) VALUES (?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getNomCategorie());
        ps.setString(2, c.getImageCategorie());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            c.setIdCategorie(rs.getInt(1));
        }

        System.out.println("Categorie ajoutée en BD");
    }

    public List<Categorie> recuperer() throws SQLException {
        List<Categorie> categories = new ArrayList<>();

        String sql = "SELECT id_categorie, nom_categorie, image_categorie FROM categorie";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Categorie c = new Categorie();
            c.setIdCategorie(rs.getInt("id_categorie"));
            c.setNomCategorie(rs.getString("nom_categorie"));
            c.setImageCategorie(rs.getString("image_categorie"));
            categories.add(c);
        }

        return categories;
    }

    public void supprimer(int idCategorie) throws SQLException {
        String sql = "DELETE FROM categorie WHERE id_categorie = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idCategorie);
        ps.executeUpdate();

        System.out.println("🗑️ Categorie supprimée");
    }

    public void modifier(Categorie c) throws SQLException {
        // Test d'unicité sur le nom (excluant la catégorie elle-même)
        if (isNomExiste(c.getNomCategorie(), c.getIdCategorie())) {
            throw new SQLException("Une autre catégorie avec ce nom existe déjà.");
        }

        String sql = "UPDATE categorie SET nom_categorie=?, image_categorie=? WHERE id_categorie=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getNomCategorie());
        ps.setString(2, c.getImageCategorie());
        ps.setInt(3, c.getIdCategorie());

        ps.executeUpdate();
        System.out.println(" Categorie modifiée");
    }

    public Categorie findById(int idCategorie) throws SQLException {
        String sql = "SELECT id_categorie, nom_categorie, image_categorie FROM categorie WHERE id_categorie = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idCategorie);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Categorie c = new Categorie();
            c.setIdCategorie(rs.getInt("id_categorie"));
            c.setNomCategorie(rs.getString("nom_categorie"));
            c.setImageCategorie(rs.getString("image_categorie"));
            return c;
        }

        return null;
    }

    public boolean isNomExiste(String nom, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie WHERE LOWER(nom_categorie) = LOWER(?) AND id_categorie != ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, nom.trim());
        ps.setInt(2, excludeId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }
}
