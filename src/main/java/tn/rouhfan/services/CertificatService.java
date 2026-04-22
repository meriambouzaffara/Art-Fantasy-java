package tn.rouhfan.services;

import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CertificatService {

    private Connection cnx;

    public CertificatService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Certificat c) throws SQLException {
        String sql = "INSERT INTO certificat (nom, niveau, score, id_cours, id_participant, date_obtention) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getNiveau());
            ps.setBigDecimal(3, c.getScore());
            ps.setInt(4, c.getCours().getId());
            ps.setInt(5, c.getParticipant().getId());
            ps.setDate(6, new java.sql.Date(c.getDateObtention().getTime()));
            ps.executeUpdate();
        }
    }

    public void modifier(Certificat c) throws SQLException {
        String sql = "UPDATE certificat SET nom=?, niveau=?, score=?, id_cours=?, id_participant=?, date_obtention=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getNiveau());
            ps.setBigDecimal(3, c.getScore());
            ps.setInt(4, c.getCours().getId());
            ps.setInt(5, c.getParticipant().getId());
            ps.setDate(6, new java.sql.Date(c.getDateObtention().getTime()));
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM certificat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Certificat> recuperer() throws SQLException {
        List<Certificat> liste = new ArrayList<>();
        String sql = "SELECT c.*, co.nom as cours_nom, u.nom as user_nom, u.prenom as user_prenom " +
                "FROM certificat c " +
                "JOIN cours co ON c.id_cours = co.id " +
                "JOIN user u ON c.id_participant = u.id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Certificat cert = new Certificat();
                cert.setId(rs.getInt("id"));
                cert.setNom(rs.getString("nom"));
                cert.setNiveau(rs.getString("niveau"));
                cert.setScore(rs.getBigDecimal("score"));
                cert.setDateObtention(rs.getDate("date_obtention"));

                Cours co = new Cours();
                co.setId(rs.getInt("id_cours"));
                co.setNom(rs.getString("cours_nom"));
                cert.setCours(co);

                User u = new User();
                u.setId(rs.getInt("id_participant"));
                u.setNom(rs.getString("user_nom"));
                u.setPrenom(rs.getString("user_prenom"));
                cert.setParticipant(u);

                liste.add(cert);
            }
        }
        return liste;
    }
    public void supprimerParParticipant(int participantId) throws SQLException {
        String sql = "DELETE FROM certificat WHERE id_participant = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            int rows = ps.executeUpdate();

            System.out.println("Certificats supprimés pour participant #" + participantId + " : " + rows);
        }
    }
}