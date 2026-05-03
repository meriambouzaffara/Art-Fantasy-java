package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.MyDatabase;
import tn.rouhfan.tools.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des utilisateurs enrichi.
 * CRUD + Authentification + Statistiques + Requêtes avancées.
 */
public class UserService {

    /**
     * Récupère une connexion fraîche à chaque appel pour éviter les connexions stale.
     */
    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ════════════════════════════════════════
    //  CRUD EXISTANT
    // ════════════════════════════════════════

    public void ajouter(User u) throws SQLException {
        ensurePhotoColumn();
        String sql = "INSERT INTO `user` (nom, prenom, email, password, roles, created_at, statut, is_verified, type, photo_profile) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPassword());
            ps.setString(5, u.getRoles());
            ps.setString(6, u.getStatut());
            ps.setBoolean(7, u.isVerified());
            ps.setString(8, u.getType());
            ps.setString(9, u.getPhotoProfile());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        u.setId(keys.getInt(1));
                    }
                }
                AppLogger.userAction("ACCOUNT_CREATED", u.getId(),
                        u.getPrenom() + " " + u.getNom() + " (" + u.getEmail() + ")");
            } else {
                AppLogger.error("[UserService] Aucune ligne insérée pour: " + u.getEmail());
            }

            if (!cnx.getAutoCommit()) {
                cnx.commit();
            }
        } catch (SQLException e) {
            AppLogger.error("[UserService] Erreur SQL lors de l'ajout: " + e.getMessage(), e);
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
                users.add(mapFromResultSet(rs));
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

        // 6. Login logs
        try {
            String logSql = "DELETE FROM `login_log` WHERE user_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(logSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Table login_log peut ne pas exister
        }

        // ── FIN CASCADE ──

        String sql = "DELETE FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AppLogger.userAction("ACCOUNT_DELETED", id, "Utilisateur supprimé avec toutes ses données");
            } else {
                AppLogger.warn("[UserService] Aucun utilisateur trouvé avec l'ID: " + id);
            }
        }
    }

    // Modifier (Admin — met à jour tous les champs y compris roles)
    public void modifier(User u) throws SQLException {
        ensurePhotoColumn();
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, roles=?, statut=?, type=?, photo_profile=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRoles());
            ps.setString(5, u.getStatut());
            ps.setString(6, u.getType());
            ps.setString(7, u.getPhotoProfile());
            ps.setInt(8, u.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                AppLogger.userAction("PROFILE_MODIFIED_BY_ADMIN", u.getId(),
                        u.getPrenom() + " " + u.getNom());
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
            AppLogger.userAction("PASSWORD_CHANGED", userId, "Mot de passe modifié");
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
        ensurePhotoColumn();
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, photo_profile=? WHERE id=?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhotoProfile());
            ps.setInt(5, u.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                AppLogger.userAction("PROFILE_UPDATED", u.getId(),
                        u.getPrenom() + " " + u.getNom() + " (" + u.getEmail() + ")");
            }
        }
    }

    // ════════════════════════════════════════
    //  RECHERCHE
    // ════════════════════════════════════════

    // Rechercher par ID
    public User findById(int id) throws SQLException {
        ensurePhotoColumn();
        String sql = "SELECT * FROM `user` WHERE id = ?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
            }
        }
        return null;
    }

    // Rechercher par email
    public User findByEmail(String email) throws SQLException {
        ensurePhotoColumn();
        String sql = "SELECT * FROM `user` WHERE email = ?";
        Connection cnx = getConnection();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
            }
        }
        return null;
    }

    // ════════════════════════════════════════
    //  AUTHENTIFICATION
    // ════════════════════════════════════════

    /**
     * Authentifie un utilisateur par email et mot de passe.
     * Supporte BCrypt ET mots de passe en clair (compatibilité).
     * @return l'objet User si authentification réussie, null sinon
     */
    public User authenticate(String email, String password) throws SQLException {
        User user = findByEmail(email);
        if (user != null && PasswordUtils.checkPassword(password, user.getPassword())) {
            // Mettre à jour last_login_at
            updateLastLogin(user.getId());
            return user;
        }
        return null;
    }

    /**
     * Met à jour la date de dernière connexion.
     */
    public void updateLastLogin(int userId) {
        try {
            ensureLastLoginColumn();
            String sql = "UPDATE `user` SET last_login_at = NOW() WHERE id = ?";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            AppLogger.error("[UserService] Erreur mise à jour last_login_at", e);
        }
    }

    // ════════════════════════════════════════
    //  STATISTIQUES (Dashboard)
    // ════════════════════════════════════════

    /**
     * Compte le nombre total d'utilisateurs.
     */
    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM `user`";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compte les utilisateurs actifs (statut = 'actif').
     */
    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM `user` WHERE statut = 'actif'";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compte les utilisateurs par rôle.
     */
    public int countByRole(String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `user` WHERE roles LIKE ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + role + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Récupère les N dernières connexions (utilisateurs avec last_login_at).
     */
    public List<User> getLastLogins(int limit) throws SQLException {
        ensureLastLoginColumn();
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM `user` WHERE last_login_at IS NOT NULL ORDER BY last_login_at DESC LIMIT ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapFromResultSet(rs));
                }
            }
        }
        return users;
    }

    /**
     * Compte les utilisateurs créés aujourd'hui.
     */
    public int countCreatedToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = CURDATE()";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ════════════════════════════════════════
    //  HELPERS INTERNES
    // ════════════════════════════════════════

    /**
     * Mappe un ResultSet vers un objet User.
     */
    private User mapFromResultSet(ResultSet rs) throws SQLException {
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
        u.setPhotoProfile(rs.getString("photo_profile"));

        // Nouveaux champs (avec protection si colonnes absentes)
        try { u.setLastLoginAt(rs.getTimestamp("last_login_at")); } catch (SQLException ignored) {}
        try { u.setLoginProvider(rs.getString("login_provider")); } catch (SQLException ignored) {}
        try { u.setVerificationToken(rs.getString("verification_token")); } catch (SQLException ignored) {}
        try { u.setResetToken(rs.getString("reset_token")); } catch (SQLException ignored) {}
        try { u.setResetTokenExpiry(rs.getTimestamp("reset_token_expiry")); } catch (SQLException ignored) {}
        try { u.setFaceEmbedding(rs.getString("face_embedding")); } catch (SQLException ignored) {}
        try { u.setFaceEnabled(rs.getBoolean("face_enabled")); } catch (SQLException ignored) {}

        return u;
    }

    /**
     * S'assure que la colonne last_login_at existe.
     */
    private void ensureLastLoginColumn() {
        try {
            Connection cnx = getConnection();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "user", "last_login_at");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN last_login_at DATETIME NULL");
                    AppLogger.info("[UserService] Colonne last_login_at ajoutée");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Colonne probablement déjà existante
        }
    }

    /**
     * S'assure que la colonne photo_profile existe.
     */
    private void ensurePhotoColumn() {
        try {
            Connection cnx = getConnection();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "user", "photo_profile");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN photo_profile VARCHAR(255) NULL");
                    AppLogger.info("[UserService] Colonne photo_profile ajoutée");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Ignorer
        }
    }

    /**
     * S'assure que les colonnes face_embedding et face_enabled existent.
     */
    private void ensureFaceColumns() {
        try {
            Connection cnx = getConnection();
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "user", "face_embedding");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN face_embedding TEXT NULL");
                    AppLogger.info("[UserService] Colonne face_embedding ajoutée");
                }
            }
            rs.close();

            rs = meta.getColumns(null, null, "user", "face_enabled");
            if (!rs.next()) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE `user` ADD COLUMN face_enabled BOOLEAN DEFAULT FALSE");
                    AppLogger.info("[UserService] Colonne face_enabled ajoutée");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Ignorer
        }
    }

    /**
     * Met à jour les données faciales d'un utilisateur.
     */
    public void updateFaceData(User u) throws SQLException {
        ensureFaceColumns();
        String sql = "UPDATE `user` SET face_embedding=?, face_enabled=? WHERE id=?";
        Connection cnx = getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getFaceEmbedding());
            ps.setBoolean(2, u.isFaceEnabled());
            ps.setInt(3, u.getId());
            ps.executeUpdate();
            AppLogger.info("[UserService] Données faciales mises à jour pour user ID: " + u.getId());
        }
    }
}