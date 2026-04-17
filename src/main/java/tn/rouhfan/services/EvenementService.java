package tn.rouhfan.services;

import tn.rouhfan.entities.Evenement;
import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;
import tn.rouhfan.tools.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EvenementService implements IService<Evenement> {

    Connection cnx;
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "Formation", "Exposition", "Concert", "Festival", "Atelier", "Concours", "Conference"
    );
    private GoogleCalendarService calendarService;

    public EvenementService() {
        cnx = MyDatabase.getInstance().getConnection();
        calendarService = new GoogleCalendarService();
        verifierEtAjouterColonneCreateurId();
        verifierEtAjouterColonneGoogleEventId();
    }

    private void verifierEtAjouterColonneCreateurId() {
        try {
            DatabaseMetaData metaData = cnx.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, "evenement", "createur_id");
            if (!rs.next()) {
                System.out.println("⚠️ Colonne createur_id manquante, ajout en cours...");
                Statement st = cnx.createStatement();
                st.executeUpdate("ALTER TABLE evenement ADD COLUMN createur_id INT DEFAULT NULL");
                System.out.println("✅ Colonne createur_id ajoutée avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la colonne createur_id: " + e.getMessage());
        }
    }

    private void verifierEtAjouterColonneGoogleEventId() {
        try {
            DatabaseMetaData metaData = cnx.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, "evenement", "google_event_id");
            if (!rs.next()) {
                System.out.println("⚠️ Colonne google_event_id manquante, ajout en cours...");
                Statement st = cnx.createStatement();
                st.executeUpdate("ALTER TABLE evenement ADD COLUMN google_event_id VARCHAR(255) DEFAULT NULL");
                System.out.println("✅ Colonne google_event_id ajoutée avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la colonne google_event_id: " + e.getMessage());
        }
    }

    public boolean valider(Evenement e) {
        if (e.getTitre() == null || e.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Le titre est obligatoire");
        }
        if (e.getTitre().trim().length() < 3) {
            throw new IllegalArgumentException("❌ Le titre doit contenir au moins 3 caractères");
        }
        if (e.getDateEvent() == null) {
            throw new IllegalArgumentException("❌ La date est obligatoire");
        }
        if (e.getDateEvent().getTime() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("❌ La date doit être dans le futur");
        }
        if (e.getLieu() == null || e.getLieu().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Le lieu est obligatoire");
        }
        if (e.getType() == null || e.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Le type est obligatoire");
        }
        if (ALLOWED_TYPES.stream().noneMatch(type -> type.equalsIgnoreCase(e.getType().trim()))) {
            throw new IllegalArgumentException("❌ Type invalide. Choisissez parmi : " + String.join(", ", ALLOWED_TYPES));
        }
        if (e.getCapacite() != null) {
            if (e.getCapacite() < 0) {
                throw new IllegalArgumentException("❌ La capacité ne peut pas être négative");
            }
            if (e.getCapacite() == 0) {
                throw new IllegalArgumentException("❌ La capacité doit être supérieure à 0");
            }
        }
        if (e.getNbParticipants() < 0) {
            throw new IllegalArgumentException("❌ Le nombre de participants ne peut pas être négatif");
        }
        if (e.getCapacite() != null && e.getNbParticipants() > e.getCapacite()) {
            throw new IllegalArgumentException("❌ Le nombre de participants ne peut pas dépasser la capacité");
        }
        return true;
    }

    private boolean existeEvenementEnDouble(Evenement e) throws SQLException {
        String sql = "SELECT COUNT(*) FROM evenement WHERE LOWER(titre)=LOWER(?) AND date_event=? AND LOWER(lieu)=LOWER(?) AND LOWER(type)=LOWER(?)";
        if (e.getId() > 0) {
            sql += " AND id <> ?";
        }

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, e.getTitre().trim());
        ps.setTimestamp(2, new Timestamp(e.getDateEvent().getTime()));
        ps.setString(3, e.getLieu().trim());
        ps.setString(4, e.getType().trim());
        if (e.getId() > 0) {
            ps.setInt(5, e.getId());
        }

        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
        valider(e);
        if (existeEvenementEnDouble(e)) {
            throw new SQLException("❌ Événement déjà existant avec le même titre, date, lieu et type");
        }

        String sql = "INSERT INTO evenement (titre, description, image, type, statut, date_event, lieu, capacite, nb_participants, google_event_id, sponsor_id, createur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, e.getTitre().trim());
        ps.setString(2, e.getDescription() != null ? e.getDescription().trim() : "");
        ps.setString(3, e.getImage() != null ? e.getImage() : "");
        ps.setString(4, e.getType() != null ? e.getType() : "");
        ps.setString(5, e.getStatut() != null ? e.getStatut() : "PLANIFIÉ");
        ps.setTimestamp(6, new Timestamp(e.getDateEvent().getTime()));
        ps.setString(7, e.getLieu().trim());

        if (e.getCapacite() != null)
            ps.setInt(8, e.getCapacite());
        else
            ps.setNull(8, Types.INTEGER);

        ps.setInt(9, e.getNbParticipants());
        
        // Pushing to Google Calendar
        String googleId = calendarService.ajouterEvenement(e);
        e.setGoogleEventId(googleId);
        ps.setString(10, googleId != null ? googleId : "");

        if (e.getSponsor() != null)
            ps.setInt(11, e.getSponsor().getId());
        else
            ps.setNull(11, Types.INTEGER);

        int createurId = 0;
        if (SessionManager.getInstance().isLoggedIn()) {
            createurId = SessionManager.getInstance().getCurrentUser().getId();
        }
        if (createurId > 0) {
            ps.setInt(12, createurId);
        } else {
            ps.setNull(12, Types.INTEGER);
        }

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            e.setId(rs.getInt(1));
        }

        System.out.println("✅ Evenement ajouté en BD: " + e.getTitre());
    }

    @Override
    public List<Evenement> recuperer() throws SQLException {
        return recupererAvecJoin("SELECT e.*, s.id AS s_id, s.nom AS s_nom, u.nom AS u_nom, u.prenom AS u_prenom FROM evenement e LEFT JOIN sponsor s ON e.sponsor_id = s.id LEFT JOIN `user` u ON e.createur_id = u.id");
    }

    @Override
    public Evenement findById(int id) throws SQLException {
        String sql = "SELECT e.*, s.id AS s_id, s.nom AS s_nom, u.nom AS u_nom, u.prenom AS u_prenom FROM evenement e LEFT JOIN sponsor s ON e.sponsor_id = s.id LEFT JOIN `user` u ON e.createur_id = u.id WHERE e.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToEvenement(rs);
        }
        return null;
    }

    @Override
    public void supprimer(int id) throws SQLException {
        Evenement eventLocal = findById(id);
        
        String sql = "DELETE FROM evenement WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        int rows = ps.executeUpdate();
        
        if (rows > 0) {
            System.out.println("✅ Evenement supprimé (ID: " + id + ")");
            if (eventLocal != null && eventLocal.getGoogleEventId() != null && !eventLocal.getGoogleEventId().isEmpty()) {
                calendarService.supprimerEvenement(eventLocal.getGoogleEventId());
            }
        }
    }

    @Override
    public void modifier(Evenement e) throws SQLException {
        valider(e);
        if (existeEvenementEnDouble(e)) {
            throw new SQLException("❌ Événement déjà existant avec le même titre, date, lieu et type");
        }

        String sql = "UPDATE evenement SET titre=?, description=?, image=?, type=?, statut=?, date_event=?, lieu=?, capacite=?, nb_participants=?, google_event_id=?, sponsor_id=?, createur_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, e.getTitre().trim());
        ps.setString(2, e.getDescription() != null ? e.getDescription().trim() : "");
        ps.setString(3, e.getImage() != null ? e.getImage() : "");
        ps.setString(4, e.getType() != null ? e.getType() : "");
        ps.setString(5, e.getStatut() != null ? e.getStatut() : "PLANIFIÉ");
        ps.setTimestamp(6, new Timestamp(e.getDateEvent().getTime()));
        ps.setString(7, e.getLieu().trim());

        if (e.getCapacite() != null)
            ps.setInt(8, e.getCapacite());
        else
            ps.setNull(8, Types.INTEGER);

        ps.setInt(9, e.getNbParticipants());
        ps.setString(10, e.getGoogleEventId() != null ? e.getGoogleEventId() : "");

        if (e.getSponsor() != null)
            ps.setInt(11, e.getSponsor().getId());
        else
            ps.setNull(11, Types.INTEGER);

        if (e.getCreateurId() > 0)
            ps.setInt(12, e.getCreateurId());
        else
            ps.setNull(12, Types.INTEGER);

        ps.setInt(13, e.getId());

        int rows = ps.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Evenement modifié: " + e.getTitre());
            if (e.getGoogleEventId() != null && !e.getGoogleEventId().isEmpty()) {
                calendarService.modifierEvenement(e.getGoogleEventId(), e);
            }
        }
    }

    // RECHERCHE (par titre, description, lieu, type)
    public List<Evenement> rechercher(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return recuperer();
        }

        String sql = "SELECT e.*, s.id AS s_id, s.nom AS s_nom, u.nom AS u_nom, u.prenom AS u_prenom FROM evenement e LEFT JOIN sponsor s ON e.sponsor_id = s.id LEFT JOIN `user` u ON e.createur_id = u.id " +
                "WHERE LOWER(e.titre) LIKE LOWER(?) OR LOWER(e.description) LIKE LOWER(?) OR LOWER(e.lieu) LIKE LOWER(?) OR LOWER(e.type) LIKE LOWER(?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        String pattern = "%" + keyword.trim() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ps.setString(3, pattern);
        ps.setString(4, pattern);

        ResultSet rs = ps.executeQuery();
        List<Evenement> results = new ArrayList<>();

        while (rs.next()) {
            results.add(mapResultSetToEvenement(rs));
        }

        return results;
    }

    public List<Evenement> triPar(String colonne, boolean ascending) throws SQLException {
        List<Evenement> events = recuperer();

        switch (colonne.toLowerCase()) {
            case "titre":
                events.sort((a, b) -> ascending ? a.getTitre().compareTo(b.getTitre()) : b.getTitre().compareTo(a.getTitre()));
                break;
            case "date":
                events.sort((a, b) -> ascending ? a.getDateEvent().compareTo(b.getDateEvent()) : b.getDateEvent().compareTo(a.getDateEvent()));
                break;
            case "lieu":
                events.sort((a, b) -> ascending ? a.getLieu().compareTo(b.getLieu()) : b.getLieu().compareTo(a.getLieu()));
                break;
            case "capacite":
                events.sort((a, b) -> ascending ? Integer.compare(a.getCapacite(), b.getCapacite()) : Integer.compare(b.getCapacite(), a.getCapacite()));
                break;
            case "participants":
                events.sort((a, b) -> ascending ? Integer.compare(a.getNbParticipants(), b.getNbParticipants()) : Integer.compare(b.getNbParticipants(), a.getNbParticipants()));
                break;
            case "type":
                events.sort((a, b) -> ascending ? a.getType().compareTo(b.getType()) : b.getType().compareTo(a.getType()));
                break;
            case "statut":
                events.sort((a, b) -> ascending ? a.getStatut().compareTo(b.getStatut()) : b.getStatut().compareTo(a.getStatut()));
                break;
        }

        return events;
    }

    public List<Evenement> rechercherEtTrier(String keyword, String colonne, boolean ascending) throws SQLException {
        List<Evenement> events = rechercher(keyword);

        switch (colonne.toLowerCase()) {
            case "titre":
                events.sort((a, b) -> ascending ? a.getTitre().compareTo(b.getTitre()) : b.getTitre().compareTo(a.getTitre()));
                break;
            case "date":
                events.sort((a, b) -> ascending ? a.getDateEvent().compareTo(b.getDateEvent()) : b.getDateEvent().compareTo(a.getDateEvent()));
                break;
            case "lieu":
                events.sort((a, b) -> ascending ? a.getLieu().compareTo(b.getLieu()) : b.getLieu().compareTo(a.getLieu()));
                break;
            case "capacite":
                events.sort((a, b) -> ascending ? Integer.compare(a.getCapacite(), b.getCapacite()) : Integer.compare(b.getCapacite(), a.getCapacite()));
                break;
            case "participants":
                events.sort((a, b) -> ascending ? Integer.compare(a.getNbParticipants(), b.getNbParticipants()) : Integer.compare(b.getNbParticipants(), a.getNbParticipants()));
                break;
            case "type":
                events.sort((a, b) -> ascending ? a.getType().compareTo(b.getType()) : b.getType().compareTo(a.getType()));
                break;
            case "statut":
                events.sort((a, b) -> ascending ? a.getStatut().compareTo(b.getStatut()) : b.getStatut().compareTo(a.getStatut()));
                break;
        }

        return events;
    }

    private List<Evenement> recupererAvecJoin(String sql) throws SQLException {
        List<Evenement> events = new ArrayList<>();
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            events.add(mapResultSetToEvenement(rs));
        }

        return events;
    }

    public void participer(int eventId) throws SQLException {
        // First check if event exists and has capacity
        Evenement event = findById(eventId);
        if (event == null) {
            throw new SQLException("Événement non trouvé");
        }

        // Check if event is full
        if (event.getCapacite() != null && event.getNbParticipants() >= event.getCapacite()) {
            throw new SQLException("Événement complet");
        }

        String sql = "UPDATE evenement SET nb_participants = nb_participants + 1 WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, eventId);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("✅ Participation enregistrée pour l'événement ID: " + eventId);
        } else {
            throw new SQLException("Erreur lors de la mise à jour");
        }
    }

    private Evenement mapResultSetToEvenement(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();

        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setImage(rs.getString("image"));
        e.setType(rs.getString("type"));
        e.setStatut(rs.getString("statut"));
        e.setDateEvent(rs.getTimestamp("date_event"));
        e.setLieu(rs.getString("lieu"));
        int capaciteValue = rs.getInt("capacite");
        e.setCapacite(rs.wasNull() ? null : capaciteValue);
        e.setNbParticipants(rs.getInt("nb_participants"));
        e.setGoogleEventId(rs.getString("google_event_id"));

        int createurId = rs.getInt("createur_id");
        if (!rs.wasNull()) {
            e.setCreateurId(createurId);
            User createur = new User();
            createur.setId(createurId);
            createur.setNom(rs.getString("u_nom"));
            createur.setPrenom(rs.getString("u_prenom"));
            e.setCreateur(createur);
        }

        int sponsorId = rs.getInt("s_id");
        if (!rs.wasNull()) {
            Sponsor s = new Sponsor();
            s.setId(sponsorId);
            s.setNom(rs.getString("s_nom"));
            e.setSponsor(s);
        }

        return e;
    }
}