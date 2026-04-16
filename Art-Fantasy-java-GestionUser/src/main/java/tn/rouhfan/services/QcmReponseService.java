package tn.rouhfan.services;

import tn.rouhfan.entities.QcmReponse;
import tn.rouhfan.tools.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QcmReponseService {
    private Connection cnx;

    public QcmReponseService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public List<QcmReponse> recupererParQuestion(int questionId) throws SQLException {
        List<QcmReponse> liste = new ArrayList<>();
        String sql = "SELECT * FROM qcm_reponse WHERE id_question = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QcmReponse r = new QcmReponse();
                    r.setId(rs.getInt("id"));
                    r.setTexte(rs.getString("texte"));
                    r.setCorrecte(rs.getBoolean("correcte"));
                    liste.add(r);
                }
            }
        }
        return liste;
    }
}