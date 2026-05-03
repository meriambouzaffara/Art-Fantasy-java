package tn.rouhfan.services;

import tn.rouhfan.entities.Commentaire;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService {
    
    private Connection cnx;

    public CommentaireService() {
        cnx = MyDatabase.getInstance().getConnection();
        try {
            createTableIfNotExists();
        } catch (SQLException e) {
            System.err.println("Erreur création table commentaire: " + e.getMessage());
        }
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS commentaire (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "contenu TEXT NOT NULL, " +
                "date_commentaire TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "oeuvre_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "parent_comment_id INT DEFAULT NULL, " +
                "FOREIGN KEY (oeuvre_id) REFERENCES oeuvre(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (parent_comment_id) REFERENCES commentaire(id) ON DELETE CASCADE" +
                ")";
        Statement st = cnx.createStatement();
        st.execute(sql);

        // Add reaction table
        String sqlReaction = "CREATE TABLE IF NOT EXISTS commentaire_reaction (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "commentaire_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "reaction_type VARCHAR(20) NOT NULL, " +
                "FOREIGN KEY (commentaire_id) REFERENCES commentaire(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_reaction (commentaire_id, user_id)" +
                ")";
        st.execute(sqlReaction);
    }

    public void ajouterReaction(int commentId, int userId, String type) throws SQLException {
        String sql = "INSERT INTO commentaire_reaction (commentaire_id, user_id, reaction_type) " +
                     "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE reaction_type = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.setInt(2, userId);
        ps.setString(3, type);
        ps.setString(4, type);
        ps.executeUpdate();

        // --- Logique de notification pour le participant ---
        // Si l'artiste (propriétaire de l'œuvre) réagit au commentaire d'un utilisateur
        String sqlInfo = "SELECT c.user_id as comment_author_id, o.user_id as owner_id, o.titre, o.id as oeuvre_id " +
                         "FROM commentaire c " +
                         "JOIN oeuvre o ON c.oeuvre_id = o.id " +
                         "WHERE c.id = ?";
        PreparedStatement psInfo = cnx.prepareStatement(sqlInfo);
        psInfo.setInt(1, commentId);
        ResultSet rsInfo = psInfo.executeQuery();
        
        if (rsInfo.next()) {
            int commentAuthorId = rsInfo.getInt("comment_author_id");
            int ownerId = rsInfo.getInt("owner_id");
            String oeuvreTitre = rsInfo.getString("titre");
            int oeuvreId = rsInfo.getInt("oeuvre_id");

            // Si c'est l'artiste qui réagit et que l'auteur du commentaire n'est pas lui-même
            if (userId == ownerId && commentAuthorId != ownerId) {
                String message = "L'artiste a réagi avec \"" + type + "\" à votre commentaire sur l'œuvre \"" + oeuvreTitre + "\".";
                NotificationService ns = new NotificationService();
                ns.ajouter(new tn.rouhfan.entities.Notification(commentAuthorId, message, oeuvreId));
            }
        }
    }

    public void supprimerReaction(int commentId, int userId) throws SQLException {
        String sql = "DELETE FROM commentaire_reaction WHERE commentaire_id = ? AND user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public java.util.Map<String, Integer> getReactionCounts(int commentId) throws SQLException {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        String sql = "SELECT reaction_type, COUNT(*) as count FROM commentaire_reaction WHERE commentaire_id = ? GROUP BY reaction_type";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, commentId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            counts.put(rs.getString("reaction_type"), rs.getInt("count"));
        }
        return counts;
    }

    public String getUserReaction(int commentId, int userId) throws SQLException {
        String sql = "SELECT reaction_type FROM commentaire_reaction WHERE commentaire_id = ? AND user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, commentId);
        ps.setInt(2, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("reaction_type");
        }
        return null;
    }

    public void ajouter(Commentaire c) throws SQLException {
        String sql = "INSERT INTO commentaire (contenu, oeuvre_id, user_id, parent_comment_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getContenu());
        ps.setInt(2, c.getOeuvreId());
        ps.setInt(3, c.getUserId());
        if (c.getParentCommentId() != null) {
            ps.setInt(4, c.getParentCommentId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            c.setId(rs.getInt(1));
        }

        // --- Logique de notification interne ---
        String sqlOwner = "SELECT user_id, titre FROM oeuvre WHERE id = ?";
        PreparedStatement psOwner = cnx.prepareStatement(sqlOwner);
        psOwner.setInt(1, c.getOeuvreId());
        ResultSet rsOwner = psOwner.executeQuery();
        if (rsOwner.next()) {
            int ownerId = rsOwner.getInt("user_id");
            String oeuvreTitre = rsOwner.getString("titre");
            
            // 1. Notifier l'artiste si quelqu'un d'autre commente son œuvre
            if (ownerId != c.getUserId()) {
                String nomCommentateur = "Un utilisateur";
                String sqlUser = "SELECT nom, prenom FROM user WHERE id = ?";
                PreparedStatement psUser = cnx.prepareStatement(sqlUser);
                psUser.setInt(1, c.getUserId());
                ResultSet rsUser = psUser.executeQuery();
                if (rsUser.next()) {
                    nomCommentateur = rsUser.getString("prenom") + " " + rsUser.getString("nom");
                }
                
                String message = nomCommentateur + " a commenté votre œuvre \"" + oeuvreTitre + "\".";
                NotificationService ns = new NotificationService();
                ns.ajouter(new tn.rouhfan.entities.Notification(ownerId, message, c.getOeuvreId()));
            }

            // 2. Notifier l'auteur du commentaire parent si l'artiste répond
            if (c.getParentCommentId() != null) {
                String sqlParent = "SELECT user_id FROM commentaire WHERE id = ?";
                PreparedStatement psParent = cnx.prepareStatement(sqlParent);
                psParent.setInt(1, c.getParentCommentId());
                ResultSet rsParent = psParent.executeQuery();
                if (rsParent.next()) {
                    int parentAuthorId = rsParent.getInt("user_id");
                    
                    // Si le répondant est l'artiste et qu'il répond à quelqu'un d'autre
                    if (c.getUserId() == ownerId && parentAuthorId != ownerId) {
                        String message = "L'artiste a répondu à votre commentaire sur l'œuvre \"" + oeuvreTitre + "\".";
                        NotificationService ns = new NotificationService();
                        ns.ajouter(new tn.rouhfan.entities.Notification(parentAuthorId, message, c.getOeuvreId()));
                    }
                }
            }
        }
    }

    public List<Commentaire> recupererParOeuvre(int oeuvreId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.*, u.nom, u.prenom, u.type FROM commentaire c " +
                "JOIN user u ON c.user_id = u.id " +
                "WHERE c.oeuvre_id = ? " +
                "ORDER BY c.date_commentaire ASC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, oeuvreId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setId(rs.getInt("id"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire"));
            c.setOeuvreId(rs.getInt("oeuvre_id"));
            c.setUserId(rs.getInt("user_id"));
            int parentId = rs.getInt("parent_comment_id");
            if (!rs.wasNull()) {
                c.setParentCommentId(parentId);
            }
            c.setUserName(rs.getString("nom") + " " + rs.getString("prenom"));
            c.setUserType(rs.getString("type"));
            commentaires.add(c);
        }
        return commentaires;
    }

    public List<Commentaire> recupererParArtiste(int artisteId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.*, u.nom, u.prenom, u.type, o.titre as oeuvre_titre FROM commentaire c " +
                "JOIN user u ON c.user_id = u.id " +
                "JOIN oeuvre o ON c.oeuvre_id = o.id " +
                "WHERE o.user_id = ? " +
                "ORDER BY c.date_commentaire DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, artisteId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Commentaire c = new Commentaire();
            c.setId(rs.getInt("id"));
            c.setContenu(rs.getString("contenu"));
            c.setDateCommentaire(rs.getTimestamp("date_commentaire"));
            c.setOeuvreId(rs.getInt("oeuvre_id"));
            c.setUserId(rs.getInt("user_id"));
            int parentId = rs.getInt("parent_comment_id");
            if (!rs.wasNull()) {
                c.setParentCommentId(parentId);
            }
            c.setUserName(rs.getString("nom") + " " + rs.getString("prenom"));
            c.setUserType(rs.getString("type"));
            c.setOeuvreTitre(rs.getString("oeuvre_titre"));
            commentaires.add(c);
        }
        return commentaires;
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
