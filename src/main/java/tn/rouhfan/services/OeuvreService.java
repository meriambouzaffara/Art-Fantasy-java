package tn.rouhfan.services;

import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OeuvreService implements IService<Oeuvre> {

    Connection cnx;

    public OeuvreService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Oeuvre o) throws SQLException {
        // Test d'unicité sur titre + description + user_id
        if (isOeuvreExiste(o, 0)) {
            throw new SQLException("Une œuvre identique existe déjà pour cet artiste.");
        }

        String sql = "INSERT INTO oeuvre (description, titre, prix, statut, image, favori, date_vente, user_id, categorie_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, o.getDescription());
        ps.setString(2, o.getTitre());

        if (o.getPrix() != null)
            ps.setBigDecimal(3, o.getPrix());
        else
            ps.setNull(3, Types.DECIMAL);

        ps.setString(4, o.getStatut());
        ps.setString(5, o.getImage());

        if (o.getDateVente() != null)
            ps.setTimestamp(6, new Timestamp(o.getDateVente().getTime()));
        else
            ps.setNull(6, Types.TIMESTAMP);

        if (o.getUser() != null)
            ps.setInt(7, o.getUser().getId());
        else
            ps.setNull(7, Types.INTEGER);

        if (o.getCategorie() != null)
            ps.setInt(8, o.getCategorie().getIdCategorie());
        else
            ps.setNull(8, Types.INTEGER);

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            o.setId(rs.getInt(1));
        }

        System.out.println("Oeuvre ajoutée en BD");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM oeuvre WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("🗑️ Oeuvre supprimée");
    }

    @Override
    public void modifier(Oeuvre o) throws SQLException {
        // Test d'unicité sur titre + description + user_id (excluant l'œuvre elle-même)
        if (isOeuvreExiste(o, o.getId())) {
            throw new SQLException("Une autre œuvre identique existe déjà pour cet artiste.");
        }

        String sql = "UPDATE oeuvre SET description=?, titre=?, prix=?, statut=?, image=?, favori=?, date_vente=?, user_id=?, categorie_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getDescription());
        ps.setString(2, o.getTitre());

        if (o.getPrix() != null)
            ps.setBigDecimal(3, o.getPrix());
        else
            ps.setNull(3, Types.DECIMAL);

        ps.setString(4, o.getStatut());
        ps.setString(5, o.getImage());
        ps.setBoolean(6, o.isFavori());

        if (o.getDateVente() != null)
            ps.setTimestamp(7, new Timestamp(o.getDateVente().getTime()));
        else
            ps.setNull(7, Types.TIMESTAMP);

        if (o.getUser() != null)
            ps.setInt(8, o.getUser().getId());
        else
            ps.setNull(8, Types.INTEGER);

        if (o.getCategorie() != null)
            ps.setInt(9, o.getCategorie().getIdCategorie());
        else
            ps.setNull(9, Types.INTEGER);

        ps.setInt(10, o.getId());

        ps.executeUpdate();
        System.out.println("✏️ Oeuvre modifiée");
    }

    @Override
    public List<Oeuvre> recuperer() throws SQLException {
        List<Oeuvre> oeuvres = new ArrayList<>();

        String sql = "SELECT o.*, o.date_vente, " +
                "u.id AS u_id, u.nom AS u_nom, u.prenom AS u_prenom, " +
                "c.id_categorie AS c_id, c.nom_categorie AS c_nom " +
                "FROM oeuvre o " +
                "LEFT JOIN `user` u ON o.user_id = u.id " +
                "LEFT JOIN categorie c ON o.categorie_id = c.id_categorie";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Oeuvre o = new Oeuvre();

            o.setId(rs.getInt("id"));
            o.setDescription(rs.getString("description"));
            o.setTitre(rs.getString("titre"));

            BigDecimal prix = rs.getBigDecimal("prix");
            if (!rs.wasNull()) {
                o.setPrix(prix);
            }

            o.setStatut(rs.getString("statut"));
            o.setImage(rs.getString("image"));


            Timestamp ts = rs.getTimestamp("date_vente");
            if (ts != null) {
                o.setDateVente(ts);
            }

            int userId = rs.getInt("u_id");
            if (!rs.wasNull()) {
                User u = new User();
                u.setId(userId);
                u.setNom(rs.getString("u_nom"));
                u.setPrenom(rs.getString("u_prenom"));
                o.setUser(u);
            }

            int catId = rs.getInt("c_id");
            if (!rs.wasNull()) {
                Categorie c = new Categorie();
                c.setIdCategorie(catId);
                c.setNomCategorie(rs.getString("c_nom"));
                o.setCategorie(c);
            }

            oeuvres.add(o);
        }
        return oeuvres;
    }

    @Override
    public Oeuvre findById(int id) throws SQLException {
        String sql = "SELECT o.*, " +
                "u.id AS u_id, u.nom AS u_nom, u.prenom AS u_prenom, " +
                "c.id_categorie AS c_id, c.nom_categorie AS c_nom " +
                "FROM oeuvre o " +
                "LEFT JOIN `user` u ON o.user_id = u.id " +
                "LEFT JOIN categorie c ON o.categorie_id = c.id_categorie " +
                "WHERE o.id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Oeuvre o = new Oeuvre();

            o.setId(rs.getInt("id"));
            o.setDescription(rs.getString("description"));
            o.setTitre(rs.getString("titre"));

            BigDecimal prix = rs.getBigDecimal("prix");
            if (!rs.wasNull()) {
                o.setPrix(prix);
            }

            o.setStatut(rs.getString("statut"));
            o.setImage(rs.getString("image"));

            Timestamp ts = rs.getTimestamp("date_vente");
            if (ts != null) {
                o.setDateVente(ts);
            }

            int userId = rs.getInt("u_id");
            if (!rs.wasNull()) {
                User u = new User();
                u.setId(userId);
                u.setNom(rs.getString("u_nom"));
                u.setPrenom(rs.getString("u_prenom"));
                o.setUser(u);
            }

            int catId = rs.getInt("c_id");
            if (!rs.wasNull()) {
                Categorie c = new Categorie();
                c.setIdCategorie(catId);
                c.setNomCategorie(rs.getString("c_nom"));
                o.setCategorie(c);
            }

            return o;
        }
        return null;
    }

    private boolean isOeuvreExiste(Oeuvre o, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM oeuvre WHERE titre = ? AND description = ? AND user_id = ? AND id != ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        if (o.getUser() != null) {
            ps.setInt(3, o.getUser().getId());
        } else {
            ps.setNull(3, Types.INTEGER);
        }
        ps.setInt(4, excludeId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public void supprimerParUser(int userId) throws SQLException {
        String sql = "DELETE FROM oeuvre WHERE user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        int rows = ps.executeUpdate();
        if (rows > 0) {
            System.out.println("🗑️ " + rows + " Oeuvre(s) supprimée(s) pour l'utilisateur ID: " + userId);
        }
    }
}
