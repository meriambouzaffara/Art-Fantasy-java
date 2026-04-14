package tn.rouhfan.services;

import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements IService<Reclamation> {

    Connection cnx;

    public ReclamationService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Reclamation r) throws SQLException {
        String sql = "INSERT INTO reclamation (sujet, description, statut, created_at, auteur_id, categorie) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, r.getSujet());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getStatut());
        ps.setDate(4, new java.sql.Date(r.getCreatedAt().getTime()));
        ps.setInt(5, r.getAuteurId());
        ps.setString(6, r.getCategorie());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            r.setId(rs.getInt(1));
        }

        System.out.println("Reclamation ajoutée en BD");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Supprimer d'abord les réponses
        ReponseReclamationService reponseService = new ReponseReclamationService();
        reponseService.supprimerParReclamation(id);

        String sql = "DELETE FROM reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("🗑️ Reclamation supprimée (ainsi que ses réponses)");
    }

    public void supprimerParAuteur(int auteurId) throws SQLException {
        // 1. Récupérer les IDs des réclamations de cet auteur
        List<Integer> reclamationIds = new ArrayList<>();
        String findIdsSql = "SELECT id FROM reclamation WHERE auteur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(findIdsSql)) {
            ps.setInt(1, auteurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reclamationIds.add(rs.getInt("id"));
                }
            }
        }

        // 2. Supprimer les réponses de ces réclamations
        ReponseReclamationService reponseService = new ReponseReclamationService();
        for (Integer id : reclamationIds) {
            reponseService.supprimerParReclamation(id);
        }

        // 3. Supprimer les réclamations
        String sql = "DELETE FROM reclamation WHERE auteur_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, auteurId);
        ps.executeUpdate();
        System.out.println("🗑️ Toutes les réclamations et réponses de l'utilisateur supprimées");
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLException {
        // Optionnel
    }

    @Override
    public List<Reclamation> recuperer() throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamation";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Reclamation r = new Reclamation();
            r.setId(rs.getInt("id"));
            r.setSujet(rs.getString("sujet"));
            r.setDescription(rs.getString("description"));
            r.setStatut(rs.getString("statut"));
            r.setCreatedAt(rs.getDate("created_at"));
            r.setAuteurId(rs.getInt("auteur_id"));
            r.setCategorie(rs.getString("categorie"));
            list.add(r);
        }
        return list;
    }

    @Override
    public Reclamation findById(int id) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Reclamation r = new Reclamation();
            r.setId(rs.getInt("id"));
            r.setSujet(rs.getString("sujet"));
            r.setDescription(rs.getString("description"));
            r.setStatut(rs.getString("statut"));
            r.setCreatedAt(rs.getDate("created_at"));
            r.setAuteurId(rs.getInt("auteur_id"));
            r.setCategorie(rs.getString("categorie"));
            return r;
        }
        return null;
    }
}