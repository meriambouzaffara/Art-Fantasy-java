package tn.rouhfan.services;

import tn.rouhfan.entities.User; // ✅ Import User
import tn.rouhfan.entities.Cours;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements IService<Cours> {

    Connection cnx;

    public CoursService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Cours c) throws SQLException {
        //  Utilisation de id_artiste pointant vers la table user
        String sql = "INSERT INTO cours (nom, description, niveau, duree, statut, contenu, id_artiste) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getNiveau());
        ps.setString(4, c.getDuree());
        ps.setString(5, c.getStatut());
        ps.setString(6, c.getContenu());
        ps.setInt(7, c.getArtiste().getId());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) c.setId(rs.getInt(1));
    }

    @Override
    public List<Cours> recuperer() throws SQLException {
        List<Cours> liste = new ArrayList<>();
        //  Jointure sur la table 'user'
        String sql = "SELECT c.*, u.id AS artiste_id, u.nom AS artiste_nom, u.prenom AS artiste_prenom FROM cours c LEFT JOIN user u ON c.id_artiste = u.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) liste.add(mapper(rs));
        return liste;
    }

    @Override
    public Cours findById(int id) throws SQLException {
        String sql = "SELECT c.*, u.id AS artiste_id, u.nom AS artiste_nom, u.prenom AS artiste_prenom FROM cours c LEFT JOIN user u ON c.id_artiste = u.id WHERE c.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapper(rs);
        return null;
    }

    @Override
    public void modifier(Cours c) throws SQLException {
        String sql = "UPDATE cours SET nom=?, description=?, niveau=?, duree=?, statut=?, contenu=?, id_artiste=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setString(3, c.getNiveau());
        ps.setString(4, c.getDuree());
        ps.setString(5, c.getStatut());
        ps.setString(6, c.getContenu());
        ps.setInt(7, c.getArtiste().getId());
        ps.setInt(8, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM cours WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Cours mapper(ResultSet rs) throws SQLException {
        Cours c = new Cours();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        c.setNiveau(rs.getString("niveau"));
        c.setDuree(rs.getString("duree"));
        c.setStatut(rs.getString("statut"));
        c.setContenu(rs.getString("contenu"));

        // Création d'un User pour l'artiste
        User u = new User();
        u.setId(rs.getInt("artiste_id"));
        u.setNom(rs.getString("artiste_nom"));
        u.setPrenom(rs.getString("artiste_prenom"));
        c.setArtiste(u);
        return c;
    }
    public void supprimerParArtiste(int idArtiste) throws SQLException {
        String sql = "DELETE FROM cours WHERE id_artiste = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idArtiste);
        ps.executeUpdate();
    }
}