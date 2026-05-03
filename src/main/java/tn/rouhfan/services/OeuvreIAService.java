package tn.rouhfan.services;

import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OeuvreIAService {

    private Connection cnx;

    public OeuvreIAService() {
        cnx = MyDatabase.getInstance().getConnection();
        creerTableSiNonExiste();
    }

    private void creerTableSiNonExiste() {
        String sql = "CREATE TABLE IF NOT EXISTS oeuvre_ia (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "titre VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "image VARCHAR(255) NOT NULL, " +
                "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "user_id INT, " +
                "categorie_id INT, " +
                "FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (categorie_id) REFERENCES categorie(id_categorie) ON DELETE SET NULL" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            System.out.println("✅ Table oeuvre_ia vrifie/cre.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la cration automatique de la table oeuvre_ia : " + e.getMessage());
        }
    }

    public void ajouter(OeuvreIA o) throws SQLException {
        String sql = "INSERT INTO oeuvre_ia (titre, description, image, user_id, categorie_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setString(3, o.getImage());
        
        if (o.getUser() != null) ps.setInt(4, o.getUser().getId());
        else ps.setNull(4, Types.INTEGER);
        
        if (o.getCategorie() != null) ps.setInt(5, o.getCategorie().getIdCategorie());
        else ps.setNull(5, Types.INTEGER);

        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            o.setId(rs.getInt(1));
        }
    }

    public List<OeuvreIA> recupererParUser(int userId) throws SQLException {
        List<OeuvreIA> list = new ArrayList<>();
        String sql = "SELECT oi.*, c.nom_categorie FROM oeuvre_ia oi " +
                     "LEFT JOIN categorie c ON oi.categorie_id = c.id_categorie " +
                     "WHERE oi.user_id = ? ORDER BY oi.date_creation DESC";
        
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            OeuvreIA o = new OeuvreIA();
            o.setId(rs.getInt("id"));
            o.setTitre(rs.getString("titre"));
            o.setDescription(rs.getString("description"));
            o.setImage(rs.getString("image"));
            o.setDateCreation(rs.getTimestamp("date_creation"));
            
            Categorie c = new Categorie();
            c.setIdCategorie(rs.getInt("categorie_id"));
            c.setNomCategorie(rs.getString("nom_categorie"));
            o.setCategorie(c);
            
            list.add(o);
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM oeuvre_ia WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
