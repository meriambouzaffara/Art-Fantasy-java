package tn.rouhfan.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une commande payée, enregistrée dans historique_achat.
 */
public class HistoriqueAchat {

    private Long          id;
    private String        orderReference;
    private String        customerEmail;
    private double        total;
    private LocalDateTime dateAchat;
    private Long          userId;
    private String        statut;

    /** Lignes (articles) de la commande — chargées séparément. */
    private List<HistoriqueAchatLigne> lignes = new ArrayList<>();

    // ── Constructeurs ─────────────────────────────────────────────

    public HistoriqueAchat() {}

    /** Constructeur sans ID (pour l'insertion). */
    public HistoriqueAchat(String orderReference, String customerEmail,
                           double total, LocalDateTime dateAchat,
                           Long userId, String statut) {
        this.orderReference = orderReference;
        this.customerEmail  = customerEmail;
        this.total          = total;
        this.dateAchat      = dateAchat != null ? dateAchat : LocalDateTime.now();
        this.userId         = userId;
        this.statut         = statut != null ? statut : "PAYE";
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public LocalDateTime getDateAchat() { return dateAchat; }
    public void setDateAchat(LocalDateTime dateAchat) { this.dateAchat = dateAchat; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public List<HistoriqueAchatLigne> getLignes() { return lignes; }
    public void setLignes(List<HistoriqueAchatLigne> lignes) { this.lignes = lignes; }

    @Override
    public String toString() {
        return "HistoriqueAchat{id=" + id
                + ", ref='" + orderReference + '\''
                + ", email='" + customerEmail + '\''
                + ", total=" + total
                + ", date=" + dateAchat
                + ", statut='" + statut + '\'' + '}';
    }
}
