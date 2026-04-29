package tn.rouhfan.services;

import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.CoursReview;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Coursreviewservice {

    private final Connection cnx;

    public Coursreviewservice() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // ── Ajouter ou mettre à jour (INSERT OR REPLACE) ──────────────────────────

    /**
     * Si le participant a déjà noté ce cours → met à jour.
     * Sinon → insère. (équivalent upsert)
     */
    public void noterCours(int coursId, int participantId, int note) throws SQLException {
        // Vérifier si un avis existe déjà
        CoursReview existing = findByCoursAndParticipant(coursId, participantId);

        if (existing != null) {
            // Mettre à jour la note existante
            String sql = "UPDATE cours_review SET note = ?, date_review = NOW() WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, note);
                ps.setInt(2, existing.getId());
                ps.executeUpdate();
            }
        } else {
            // Nouvelle note
            String sql = "INSERT INTO cours_review (id_cours, id_participant, note, date_review) " +
                    "VALUES (?, ?, ?, NOW())";
            try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, coursId);
                ps.setInt(2, participantId);
                ps.setInt(3, note);
                ps.executeUpdate();
            }
        }
    }

    // ── Récupérer la note d'un participant pour un cours ──────────────────────

    public CoursReview findByCoursAndParticipant(int coursId, int participantId) throws SQLException {
        String sql = "SELECT * FROM cours_review WHERE id_cours = ? AND id_participant = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.setInt(2, participantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        }
        return null;
    }

    // ── Statistiques ──────────────────────────────────────────────────────────

    /** Moyenne des notes d'un cours (0.0 si aucune note) */
    public double getMoyenne(int coursId) throws SQLException {
        String sql = "SELECT AVG(note) AS moy FROM cours_review WHERE id_cours = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("moy");
            }
        }
        return 0.0;
    }

    /** Nombre total de notes d'un cours */
    public int getNombreNotes(int coursId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM cours_review WHERE id_cours = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        }
        return 0;
    }

    /** Distribution des notes (index 0 pour 1 étoile, index 4 pour 5 étoiles) */
    public int[] getDistributionNotes(int coursId) throws SQLException {
        int[] distrib = new int[5];
        String sql = "SELECT note, COUNT(*) AS cnt FROM cours_review WHERE id_cours = ? GROUP BY note";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int note = rs.getInt("note");
                    if (note >= 1 && note <= 5) {
                        distrib[note - 1] = rs.getInt("cnt");
                    }
                }
            }
        }
        return distrib;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private CoursReview mapper(ResultSet rs) throws SQLException {
        CoursReview r = new CoursReview();
        r.setId(rs.getInt("id"));
        r.setNote(rs.getInt("note"));
        r.setDateReview(rs.getTimestamp("date_review").toLocalDateTime());

        Cours c = new Cours();
        c.setId(rs.getInt("id_cours"));
        r.setCours(c);

        User u = new User();
        u.setId(rs.getInt("id_participant"));
        r.setParticipant(u);

        return r;
    }
}