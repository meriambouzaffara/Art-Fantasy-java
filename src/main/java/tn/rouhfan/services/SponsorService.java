package tn.rouhfan.services;

import tn.rouhfan.entities.Sponsor;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SponsorService implements IService<Sponsor> {

    Connection cnx;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\s\\+\\-\\(\\)]{8,}$");

    public SponsorService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public boolean valider(Sponsor s) {
        if (s.getNom() == null || s.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Le nom du sponsor est obligatoire");
        }
        if (s.getNom().trim().length() < 2) {
            throw new IllegalArgumentException("❌ Le nom doit contenir au moins 2 caractères");
        }
        if (s.getEmail() == null || s.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ L'email est obligatoire");
        }
        if (!EMAIL_PATTERN.matcher(s.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("❌ L'email n'est pas valide");
        }
        if (s.getTel() == null || s.getTel().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Le téléphone est obligatoire");
        }
        if (!PHONE_PATTERN.matcher(s.getTel().trim()).matches()) {
            throw new IllegalArgumentException("❌ Le téléphone n'est pas valide (min. 8 caractères)");
        }
        if (s.getAdresse() == null || s.getAdresse().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ L'adresse est obligatoire");
        }
        if (s.getAdresse().trim().length() < 5) {
            throw new IllegalArgumentException("❌ L'adresse doit contenir au moins 5 caractères");
        }
        return true;
    }

    private boolean existeSponsorEnDouble(Sponsor s) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sponsor WHERE LOWER(nom) = LOWER(?) AND LOWER(email) = LOWER(?) " +
                "AND LOWER(tel) = LOWER(?) AND LOWER(adresse) = LOWER(?)";
        if (s.getId() > 0) {
            sql += " AND id <> ?";
        }

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, s.getNom().trim());
        ps.setString(2, s.getEmail().trim());
        ps.setString(3, s.getTel().trim());
        ps.setString(4, s.getAdresse().trim());
        if (s.getId() > 0) {
            ps.setInt(5, s.getId());
        }

        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    @Override
    public void ajouter(Sponsor s) throws SQLException {
        valider(s);
        if (existeSponsorEnDouble(s)) {
            throw new SQLException("❌ Sponsor déjà existant avec le même nom, email, téléphone et adresse");
        }

        String sql = "INSERT INTO sponsor (nom, logo, description, email, tel, adresse, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, s.getNom().trim());
        ps.setString(2, s.getLogo() != null ? s.getLogo() : "");
        ps.setString(3, s.getDescription() != null ? s.getDescription().trim() : "");
        ps.setString(4, s.getEmail().trim());
        ps.setString(5, s.getTel().trim());
        ps.setString(6, s.getAdresse().trim());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            s.setId(rs.getInt(1));
        }

        System.out.println("✅ Sponsor ajouté en BD: " + s.getNom());
    }

    @Override
    public List<Sponsor> recuperer() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();

        String sql = "SELECT * FROM sponsor ORDER BY nom ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            sponsors.add(mapResultSetToSponsor(rs));
        }

        return sponsors;
    }

    @Override
    public Sponsor findById(int id) throws SQLException {
        String sql = "SELECT * FROM sponsor WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToSponsor(rs);
        }
        return null;
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM sponsor WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("✅ Sponsor supprimé (ID: " + id + ")");
        }
    }

    @Override
    public void modifier(Sponsor s) throws SQLException {
        valider(s);
        if (existeSponsorEnDouble(s)) {
            throw new SQLException("❌ Sponsor déjà existant avec le même nom, email, téléphone et adresse");
        }

        String sql = "UPDATE sponsor SET nom=?, logo=?, description=?, email=?, tel=?, adresse=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, s.getNom().trim());
        ps.setString(2, s.getLogo() != null ? s.getLogo() : "");
        ps.setString(3, s.getDescription() != null ? s.getDescription().trim() : "");
        ps.setString(4, s.getEmail().trim());
        ps.setString(5, s.getTel().trim());
        ps.setString(6, s.getAdresse().trim());
        ps.setInt(7, s.getId());

        int rows = ps.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Sponsor modifié: " + s.getNom());
        }
    }

    //  RECHERCHE (par nom, email, tel, adresse)
    public List<Sponsor> rechercher(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return recuperer();
        }

        String sql = "SELECT * FROM sponsor " +
                "WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) OR LOWER(tel) LIKE LOWER(?) OR LOWER(adresse) LIKE LOWER(?) " +
                "ORDER BY nom ASC";

        PreparedStatement ps = cnx.prepareStatement(sql);
        String pattern = "%" + keyword.trim() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ps.setString(3, pattern);
        ps.setString(4, pattern);

        ResultSet rs = ps.executeQuery();
        List<Sponsor> results = new ArrayList<>();

        while (rs.next()) {
            results.add(mapResultSetToSponsor(rs));
        }

        return results;
    }

    public List<Sponsor> triPar(String colonne, boolean ascending) throws SQLException {
        List<Sponsor> sponsors = recuperer();

        switch (colonne.toLowerCase()) {
            case "nom":
                sponsors.sort((a, b) -> ascending ? a.getNom().compareTo(b.getNom()) : b.getNom().compareTo(a.getNom()));
                break;
            case "email":
                sponsors.sort((a, b) -> ascending ? a.getEmail().compareTo(b.getEmail()) : b.getEmail().compareTo(a.getEmail()));
                break;
            case "tel":
                sponsors.sort((a, b) -> ascending ? a.getTel().compareTo(b.getTel()) : b.getTel().compareTo(a.getTel()));
                break;
            case "date":
                sponsors.sort((a, b) -> ascending ? a.getCreatedAt().compareTo(b.getCreatedAt()) : b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case "adresse":
                sponsors.sort((a, b) -> ascending ? a.getAdresse().compareTo(b.getAdresse()) : b.getAdresse().compareTo(a.getAdresse()));
                break;
        }

        return sponsors;
    }

    public List<Sponsor> rechercherEtTrier(String keyword, String colonne, boolean ascending) throws SQLException {
        List<Sponsor> sponsors = rechercher(keyword);

        switch (colonne.toLowerCase()) {
            case "nom":
                sponsors.sort((a, b) -> ascending ? a.getNom().compareTo(b.getNom()) : b.getNom().compareTo(a.getNom()));
                break;
            case "email":
                sponsors.sort((a, b) -> ascending ? a.getEmail().compareTo(b.getEmail()) : b.getEmail().compareTo(a.getEmail()));
                break;
            case "tel":
                sponsors.sort((a, b) -> ascending ? a.getTel().compareTo(b.getTel()) : b.getTel().compareTo(a.getTel()));
                break;
            case "date":
                sponsors.sort((a, b) -> ascending ? a.getCreatedAt().compareTo(b.getCreatedAt()) : b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case "adresse":
                sponsors.sort((a, b) -> ascending ? a.getAdresse().compareTo(b.getAdresse()) : b.getAdresse().compareTo(a.getAdresse()));
                break;
        }

        return sponsors;
    }

    private Sponsor mapResultSetToSponsor(ResultSet rs) throws SQLException {
        Sponsor s = new Sponsor();

        s.setId(rs.getInt("id"));
        s.setNom(rs.getString("nom"));
        s.setLogo(rs.getString("logo"));
        s.setDescription(rs.getString("description"));
        s.setEmail(rs.getString("email"));
        s.setTel(rs.getString("tel"));
        s.setAdresse(rs.getString("adresse"));
        s.setCreatedAt(rs.getTimestamp("created_at"));

        return s;
    }
}