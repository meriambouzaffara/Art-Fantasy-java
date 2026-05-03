package tn.rouhfan.services;

import tn.rouhfan.entities.Progression;
import tn.rouhfan.entities.User;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgressionService implements IService<Progression> {

    private Connection cnx;

    public ProgressionService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Progression p) throws SQLException {
        String sql = "INSERT INTO progression (score, valide, created_at, niveau, id_participant, id_cours) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setFloat(1, p.getScore());
            ps.setBoolean(2, p.isValide());
            ps.setTimestamp(3, new Timestamp(p.getCreatedAt().getTime()));
            ps.setInt(4, p.getNiveau());
            ps.setInt(5, p.getParticipant().getId());
            ps.setInt(6, p.getCours().getId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void modifier(Progression p) throws SQLException {
        String sql = "UPDATE progression SET score = ?, valide = ?, niveau = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setFloat(1, p.getScore());
            ps.setBoolean(2, p.isValide());
            ps.setInt(3, p.getNiveau());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM progression WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Progression> recuperer() throws SQLException {
        List<Progression> liste = new ArrayList<>();
        String sql = "SELECT * FROM progression";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapper(rs));
            }
        }
        return liste;
    }

    @Override
    public Progression findById(int id) throws SQLException {
        String sql = "SELECT * FROM progression WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        }
        return null;
    }

    /**
     * Utile pour vérifier si un utilisateur a déjà validé un niveau spécifique
     */
    public List<Progression> recupererParParticipantEtCours(int userId, int coursId) throws SQLException {
        List<Progression> liste = new ArrayList<>();
        String sql = "SELECT * FROM progression WHERE id_participant = ? AND id_cours = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
        }
        return liste;
    }

    private Progression mapper(ResultSet rs) throws SQLException {
        Progression p = new Progression();
        p.setId(rs.getInt("id"));
        p.setScore(rs.getFloat("score"));
        p.setValide(rs.getBoolean("valide"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setNiveau(rs.getInt("niveau"));

        // On charge les IDs minimaux pour les relations
        User u = new User(); u.setId(rs.getInt("id_participant"));
        p.setParticipant(u);

        Cours c = new Cours(); c.setId(rs.getInt("id_cours"));
        p.setCours(c);

        return p;
    }
}