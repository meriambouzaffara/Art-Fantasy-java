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

        // 🔥 CHANGEMENT ICI → OpenRouter au lieu de HuggingFace
        ReclamationOpenRouterAIService aiService = new ReclamationOpenRouterAIService();

        String texte = r.getSujet() + " " + r.getDescription();

        String categorieAuto = aiService.categorize(texte);

        // 🔥 sécurité (très important)
        if (categorieAuto == null || categorieAuto.isEmpty()) {
            categorieAuto = "Autre";
        }

        System.out.println("Categorie détectée: " + categorieAuto);

        r.setCategorie(categorieAuto);

        String sql = "INSERT INTO reclamation (sujet, description, statut, created_at, auteur_id, categorie, image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, r.getSujet());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getStatut());
        ps.setDate(4, new Date(r.getCreatedAt().getTime()));
        ps.setInt(5, r.getAuteurId());
        ps.setString(6, r.getCategorie());
        ps.setString(7, r.getImagePath());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            r.setId(rs.getInt(1));
        }

        System.out.println("Reclamation ajoutée en BD");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("🗑️ Reclamation supprimée");
    }

    public void supprimerParAuteur(int auteurId) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE auteur_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, auteurId);
        ps.executeUpdate();
        System.out.println("🗑️ Toutes les reclamations de l'utilisateur supprimées");
    }

    @Override
    public void modifier(Reclamation reclamation) throws SQLException {
        // Optionnel
    }

    public void modifierStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE reclamation SET statut = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, statut);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public List<Reclamation> recupererParUser(int auteurId) throws SQLException {
        List<Reclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reclamation WHERE auteur_id = ? ORDER BY created_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, auteurId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Reclamation r = new Reclamation();
            r.setId(rs.getInt("id"));
            r.setSujet(rs.getString("sujet"));
            r.setDescription(rs.getString("description"));
            r.setStatut(rs.getString("statut"));
            r.setCreatedAt(rs.getDate("created_at"));
            r.setAuteurId(rs.getInt("auteur_id"));
            r.setCategorie(rs.getString("categorie"));
            r.setImagePath(rs.getString("image_path"));
            list.add(r);
        }

        return list;
    }

    @Override
    public List<Reclamation> recuperer() throws SQLException {
        List<Reclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reclamation ORDER BY created_at DESC";

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
            r.setImagePath(rs.getString("image_path"));
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
            r.setImagePath(rs.getString("image_path"));
            return r;
        }
        return null;
    }
}