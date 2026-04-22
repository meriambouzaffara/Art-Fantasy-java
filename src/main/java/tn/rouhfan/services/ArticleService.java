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
            articles.add(a);
        }
        return articles;
    }
    //  Ajouter
    @Override
    public void ajouter(Article a) throws SQLException {
        String sql = "INSERT INTO article (titre, prix, stock, description, created_at, image, magasin_id) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        ps.setDouble(2, a.getPrix());
        ps.setInt(3, a.getStock());
        ps.setString(4, a.getDescription());
        ps.setString(5, a.getImage());
        ps.setLong(6, a.getMagasin().getId());

        ps.executeUpdate();
        System.out.println(" Article ajouté");
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
        String sql = "UPDATE article SET titre=?, prix=?, stock=?, description=?, image=?, magasin_id=? WHERE id_article=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        ps.setDouble(2, a.getPrix());
        ps.setInt(3, a.getStock());
        ps.setString(4, a.getDescription());
        ps.setString(5, a.getImage());
        ps.setLong(6, a.getMagasin().getId());
        ps.setLong(7, a.getIdArticle());

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

            return new Article(
                    rs.getLong("id_article"),
                    rs.getString("titre"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getString("image"),
                    m
            );
        }

        return null;
    }
}