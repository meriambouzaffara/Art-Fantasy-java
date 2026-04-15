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

        System.out.println("ReponseReclamation ajoutée en BD");
    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public void modifier(ReponseReclamation reponseReclamation) throws SQLException {

    }

    @Override
    public List<ReponseReclamation> recuperer() throws SQLException {
        List<ReponseReclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse_reclamation";
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

    @Override
    public ReponseReclamation findById(int id) throws SQLException {
        return null;
    }

    public void supprimerParReclamation(int reclamationId) throws SQLException {
        String sql = "DELETE FROM reponse_reclamation WHERE reclamation_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, reclamationId);
        ps.executeUpdate();
        System.out.println("🗑️ Toutes les réponses de la réclamation supprimées");
    }
}