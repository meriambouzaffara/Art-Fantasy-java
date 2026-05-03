package tn.rouhfan.services;


import tn.rouhfan.entities.QcmResultat;
import tn.rouhfan.entities.Qcm;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QcmResultatService implements IService<QcmResultat> {

    private Connection cnx;

    public QcmResultatService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(QcmResultat res) throws SQLException {
        String sql = "INSERT INTO qcm_resultat (id_qcm, id_participant, score, valide, date_tentative) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, res.getQcm().getId());
            ps.setInt(2, res.getParticipant().getId());
            ps.setFloat(3, res.getScore());
            ps.setBoolean(4, res.isValide());
            // Conversion LocalDateTime -> Timestamp pour SQL
            ps.setTimestamp(5, Timestamp.valueOf(res.getDateTentative()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    res.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void modifier(QcmResultat res) throws SQLException {
        String sql = "UPDATE qcm_resultat SET score = ?, valide = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setFloat(1, res.getScore());
            ps.setBoolean(2, res.isValide());
            ps.setInt(3, res.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM qcm_resultat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<QcmResultat> recuperer() throws SQLException {
        List<QcmResultat> liste = new ArrayList<>();
        String sql = "SELECT * FROM qcm_resultat";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapper(rs));
            }
        }
        return liste;
    }

    @Override
    public QcmResultat findById(int id) throws SQLException {
        String sql = "SELECT * FROM qcm_resultat WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        }
        return null;
    }

    /**
     * Pour afficher l'historique d'un participant spécifique
     */
    public List<QcmResultat> recupererParParticipant(int userId) throws SQLException {
        List<QcmResultat> liste = new ArrayList<>();
        String sql = "SELECT * FROM qcm_resultat WHERE id_participant = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
        }
        return liste;
    }

    private QcmResultat mapper(ResultSet rs) throws SQLException {
        QcmResultat res = new QcmResultat();
        res.setId(rs.getInt("id"));
        res.setScore(rs.getFloat("score"));
        res.setValide(rs.getBoolean("valide"));
        res.setDateTentative(rs.getTimestamp("date_tentative").toLocalDateTime());

        // On charge les IDs pour les relations
        Qcm q = new Qcm(); q.setId(rs.getInt("id_qcm"));
        res.setQcm(q);

        User u = new User(); u.setId(rs.getInt("id_participant"));
        res.setParticipant(u);

        return res;
    }
}