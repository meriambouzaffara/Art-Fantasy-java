package tn.rouhfan.services;

import tn.rouhfan.entities.Evenement;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {

    Connection cnx;

    public EvenementService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // Ajouter
    public void ajouter(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, description, image, type, statut, date_event, lieu, capacite, nb_participants, google_event_id, sponsor_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getImage());
        ps.setString(4, e.getType());
        ps.setString(5, e.getStatut());
        ps.setTimestamp(6, new Timestamp(e.getDateEvent().getTime()));
        ps.setString(7, e.getLieu());

        if (e.getCapacite() != null)
            ps.setInt(8, e.getCapacite());
        else
            ps.setNull(8, Types.INTEGER);

        ps.setInt(9, e.getNbParticipants());
        ps.setString(10, e.getGoogleEventId());

        // 🔥 relation sponsor
        if (e.getSponsor() != null)
            ps.setInt(11, e.getSponsor().getId());
        else
            ps.setNull(11, Types.INTEGER);

        ps.executeUpdate();

        System.out.println("Evenement ajouté en BD");
    }

    // Récupérer avec JOIN sponsor
    public List<Evenement> recuperer() throws SQLException {
        List<Evenement> events = new ArrayList<>();

        String sql = "SELECT e.*, s.id AS s_id, s.nom AS s_nom " +
                "FROM evenement e LEFT JOIN sponsor s ON e.sponsor_id = s.id";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Evenement e = new Evenement();

            e.setId(rs.getInt("id"));
            e.setTitre(rs.getString("titre"));
            e.setDescription(rs.getString("description"));
            e.setImage(rs.getString("image"));
            e.setType(rs.getString("type"));
            e.setStatut(rs.getString("statut"));
            e.setDateEvent(rs.getTimestamp("date_event"));
            e.setLieu(rs.getString("lieu"));
            e.setCapacite(rs.getInt("capacite"));
            e.setNbParticipants(rs.getInt("nb_participants"));
            e.setGoogleEventId(rs.getString("google_event_id"));

            // 🔥 récupérer sponsor
            int sponsorId = rs.getInt("s_id");
            if (!rs.wasNull()) {
                Sponsor s = new Sponsor();
                s.setId(sponsorId);
                s.setNom(rs.getString("s_nom"));
                e.setSponsor(s);
            }

            events.add(e);
        }

        return events;
    }

    // Supprimer
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println(" Evenement supprimé");
    }

    // Modifier
    public void modifier(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET titre=?, statut=?, lieu=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, e.getTitre());
        ps.setString(2, e.getStatut());
        ps.setString(3, e.getLieu());
        ps.setInt(4, e.getId());

        ps.executeUpdate();
        System.out.println(" Evenement modifié");
    }
}