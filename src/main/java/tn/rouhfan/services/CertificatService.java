package tn.rouhfan.services;

import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.Progression;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.math.BigDecimal;
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
            ps.setDate(6, new Date(c.getDateObtention().getTime()));
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
            ps.setDate(6, new Date(c.getDateObtention().getTime()));
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

    public void supprimerParParticipant(int participantId) throws SQLException {
        String sql = "DELETE FROM certificat WHERE id_participant = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participantId);
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


    public void handleProgression(Progression progression) throws SQLException {
        if (progression == null || !progression.isValide()) {
            return;
        }

        User participant = progression.getParticipant();
        Cours cours = progression.getCours();

        // 1. Vérifier si l'utilisateur possède déjà ce certificat (par nom exact du cours)
        if (checkIfCertificatExistsByName(participant.getId(), cours.getNom())) {
            System.out.println("INFO: L'utilisateur a déjà obtenu ce certificat.");
            return;
        }

        // 2. Vérifier que l'utilisateur a validé les 3 niveaux (1, 2 ET 3)
        //    pour tous les cours partageant le même nom de base (ex: "Peinture")
        boolean tousNiveauxValides = hasValidatedAllThreeLevels(participant.getId(), cours.getNom());

        System.out.println("LOG CERTIFICAT -> Participant ID: " + participant.getId()
                + " | Cours: " + cours.getNom()
                + " | Tous niveaux validés: " + tousNiveauxValides);

        // 3. CONDITION STRICTE : niveaux 1, 2 ET 3 doivent tous être validés
        if (tousNiveauxValides) {
            genererCertificatAutomatique(progression);
        } else {
            System.out.println("LOG: Il manque encore des niveaux pour obtenir le certificat.");
        }
    }

    /**
     * Vérifie que l'utilisateur a validé les niveaux 1, 2 ET 3
     * pour l'ensemble des cours ayant le même nom de base (même sujet).
     * Chaque niveau peut être un cours séparé dans la BDD.
     */
    private boolean hasValidatedAllThreeLevels(int userId, String nomCours) throws SQLException {
        // On cherche les niveaux distincts validés parmi tous les cours du même nom
        String sql = "SELECT COUNT(DISTINCT p.niveau) FROM progression p " +
                "JOIN cours c ON p.id_cours = c.id " +
                "WHERE p.id_participant = ? AND c.nom = ? AND p.valide = 1 " +
                "AND p.niveau IN (1, 2, 3)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, nomCours);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("LOG: Niveaux distincts validés pour '" + nomCours + "': " + count + "/3");
                    return count >= 3;
                }
            }
        }
        return false;
    }

    private boolean checkIfCertificatExistsByName(int userId, String nomCours) throws SQLException {
        // Recherche dans la table certificat si un titre contient déjà le nom du cours
        String sql = "SELECT COUNT(*) FROM certificat WHERE id_participant = ? AND nom LIKE ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + nomCours + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void genererCertificatAutomatique(Progression p) throws SQLException {
        Certificat cert = new Certificat();
        cert.setNom("Certificat de Maîtrise - " + p.getCours().getNom());
        cert.setNiveau("Expert");
        cert.setScore(BigDecimal.valueOf(p.getScore()));
        cert.setDateObtention(new java.util.Date());
        cert.setCours(p.getCours());
        cert.setParticipant(p.getParticipant());

        this.ajouter(cert);
        System.out.println("✨ SUCCÈS : Certificat généré pour " + p.getParticipant().getNom());
    }


}