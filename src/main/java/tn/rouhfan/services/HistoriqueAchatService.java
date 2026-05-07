package tn.rouhfan.services;

import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.HistoriqueAchat;
import tn.rouhfan.entities.HistoriqueAchatLigne;
import tn.rouhfan.tools.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour les tables historique_achat et historique_achat_ligne.
 *
 * Utilisation typique depuis CheckoutController (après confirmation du paiement) :
 * <pre>
 *   HistoriqueAchatService service = new HistoriqueAchatService();
 *   service.enregistrerAchat(orderReference, email, items, total, userId);
 * </pre>
 */
public class HistoriqueAchatService {

    private final Connection cnx;

    public HistoriqueAchatService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // ─────────────────────────────────────────────────────────────
    // INSERTION COMPLÈTE (achat + lignes)
    // ─────────────────────────────────────────────────────────────

    /**
     * Enregistre un achat complet (entête + toutes les lignes) dans la DB.
     *
     * @param orderReference référence de commande unique (ex : "RF-20240428123045")
     * @param customerEmail  email du client
     * @param items          liste des articles du panier
     * @param total          montant total payé
     * @param userId         id de l'utilisateur connecté (null si non connecté)
     * @return l'objet HistoriqueAchat inséré avec son id généré
     * @throws SQLException en cas d'erreur DB
     */
    public HistoriqueAchat enregistrerAchat(String orderReference,
                                            String customerEmail,
                                            List<PanierItem> items,
                                            double total,
                                            Long userId) throws SQLException {
        // 1. Insérer l'entête de commande
        HistoriqueAchat achat = new HistoriqueAchat(
                orderReference, customerEmail, total,
                LocalDateTime.now(), userId, "PAYE");
        Long achatId = insertAchat(achat);
        achat.setId(achatId);

        // 2. Insérer chaque ligne article
        List<HistoriqueAchatLigne> lignes = new ArrayList<>();
        for (PanierItem item : items) {
            Article article = item.getArticle();
            if (article == null) continue;

            String titreArticle = article.getTitre() != null ? article.getTitre() : "—";
            String nomMagasin   = (article.getMagasin() != null && article.getMagasin().getNom() != null)
                    ? article.getMagasin().getNom() : null;

            HistoriqueAchatLigne ligne = new HistoriqueAchatLigne(
                    achatId,
                    article.getIdArticle(),
                    titreArticle,
                    article.getPrix(),
                    item.getQuantity(),
                    item.getSubtotal(),
                    nomMagasin
            );
            Long ligneId = insertLigne(ligne);
            ligne.setId(ligneId);
            lignes.add(ligne);
        }
        achat.setLignes(lignes);

        System.out.println("[HistoriqueAchat] Commande enregistrée : " + achat);
        return achat;
    }

    // ─────────────────────────────────────────────────────────────
    // LECTURE
    // ─────────────────────────────────────────────────────────────

    /** Récupère tous les achats (sans les lignes). */
    public List<HistoriqueAchat> recupererTous() throws SQLException {
        List<HistoriqueAchat> list = new ArrayList<>();
        String sql = "SELECT * FROM historique_achat ORDER BY date_achat DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapAchat(rs));
            }
        }
        return list;
    }

    /** Récupère tous les achats d'un utilisateur. */
    public List<HistoriqueAchat> recupererParUser(Long userId) throws SQLException {
        List<HistoriqueAchat> list = new ArrayList<>();
        String sql = "SELECT * FROM historique_achat WHERE user_id = ? ORDER BY date_achat DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAchat(rs));
                }
            }
        }
        return list;
    }

    /** Récupère les lignes d'un achat donné. */
    public List<HistoriqueAchatLigne> recupererLignes(Long achatId) throws SQLException {
        List<HistoriqueAchatLigne> list = new ArrayList<>();
        String sql = "SELECT * FROM historique_achat_ligne WHERE achat_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, achatId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLigne(rs));
                }
            }
        }
        return list;
    }

    /** Récupère un achat complet (entête + lignes) par sa référence. */
    public HistoriqueAchat recupererParReference(String orderReference) throws SQLException {
        String sql = "SELECT * FROM historique_achat WHERE order_reference = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, orderReference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HistoriqueAchat achat = mapAchat(rs);
                    achat.setLignes(recupererLignes(achat.getId()));
                    return achat;
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────

    /** Insère un achat et retourne l'ID généré. */
    private Long insertAchat(HistoriqueAchat achat) throws SQLException {
        String sql = "INSERT INTO historique_achat "
                + "(order_reference, customer_email, total, date_achat, user_id, statut) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, achat.getOrderReference());
            ps.setString(2, achat.getCustomerEmail());
            ps.setDouble(3, achat.getTotal());
            ps.setTimestamp(4, Timestamp.valueOf(achat.getDateAchat()));
            if (achat.getUserId() != null) {
                ps.setLong(5, achat.getUserId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setString(6, achat.getStatut());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Impossible de récupérer l'ID de l'achat inséré.");
    }

    /** Insère une ligne et retourne l'ID généré. */
    private Long insertLigne(HistoriqueAchatLigne ligne) throws SQLException {
        String sql = "INSERT INTO historique_achat_ligne "
                + "(achat_id, article_id, titre_article, prix_unitaire, quantite, sous_total, nom_magasin) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, ligne.getAchatId());
            if (ligne.getArticleId() != null) {
                ps.setLong(2, ligne.getArticleId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, ligne.getTitreArticle());
            ps.setDouble(4, ligne.getPrixUnitaire());
            ps.setInt(5, ligne.getQuantite());
            ps.setDouble(6, ligne.getSousTotal());
            if (ligne.getNomMagasin() != null) {
                ps.setString(7, ligne.getNomMagasin());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Impossible de récupérer l'ID de la ligne insérée.");
    }

    private HistoriqueAchat mapAchat(ResultSet rs) throws SQLException {
        HistoriqueAchat a = new HistoriqueAchat();
        a.setId(rs.getLong("id"));
        a.setOrderReference(rs.getString("order_reference"));
        a.setCustomerEmail(rs.getString("customer_email"));
        a.setTotal(rs.getDouble("total"));
        Timestamp ts = rs.getTimestamp("date_achat");
        a.setDateAchat(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        long uid = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : uid);
        a.setStatut(rs.getString("statut"));
        return a;
    }

    private HistoriqueAchatLigne mapLigne(ResultSet rs) throws SQLException {
        HistoriqueAchatLigne l = new HistoriqueAchatLigne();
        l.setId(rs.getLong("id"));
        l.setAchatId(rs.getLong("achat_id"));
        long artId = rs.getLong("article_id");
        l.setArticleId(rs.wasNull() ? null : artId);
        l.setTitreArticle(rs.getString("titre_article"));
        l.setPrixUnitaire(rs.getDouble("prix_unitaire"));
        l.setQuantite(rs.getInt("quantite"));
        l.setSousTotal(rs.getDouble("sous_total"));
        l.setNomMagasin(rs.getString("nom_magasin"));
        return l;
    }
}
