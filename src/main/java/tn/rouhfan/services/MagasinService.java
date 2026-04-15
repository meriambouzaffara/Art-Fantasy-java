package tn.rouhfan.services;

import tn.rouhfan.entities.Magasin;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MagasinService implements IService<Magasin> {

    Connection cnx;

    public MagasinService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    //  Ajouter
    @Override
    public void ajouter(Magasin m) throws SQLException {
        String sql = "INSERT INTO magasin (nom, adresse, tel, email, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, m.getNom());
        ps.setString(2, m.getAdresse());
        ps.setString(3, m.getTel());
        ps.setString(4, m.getEmail());
        ps.setDouble(5, m.getLatitude());
        ps.setDouble(6, m.getLongitude());

        ps.executeUpdate();
        System.out.println(" Magasin ajouté");
    }

    //  Supprimer
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM magasin WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("🗑️ Magasin supprimé");
    }

    //  Modifier
    @Override
    public void modifier(Magasin m) throws SQLException {
        String sql = "UPDATE magasin SET nom=?, adresse=?, tel=?, email=?, latitude=?, longitude=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, m.getNom());
        ps.setString(2, m.getAdresse());
        ps.setString(3, m.getTel());
        ps.setString(4, m.getEmail());
        ps.setDouble(5, m.getLatitude());
        ps.setDouble(6, m.getLongitude());
        ps.setLong(7, m.getId());

        ps.executeUpdate();
        System.out.println("✏️ Magasin modifié");
    }

    //  Récupérer tous
    @Override
    public List<Magasin> recuperer() throws SQLException {
        List<Magasin> magasins = new ArrayList<>();

        String sql = "SELECT * FROM magasin";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Magasin m = new Magasin(
                    rs.getLong("id"),
                    rs.getString("nom"),
                    rs.getString("adresse"),
                    rs.getString("tel"),
                    rs.getString("email"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );

            magasins.add(m);
        }

        return magasins;
    }

    //  Find by ID
    @Override
    public Magasin findById(int id) throws SQLException {
        String sql = "SELECT * FROM magasin WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Magasin(
                    rs.getLong("id"),
                    rs.getString("nom"),
                    rs.getString("adresse"),
                    rs.getString("tel"),
                    rs.getString("email"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );
        }

        return null;
    }
}