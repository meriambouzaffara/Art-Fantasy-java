package tn.rouhfan.services;

import tn.rouhfan.entities.User;
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
        String sql = "SELECT c.*, u.id AS artiste_id, u.nom AS artiste_nom, u.prenom AS artiste_prenom FROM cours c LEFT JOIN user u ON c.id_artiste = u.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) liste.add(mapper(rs));
        return liste;
    }

    // Ajoutez cette méthode dans CoursService.java
    public List<Cours> recupererFront() throws SQLException {
        List<Cours> liste = new ArrayList<>();
        String sql = "SELECT c.*, u.id AS artiste_id, u.nom AS artiste_nom, u.prenom AS artiste_prenom FROM cours c LEFT JOIN user u ON c.id_artiste = u.id WHERE c.statut = 'Publié'";
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
        // Désactiver temporairement les vérifications de clés étrangères
        cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

        try {
            // 1. Trouver le QCM associé à ce cours
            String findQcmSql = "SELECT id FROM qcm WHERE cours_id = ?";
            PreparedStatement psFindQcm = cnx.prepareStatement(findQcmSql);
            psFindQcm.setInt(1, id);
            ResultSet rsQcm = psFindQcm.executeQuery();

            if (rsQcm.next()) {
                int qcmId = rsQcm.getInt("id");
                rsQcm.close();
                psFindQcm.close();

                // 2. Trouver les questions de ce QCM
                String findQuestionsSql = "SELECT id FROM qcm_question WHERE id_qcm = ?";
                PreparedStatement psFindQuestions = cnx.prepareStatement(findQuestionsSql);
                psFindQuestions.setInt(1, qcmId);
                ResultSet rsQuestions = psFindQuestions.executeQuery();

                List<Integer> questionIds = new ArrayList<>();
                while (rsQuestions.next()) {
                    questionIds.add(rsQuestions.getInt("id"));
                }
                rsQuestions.close();
                psFindQuestions.close();

                // 3. Supprimer les réponses de chaque question
                String deleteReponseSql = "DELETE FROM qcm_reponse WHERE id_question = ?";
                PreparedStatement psDeleteReponse = cnx.prepareStatement(deleteReponseSql);
                for (int questionId : questionIds) {
                    psDeleteReponse.setInt(1, questionId);
                    psDeleteReponse.executeUpdate();
                }
                psDeleteReponse.close();

                // 4. Supprimer toutes les questions du QCM
                String deleteQuestionsSql = "DELETE FROM qcm_question WHERE id_qcm = ?";
                PreparedStatement psDeleteQuestions = cnx.prepareStatement(deleteQuestionsSql);
                psDeleteQuestions.setInt(1, qcmId);
                psDeleteQuestions.executeUpdate();
                psDeleteQuestions.close();

                // 5. Supprimer le QCM
                String deleteQcmSql = "DELETE FROM qcm WHERE id = ?";
                PreparedStatement psDeleteQcm = cnx.prepareStatement(deleteQcmSql);
                psDeleteQcm.setInt(1, qcmId);
                psDeleteQcm.executeUpdate();
                psDeleteQcm.close();
            } else {
                rsQcm.close();
                psFindQcm.close();
            }

            // 6. Enfin, supprimer le cours
            String deleteCoursSql = "DELETE FROM cours WHERE id = ?";
            PreparedStatement psDeleteCours = cnx.prepareStatement(deleteCoursSql);
            psDeleteCours.setInt(1, id);
            psDeleteCours.executeUpdate();
            psDeleteCours.close();

        } finally {
            // Réactiver les vérifications de clés étrangères
            cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    // Version alternative PLUS SIMPLE avec SET FOREIGN_KEY_CHECKS
    public void supprimerAvecCascade(int id) throws SQLException {
        // Désactiver temporairement les contraintes
        cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

        try {
            // Supprimer le cours (les enregistrements liés restent mais sans clé étrangère valide)
            String deleteCoursSql = "DELETE FROM cours WHERE id = ?";
            PreparedStatement ps = cnx.prepareStatement(deleteCoursSql);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();

            // Maintenant supprimer manuellement les QCMs orphelins
            String deleteOrphanQcmSql = "DELETE FROM qcm WHERE cours_id NOT IN (SELECT id FROM cours)";
            cnx.createStatement().execute(deleteOrphanQcmSql);

            // Supprimer les questions orphelines
            String deleteOrphanQuestionsSql = "DELETE FROM qcm_question WHERE id_qcm NOT IN (SELECT id FROM qcm)";
            cnx.createStatement().execute(deleteOrphanQuestionsSql);

            // Supprimer les réponses orphelines
            String deleteOrphanReponsesSql = "DELETE FROM qcm_reponse WHERE id_question NOT IN (SELECT id FROM qcm_question)";
            cnx.createStatement().execute(deleteOrphanReponsesSql);

        } finally {
            // Réactiver les contraintes
            cnx.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");
        }
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

        User u = new User();
        u.setId(rs.getInt("artiste_id"));
        u.setNom(rs.getString("artiste_nom"));
        u.setPrenom(rs.getString("artiste_prenom"));
        c.setArtiste(u);
        return c;
    }

    public void supprimerParArtiste(int idArtiste) throws SQLException {
        String sql = "SELECT id FROM cours WHERE id_artiste = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idArtiste);
        ResultSet rs = ps.executeQuery();

        List<Integer> coursIds = new ArrayList<>();
        while (rs.next()) {
            coursIds.add(rs.getInt("id"));
        }
        rs.close();
        ps.close();

        for (int coursId : coursIds) {
            supprimer(coursId);
        }
    }
}