package tn.rouhfan.services;

import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseReclamationService implements IService<ReponseReclamation> {

    Connection cnx;

    public ReponseReclamationService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // =========================
    // AJOUTER
    // =========================
    @Override
    public void ajouter(ReponseReclamation rr) throws SQLException {
        String sql = "INSERT INTO reponse_reclamation (message, created_at, reclamation_id) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, rr.getMessage());
        ps.setDate(2, new java.sql.Date(rr.getCreatedAt().getTime()));
        ps.setInt(3, rr.getReclamationId());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            rr.setId(rs.getInt(1));
        }

        System.out.println("✅ Réponse ajoutée en BD");
    }

    // =========================
    // SUPPRIMER
    // =========================
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reponse_reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("🗑️ Réponse supprimée");
    }

    // =========================
    // MODIFIER
    // =========================
    @Override
    public void modifier(ReponseReclamation rr) throws SQLException {
        String sql = "UPDATE reponse_reclamation SET message = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, rr.getMessage());
        ps.setInt(2, rr.getId());

        ps.executeUpdate();

        System.out.println("✏️ Réponse modifiée");
    }

    // =========================
    // RECUPERER TOUT
    // =========================
    @Override
    public List<ReponseReclamation> recuperer() throws SQLException {

        List<ReponseReclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_reclamation ORDER BY created_at DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            ReponseReclamation rr = new ReponseReclamation();
            rr.setId(rs.getInt("id"));
            rr.setMessage(rs.getString("message"));
            rr.setCreatedAt(rs.getDate("created_at"));
            rr.setReclamationId(rs.getInt("reclamation_id"));
            list.add(rr);
        }

        return list;
    }

    // =========================
    // RECUPERER PAR RECLAMATION
    // =========================
    public List<ReponseReclamation> getByReclamation(int reclamationId) throws SQLException {
        List<ReponseReclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_reclamation WHERE reclamation_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, reclamationId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ReponseReclamation rr = new ReponseReclamation();
            rr.setId(rs.getInt("id"));
            rr.setMessage(rs.getString("message"));
            rr.setCreatedAt(rs.getDate("created_at"));
            rr.setReclamationId(rs.getInt("reclamation_id"));
            list.add(rr);
        }

        return list;
    }

    // =========================
    // FIND BY ID
    // =========================
    @Override
    public ReponseReclamation findById(int id) throws SQLException {
        String sql = "SELECT * FROM reponse_reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            ReponseReclamation rr = new ReponseReclamation();
            rr.setId(rs.getInt("id"));
            rr.setMessage(rs.getString("message"));
            rr.setCreatedAt(rs.getDate("created_at"));
            rr.setReclamationId(rs.getInt("reclamation_id"));
            return rr;
        }

        return null;
    }
}