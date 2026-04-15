package tn.rouhfan.services;

import tn.rouhfan.entities.QcmQuestion;
import tn.rouhfan.entities.QcmReponse;
import tn.rouhfan.tools.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QcmQuestionService implements IService<QcmQuestion> {

    private Connection cnx;

    public QcmQuestionService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(QcmQuestion q) throws SQLException {
        String sql = "INSERT INTO qcm_question (id_qcm, question) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, q.getQcm().getId());
            ps.setString(2, q.getQuestion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    q.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void modifier(QcmQuestion q) throws SQLException {
        String sql = "UPDATE qcm_question SET question = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, q.getQuestion());
            ps.setInt(2, q.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // ÉTAPE 1 : Supprimer manuellement les réponses pour éviter l'erreur #1451
        String sqlReponses = "DELETE FROM qcm_reponse WHERE id_question = ?";
        try (PreparedStatement psR = cnx.prepareStatement(sqlReponses)) {
            psR.setInt(1, id);
            psR.executeUpdate();
        }

        // ÉTAPE 2 : Supprimer la question
        String sqlQuestion = "DELETE FROM qcm_question WHERE id = ?";
        try (PreparedStatement psQ = cnx.prepareStatement(sqlQuestion)) {
            psQ.setInt(1, id);
            psQ.executeUpdate();
        }
    }

    public List<QcmQuestion> recupererParQcm(int qcmId) throws SQLException {
        List<QcmQuestion> liste = new ArrayList<>();
        String sql = "SELECT * FROM qcm_question WHERE id_qcm = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, qcmId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QcmQuestion q = new QcmQuestion();
                    q.setId(rs.getInt("id"));
                    q.setQuestion(rs.getString("question"));
                    liste.add(q);
                }
            }
        }
        return liste;
    }

    @Override
    public List<QcmQuestion> recuperer() throws SQLException { return new ArrayList<>(); }

    @Override
    public QcmQuestion findById(int id) throws SQLException { return null; }
}