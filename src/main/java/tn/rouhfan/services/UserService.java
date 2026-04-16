package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;
import tn.rouhfan.tools.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    /**
     * Récupère une connexion fraîche à chaque appel pour éviter les connexions stale.
     */
    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // Ajouter
    public void ajouter(User u) throws SQLException {
        String sql = "INSERT INTO `user` (nom, prenom, email, password, roles, created_at, statut, is_verified, type) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?)";

        Connection cnx = getConnection();
        System.out.println("[UserService] ajouter() - Connexion active: " + (cnx != null && !cnx.isClosed()));

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPassword());
            ps.setString(5, u.getRoles());
            ps.setString(6, u.getStatut());
            ps.setBoolean(7, u.isVerified());
            ps.setString(8, u.getType());

            int rows = ps.executeUpdate();
            System.out.println("[UserService] INSERT exécuté - lignes affectées: " + rows);

            if (rows > 0) {
                // Récupérer l'ID généré
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        u.setId(keys.getInt(1));
                        System.out.println("[UserService] Utilisateur ajouté en BD avec ID: " + u.getId());
                    }
                }
            } else {
                System.err.println("[UserService] ERREUR: Aucune ligne insérée !");
            }

            // Forcer le commit si auto-commit est désactivé
            if (!cnx.getAutoCommit()) {
                cnx.commit();
                System.out.println("[UserService] Commit effectué manuellement");
            }
        } catch (SQLException e) {
            System.err.println("[UserService] ERREUR SQL lors de l'ajout: " + e.getMessage());
            System.err.println("[UserService] SQLState: " + e.getSQLState());
            System.err.println("[UserService] ErrorCode: " + e.getErrorCode());
            throw e;
        }
    }

    // Récupérer
    public List<User> recuperer() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM `user`";
        Connection cnx = getConnection();

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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
        }

        return users;
    }

    public void supprimer(int id) throws SQLException {
        Connection cnx = getConnection();

        // ── SUPPRESSION EN CASCADE MANUELLE ──

        // 1. Réclamations & Réponses
        ReclamationService reclamationService = new ReclamationService();
        reclamationService.supprimerParAuteur(id);

        // 2. Favoris (User)
        FavorisService favorisService = new FavorisService();
        favorisService.supprimerParUser(id);

        // 3. Certificats (User as Participant)
        CertificatService certificatService = new CertificatService();
        certificatService.supprimerParParticipant(id);

        // 4. Oeuvres (User as Artist) & leurs favoris
        OeuvreService oeuvreService = new OeuvreService();
        oeuvreService.supprimerParUser(id);

        // 5. Cours (User as Artist) & leurs certificats
        CoursService coursService = new CoursService();
        coursService.supprimerParArtiste(id);

        // ── FIN CASCADE ──

        String sql = "DELETE FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Utilisateur #" + id + " supprimé définitivement avec toutes ses données.");
            } else {
                System.out.println("⚠️ Aucun utilisateur trouvé avec l'ID: " + id);
            }
        }
    }

    // Modifier (Admin — met à jour tous les champs y compris roles)
    public void modifier(User u) throws SQLException {
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, roles=?, statut=?, type=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRoles());
            ps.setString(5, u.getStatut());
            ps.setString(6, u.getType());
            ps.setInt(7, u.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Utilisateur modifié");
            } else {
                System.out.println("Utilisateur introuvable");
            }
        }
    }

    /**
     * Modifier le mot de passe d'un utilisateur (Admin) — prend un password en clair et le hashe.
     */
    public void modifierPassword(int userId, String newPlainPassword) throws SQLException {
        String sql = "UPDATE `user` SET password=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, PasswordUtils.hashPassword(newPlainPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le hash du mot de passe directement (déjà hashé).
     * Utilisé quand le hash est calculé côté dialog.
     */
    public void updatePasswordHash(int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE `user` SET password=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Modifier le profil personnel d'un utilisateur (nom, prénom, email).
     */
    public void modifierProfil(User u) throws SQLException {
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setInt(4, u.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Profil modifié avec succès");
            }
        }
    }

    // Rechercher par ID
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE id = ?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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

                    return u;
                }
            }
        }

        return null;
    }

    // Rechercher par email
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email = ?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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

                    return u;
                }
            }
        }

        return null;
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     * Supporte BCrypt ET mots de passe en clair (compatibilité).
     * @return l'objet User si authentification réussie, null sinon
     */
    public User authenticate(String email, String password) throws SQLException {
        User user = findByEmail(email);
        if (user != null && PasswordUtils.checkPassword(password, user.getPassword())) {
            return user;
        }
        return null;
    }
}