package tn.rouhfan.services;

import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.Magasin;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ArticleService implements IService<Article> {

    Connection cnx;

    public ArticleService() {
        cnx = MyDatabase.getInstance().getConnection();
    }
// À ajouter dans ArticleService.java

    public List<Article> recupererParMagasin(Magasin magasin) throws SQLException {
        List<Article> articles = new ArrayList<>();

        String sql = "SELECT * FROM article WHERE magasin_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setLong(1, magasin.getId());
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Magasin m = new Magasin();
            m.setId(rs.getLong("magasin_id"));

            Timestamp ts = rs.getTimestamp("created_at");
            LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

            Article a = new Article(
                    rs.getLong("id_article"),
                    rs.getString("titre"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getString("description"),
                    createdAt,
                    rs.getString("image"),
                    m
            );
            a.setReference(rs.getString("reference"));
            articles.add(a);
        }
        return articles;
    }
    //  Ajouter (ou incrémenter le stock si même référence)
    @Override
    public void ajouter(Article a) throws SQLException {

        // ── Si une référence est renseignée, vérifier si elle existe déjà ──
        if (a.getReference() != null && !a.getReference().isBlank()) {
            String checkSql = "SELECT id_article FROM article WHERE reference = ?";
            try (PreparedStatement check = cnx.prepareStatement(checkSql)) {
                check.setString(1, a.getReference().trim());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        // Référence déjà présente → on ajoute le stock saisi au stock existant
                        long existingId = rs.getLong("id_article");
                        int stockAAjouter = a.getStock() != null ? a.getStock() : 1;
                        String updateSql = "UPDATE article SET stock = stock + ? WHERE id_article = ?";
                        try (PreparedStatement upd = cnx.prepareStatement(updateSql)) {
                            upd.setInt(1, stockAAjouter);
                            upd.setLong(2, existingId);
                            upd.executeUpdate();
                        }
                        System.out.println("📦 Stock mis à jour (+" + stockAAjouter + ") pour la référence : " + a.getReference());
                        return; // on arrête ici, pas d'INSERT
                    }
                }
            }
        }

        // ── Aucune référence existante → INSERT normal ──
        String sql = "INSERT INTO article (titre, reference, prix, stock, description, created_at, image, magasin_id) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        if (a.getReference() != null && !a.getReference().isBlank()) {
            ps.setString(2, a.getReference().trim());
        } else {
            ps.setNull(2, Types.VARCHAR);
        }
        ps.setDouble(3, a.getPrix());
        ps.setInt(4, a.getStock());
        ps.setString(5, a.getDescription());
        ps.setString(6, a.getImage());
        ps.setLong(7, a.getMagasin().getId());

        ps.executeUpdate();
        System.out.println("✅ Article ajouté");
    }

    //  Supprimer
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM article WHERE id_article = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("🗑️ Article supprimé");
    }

    //  Modifier
    @Override
    public void modifier(Article a) throws SQLException {
        String sql = "UPDATE article SET titre=?, reference=?, prix=?, stock=?, description=?, image=?, magasin_id=? WHERE id_article=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        if (a.getReference() != null && !a.getReference().isBlank()) {
            ps.setString(2, a.getReference().trim());
        } else {
            ps.setNull(2, Types.VARCHAR);
        }
        ps.setDouble(3, a.getPrix());
        ps.setInt(4, a.getStock());
        ps.setString(5, a.getDescription());
        ps.setString(6, a.getImage());
        ps.setLong(7, a.getMagasin().getId());
        ps.setLong(8, a.getIdArticle());

        ps.executeUpdate();
        System.out.println("Article modifié");
    }

    //Récupérer tous
    @Override
    public List<Article> recuperer() throws SQLException {
        List<Article> articles = new ArrayList<>();

        String sql = "SELECT * FROM article";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Magasin m = new Magasin();
            m.setId(rs.getLong("magasin_id"));

            Timestamp ts = rs.getTimestamp("created_at");
            LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

            Article a = new Article(
                    rs.getLong("id_article"),
                    rs.getString("titre"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getString("description"),
                    createdAt,
                    rs.getString("image"),
                    m
            );
            a.setReference(rs.getString("reference"));
            articles.add(a);
        }

        return articles;
    }

    // Find by ID
    @Override
    public Article findById(int id) throws SQLException {
        String sql = "SELECT * FROM article WHERE id_article = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Magasin m = new Magasin();
            m.setId(rs.getLong("magasin_id"));

            Article a = new Article(
                    rs.getLong("id_article"),
                    rs.getString("titre"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getString("image"),
                    m
            );
            a.setReference(rs.getString("reference"));
            return a;
        }

        return null;
    }
}