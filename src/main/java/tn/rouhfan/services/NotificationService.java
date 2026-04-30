package tn.rouhfan.services;

import tn.rouhfan.entities.Notification;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private Connection cnx;

    public NotificationService() {
        cnx = MyDatabase.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = cnx.createStatement()) {
            // Optionnel : décommenter la ligne suivante une seule fois si vous voulez forcer la recréation
            // st.execute("DROP TABLE IF EXISTS app_notification");
            
            String query = "CREATE TABLE IF NOT EXISTS app_notification (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "oeuvre_id INT DEFAULT NULL, " +
                    "message VARCHAR(500) NOT NULL, " +
                    "lu BOOLEAN DEFAULT FALSE, " +
                    "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (oeuvre_id) REFERENCES oeuvre(id) ON DELETE SET NULL)";
            st.execute(query);
            System.out.println("✅ Table 'app_notification' vérifiée/créée.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création de la table app_notification : " + e.getMessage());
        }
    }

    public void ajouter(Notification n) {
        String query = "INSERT INTO app_notification (user_id, message, lu, date_creation, oeuvre_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, n.getUserId());
            pst.setString(2, n.getMessage());
            pst.setBoolean(3, n.isLu());
            pst.setTimestamp(4, n.getDateCreation());
            pst.setInt(5, n.getOeuvreId());
            pst.executeUpdate();
            System.out.println("🔔 Notification ajoutée pour l'utilisateur ID: " + n.getUserId());
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de la notification : " + e.getMessage());
        }
    }

    public List<Notification> recupererParUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String query = "SELECT * FROM app_notification WHERE user_id = ? ORDER BY date_creation DESC";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.setId(rs.getInt("id"));
                n.setUserId(rs.getInt("user_id"));
                n.setMessage(rs.getString("message"));
                n.setLu(rs.getBoolean("lu"));
                n.setDateCreation(rs.getTimestamp("date_creation"));
                n.setOeuvreId(rs.getInt("oeuvre_id"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des notifications : " + e.getMessage());
        }
        return list;
    }

    public int countUnread(int userId) {
        String query = "SELECT COUNT(*) FROM app_notification WHERE user_id = ? AND lu = FALSE";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des notifications : " + e.getMessage());
        }
        return 0;
    }

    public void markAsRead(int notificationId) {
        String query = "UPDATE app_notification SET lu = TRUE WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, notificationId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour de la notification : " + e.getMessage());
        }
    }
    
    public void markAllAsRead(int userId) {
        String query = "UPDATE app_notification SET lu = TRUE WHERE user_id = ? AND lu = FALSE";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour des notifications : " + e.getMessage());
        }
    }

    public void supprimer(int notificationId) {
        String query = "DELETE FROM app_notification WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, notificationId);
            pst.executeUpdate();
            System.out.println("🗑️ Notification supprimée ID: " + notificationId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de la notification : " + e.getMessage());
        }
    }
}
