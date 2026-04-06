package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    Connection cnx;

    public UserService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // Ajouter
    public void ajouter(User u) throws SQLException {
        String sql = "INSERT INTO `user` (nom, prenom, email, password, roles, created_at, statut, is_verified, type) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPassword());
        ps.setString(5, u.getRoles());
        ps.setString(6, u.getStatut());
        ps.setBoolean(7, u.isVerified());
        ps.setString(8, u.getType());

        ps.executeUpdate();
        System.out.println("Utilisateur ajouté en BD");
    }

    // Récupérer tous les utilisateurs
    public List<User> recuperer() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            User u = new User();

            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setPassword(rs.getString("password"));
            u.setRoles(rs.getString("roles"));
            u.setCreatedAt(rs.getTimestamp("created_at"));
            u.setStatut(rs.getString("statut"));
            u.setVerified(rs.getBoolean("is_verified"));
            u.setType(rs.getString("type"));

            users.add(u);
        }

        return users;
    }

    // Supprimer
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("🗑️ Utilisateur supprimé");
    }

    // Modifier
    public void modifier(User u) throws SQLException {
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, statut=?, type=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getStatut());
        ps.setString(5, u.getType());
        ps.setInt(6, u.getId());

        ps.executeUpdate();
        System.out.println("✏️ Utilisateur modifié");
    }

    // Rechercher par email (login)
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            User u = new User();

            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setPassword(rs.getString("password"));
            u.setRoles(rs.getString("roles"));

            return u;
        }

        return null;
    }
}