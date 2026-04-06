package tn.rouhfan.services;

import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.Qcm;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QcmService implements IService<Qcm> {

    Connection cnx;

    public QcmService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Qcm q) throws SQLException {
        String sql = "INSERT INTO qcm (score_min_requis, duree_minutes, cours_id) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setFloat(1, q.getScoreMinRequis());
        ps.setInt(2, q.getDureeMinutes());
        ps.setInt(3, q.getCours().getId());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) q.setId(rs.getInt(1));
    }

    @Override
    public List<Qcm> recuperer() throws SQLException {
        List<Qcm> liste = new ArrayList<>();
        String sql = "SELECT q.*, c.nom AS cours_nom FROM qcm q LEFT JOIN cours c ON q.cours_id = c.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) liste.add(mapper(rs));
        return liste;
    }

    @Override
    public Qcm findById(int id) throws SQLException {
        String sql = "SELECT q.*, c.nom AS cours_nom FROM qcm q LEFT JOIN cours c ON q.cours_id = c.id WHERE q.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapper(rs);
        return null;
    }

    @Override
    public void modifier(Qcm q) throws SQLException {
        String sql = "UPDATE qcm SET score_min_requis=?, duree_minutes=?, cours_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setFloat(1, q.getScoreMinRequis());
        ps.setInt(2, q.getDureeMinutes());
        ps.setInt(3, q.getCours().getId());
        ps.setInt(4, q.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM qcm WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private Qcm mapper(ResultSet rs) throws SQLException {
        Qcm q = new Qcm();
        q.setId(rs.getInt("id"));
        q.setScoreMinRequis(rs.getFloat("score_min_requis"));
        q.setDureeMinutes(rs.getInt("duree_minutes"));

        Cours c = new Cours();
        c.setId(rs.getInt("cours_id"));
        c.setNom(rs.getString("cours_nom"));
        q.setCours(c);
        return q;
    }
}