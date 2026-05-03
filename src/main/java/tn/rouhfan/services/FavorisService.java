package tn.rouhfan.services;

import tn.rouhfan.entities.Favoris;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavorisService {

    Connection cnx;

    public FavorisService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(int userId, int oeuvreId) throws SQLException {
        String sql = "INSERT INTO favoris (id_user, oeuvre_id, created_at) VALUES (?, ?, NOW())";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ps.executeUpdate();
        System.out.println("Favori ajouté en BD");
    }

    public void supprimer(int userId, int oeuvreId) throws SQLException {
        String sql = "DELETE FROM favoris WHERE id_user = ? AND oeuvre_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ps.executeUpdate();
        System.out.println("🗑️ Favori supprimé");
    }

    public boolean exists(int userId, int oeuvreId) throws SQLException {
        String sql = "SELECT 1 FROM favoris WHERE id_user = ? AND oeuvre_id = ? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, oeuvreId);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public List<Favoris> recupererParUser(int userId) throws SQLException {
        List<Favoris> favoris = new ArrayList<>();

        String sql = "SELECT f.id_favoris, f.created_at, " +
                "u_fav.id AS u_id, u_fav.nom AS u_nom, u_fav.prenom AS u_prenom, " +
                "o.id AS o_id, o.titre AS o_titre, o.description AS o_desc, o.prix AS o_prix, o.statut AS o_statut, o.image AS o_image, o.date_vente AS o_date_vente, " +
                "c.id_categorie AS c_id, c.nom_categorie AS c_nom, " +
                "u_auth.id AS auth_id, u_auth.nom AS auth_nom, u_auth.prenom AS auth_prenom " +
                "FROM favoris f " +
                "JOIN `user` u_fav ON f.id_user = u_fav.id " +
                "JOIN oeuvre o ON f.oeuvre_id = o.id " +
                "LEFT JOIN `user` u_auth ON o.user_id = u_auth.id " +
                "LEFT JOIN categorie c ON o.categorie_id = c.id_categorie " +
                "WHERE f.id_user = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Favoris f = new Favoris();
            f.setIdFavoris(rs.getInt("id_favoris"));
            f.setCreatedAt(rs.getTimestamp("created_at"));

            // Utilisateur qui a mis en favoris
            User uFav = new User();
            uFav.setId(rs.getInt("u_id"));
            uFav.setNom(rs.getString("u_nom"));
            uFav.setPrenom(rs.getString("u_prenom"));
            f.setUser(uFav);

            // L'œuvre elle-même
            Oeuvre o = new Oeuvre();
            o.setId(rs.getInt("o_id"));
            o.setTitre(rs.getString("o_titre"));
            o.setDescription(rs.getString("o_desc"));
            o.setPrix(rs.getBigDecimal("o_prix"));
            o.setStatut(rs.getString("o_statut"));
            o.setImage(rs.getString("o_image"));
            o.setDateVente(rs.getTimestamp("o_date_vente"));

            // L'auteur de l'œuvre
            int authId = rs.getInt("auth_id");
            if (!rs.wasNull()) {
                User uAuth = new User();
                uAuth.setId(authId);
                uAuth.setNom(rs.getString("auth_nom"));
                uAuth.setPrenom(rs.getString("auth_prenom"));
                o.setUser(uAuth);
            }

            // Catégorie
            tn.rouhfan.entities.Categorie cat = new tn.rouhfan.entities.Categorie();
            cat.setIdCategorie(rs.getInt("c_id"));
            cat.setNomCategorie(rs.getString("c_nom"));
            o.setCategorie(cat);

            f.setOeuvre(o);
            favoris.add(f);
        }

        return favoris;
    }

    public List<Oeuvre> getFavoriteOeuvres(int userId) throws SQLException {
        List<Favoris> favoris = recupererParUser(userId);
        List<Oeuvre> oeuvres = new ArrayList<>();
        for (Favoris f : favoris) {
            oeuvres.add(f.getOeuvre());
        }
        return oeuvres;
    }

    /**
     * Exporte les favoris d'un utilisateur en PDF
     */
    public void exportFavoritesToPDF(int userId, String filePath) throws Exception {
        List<Favoris> favoris = recupererParUser(userId);

        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
        document.open();

        // Titre
        com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 24, new java.awt.Color(36, 17, 151));
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Œuvres favoris — Rouh el Fann", titleFont);
        title.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        title.setSpacingAfter(10f);
        document.add(title);

        // Date
        com.lowagie.text.Font metaFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 12, java.awt.Color.BLACK);
        String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        document.add(new com.lowagie.text.Paragraph("Liste exportée le " + dateStr, metaFont));
        document.add(new com.lowagie.text.Paragraph(" ", metaFont)); // Spacer

        // Tableau
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.5f, 2f, 2.5f});

        // En-têtes
        com.lowagie.text.Font headFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, java.awt.Color.BLACK);
        java.awt.Color headBg = new java.awt.Color(240, 238, 245);
        String[] headers = {"Titre", "Prix (DT)", "Catégorie", "Auteur"};

        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, headFont));
            cell.setBackgroundColor(headBg);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Données
        com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 11, java.awt.Color.BLACK);
        for (Favoris f : favoris) {
            Oeuvre o = f.getOeuvre();
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(o.getTitre(), bodyFont))).setPadding(5);
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(o.getPrix().toString(), bodyFont))).setPadding(5);
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "", bodyFont))).setPadding(5);

            String author = o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "Inconnu";
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(author, bodyFont))).setPadding(5);
        }

        document.add(table);
        document.close();
    }

    public void toggle(int userId, int oeuvreId) throws SQLException {
        if (exists(userId, oeuvreId)) {
            supprimer(userId, oeuvreId);
        } else {
            ajouter(userId, oeuvreId);
        }
    }

    public void supprimerParUser(int userId) throws SQLException {
        String sql = "DELETE FROM favoris WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public void supprimerParOeuvre(int oeuvreId) throws SQLException {
        String sql = "DELETE FROM favoris WHERE oeuvre_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, oeuvreId);
        ps.executeUpdate();
    }
}
