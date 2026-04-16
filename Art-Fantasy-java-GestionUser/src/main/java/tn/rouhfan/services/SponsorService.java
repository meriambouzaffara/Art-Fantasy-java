package tn.rouhfan.services;

import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SponsorService {

    Connection cnx;

    public SponsorService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // Ajouter
    public void ajouter(Sponsor s) throws SQLException {
        String sql = "INSERT INTO sponsor (nom, logo, description, email, tel, adresse, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, s.getNom());
        ps.setString(2, s.getLogo());
        ps.setString(3, s.getDescription());
        ps.setString(4, s.getEmail());
        ps.setString(5, s.getTel());
        ps.setString(6, s.getAdresse());

        ps.executeUpdate();

        // 🔥 récupérer ID auto généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            s.setId(rs.getInt(1));
        }

        System.out.println("Sponsor ajouté en BD");
    }

    // Récupérer
    public List<Sponsor> recuperer() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();

        String sql = "SELECT * FROM sponsor";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Sponsor s = new Sponsor();

            s.setId(rs.getInt("id"));
            s.setNom(rs.getString("nom"));
            s.setLogo(rs.getString("logo"));
            s.setDescription(rs.getString("description"));
            s.setEmail(rs.getString("email"));
            s.setTel(rs.getString("tel"));
            s.setAdresse(rs.getString("adresse"));
            s.setCreatedAt(rs.getTimestamp("created_at"));

            sponsors.add(s);
        }

        return sponsors;
    }

    // Supprimer
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM sponsor WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("🗑️ Sponsor supprimé");
    }

    // Modifier
    public void modifier(Sponsor s) throws SQLException {
        String sql = "UPDATE sponsor SET nom=?, email=?, tel=?, adresse=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, s.getNom());
        ps.setString(2, s.getEmail());
        ps.setString(3, s.getTel());
        ps.setString(4, s.getAdresse());
        ps.setInt(5, s.getId());

        ps.executeUpdate();
        System.out.println(" Sponsor modifié");
    }
}