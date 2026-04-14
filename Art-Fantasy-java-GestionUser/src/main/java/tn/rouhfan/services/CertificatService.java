package tn.rouhfan.services;

import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CertificatService implements IService<Certificat> {

    Connection cnx;

    public CertificatService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Certificat c) throws SQLException {
        // id_participant pointe vers user
        String sql = "INSERT INTO certificat (nom, niveau, score, id_cours, id_participant, date_obtention) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getNiveau());
        ps.setBigDecimal(3, c.getScore());
        ps.setInt(4, c.getCours().getId());
        ps.setInt(5, c.getParticipant().getId());
        ps.setDate(6, new java.sql.Date(c.getDateObtention().getTime()));
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next())
            c.setId(rs.getInt(1));
    }

    @Override
    public List<Certificat> recuperer() throws SQLException {
        List<Certificat> liste = new ArrayList<>();
        // Jointure sur la table 'user'
        String sql = "SELECT cert.*, co.nom AS cours_nom, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM certificat cert " +
                "LEFT JOIN cours co ON cert.id_cours = co.id " +
                "LEFT JOIN user u ON cert.id_participant = u.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next())
            liste.add(mapper(rs));
        return liste;
    }

    @Override
    public Certificat findById(int id) throws SQLException {
        String sql = "SELECT cert.*, co.nom AS cours_nom, u.nom AS user_nom, u.prenom AS user_prenom " +
                     "FROM certificat cert " +
                     "LEFT JOIN cours co ON cert.id_cours = co.id " +
                     "LEFT JOIN user u ON cert.id_participant = u.id " +
                     "WHERE cert.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next())
            return mapper(rs);
        return null;
    }

    @Override
    public void modifier(Certificat c) throws SQLException {
        String sql = "UPDATE certificat SET nom=?, niveau=?, score=?, id_cours=?, id_participant=?, date_obtention=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getNiveau());
        ps.setBigDecimal(3, c.getScore());
        ps.setInt(4, c.getCours().getId());
        ps.setInt(5, c.getParticipant().getId());
        ps.setDate(6, new java.sql.Date(c.getDateObtention().getTime()));
        ps.setInt(7, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM certificat WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Certificat mapper(ResultSet rs) throws SQLException {
        Certificat c = new Certificat();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setNiveau(rs.getString("niveau"));
        c.setScore(rs.getBigDecimal("score"));
        c.setDateObtention(rs.getDate("date_obtention"));

        Cours co = new Cours();
        co.setId(rs.getInt("id_cours"));
        co.setNom(rs.getString("cours_nom"));
        c.setCours(co);

        // Création d'un User pour le participant
        User u = new User();
        u.setId(rs.getInt("id_participant"));
        u.setNom(rs.getString("user_nom"));
        u.setPrenom(rs.getString("user_prenom"));
        c.setParticipant(u);
        return c;
    }

    public void supprimerParParticipant(int idParticipant) throws SQLException {
        String sql = "DELETE FROM certificat WHERE id_participant = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idParticipant);
        ps.executeUpdate();
        System.out.println("🗑️ Tous les certificats de l'utilisateur supprimés");
    }
}