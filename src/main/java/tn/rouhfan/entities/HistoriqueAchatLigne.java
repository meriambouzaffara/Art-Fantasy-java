package tn.rouhfan.entities;

/**
 * Représente une ligne de commande dans historique_achat_ligne.
 * Un HistoriqueAchat possède plusieurs HistoriqueAchatLigne (un par article acheté).
 */
public class HistoriqueAchatLigne {

    private Long   id;
    private Long   achatId;
    private Long   articleId;
    private String titreArticle;
    private double prixUnitaire;
    private int    quantite;
    private double sousTotal;
    private String nomMagasin;

    // ── Constructeurs ─────────────────────────────────────────────

    public HistoriqueAchatLigne() {}

    /** Constructeur sans ID (pour l'insertion). */
    public HistoriqueAchatLigne(Long achatId, Long articleId, String titreArticle,
                                double prixUnitaire, int quantite,
                                double sousTotal, String nomMagasin) {
        this.achatId       = achatId;
        this.articleId     = articleId;
        this.titreArticle  = titreArticle;
        this.prixUnitaire  = prixUnitaire;
        this.quantite      = quantite;
        this.sousTotal     = sousTotal;
        this.nomMagasin    = nomMagasin;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAchatId() { return achatId; }
    public void setAchatId(Long achatId) { this.achatId = achatId; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getTitreArticle() { return titreArticle; }
    public void setTitreArticle(String titreArticle) { this.titreArticle = titreArticle; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getSousTotal() { return sousTotal; }
    public void setSousTotal(double sousTotal) { this.sousTotal = sousTotal; }

    public String getNomMagasin() { return nomMagasin; }
    public void setNomMagasin(String nomMagasin) { this.nomMagasin = nomMagasin; }

    @Override
    public String toString() {
        return "Ligne{article='" + titreArticle + "', qte=" + quantite
                + ", sous-total=" + sousTotal + '}';
    }
}
