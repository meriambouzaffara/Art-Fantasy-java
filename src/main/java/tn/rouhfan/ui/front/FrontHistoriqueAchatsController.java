package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.rouhfan.entities.HistoriqueAchat;
import tn.rouhfan.entities.HistoriqueAchatLigne;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.HistoriqueAchatService;
import tn.rouhfan.ui.Router;
import tn.rouhfan.tools.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur front-office pour l'historique des achats.
 * Affiche chaque commande sous forme de carte-ticket.
 */
public class FrontHistoriqueAchatsController implements Initializable {

    @FXML private VBox  ticketsBox;
    @FXML private Label totalLabel;
    @FXML private Label countLabel;

    private final HistoriqueAchatService historiqueService = new HistoriqueAchatService();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHistorique();
    }

    // ── Chargement ────────────────────────────────────────────────

    private void loadHistorique() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            List<HistoriqueAchat> achats;

            if (currentUser != null) {
                achats = historiqueService.recupererParUser((long) currentUser.getId());
            } else {
                achats = historiqueService.recupererTous();
            }

            int nb = achats.size();
            totalLabel.setText(nb + " commande" + (nb > 1 ? "s" : ""));
            countLabel.setText(nb + " résultat" + (nb > 1 ? "s" : ""));

            if (achats.isEmpty()) {
                ticketsBox.getChildren().add(buildEmptyState());
                return;
            }

            for (HistoriqueAchat achat : achats) {
                // Charger les lignes de chaque commande
                List<HistoriqueAchatLigne> lignes =
                        historiqueService.recupererLignes(achat.getId());
                achat.setLignes(lignes);
                ticketsBox.getChildren().add(buildTicketCard(achat));
            }

        } catch (SQLException e) {
            System.err.println("[HistoriqueAchats] Erreur chargement : " + e.getMessage());
            totalLabel.setText("Erreur de chargement");
            ticketsBox.getChildren().add(buildEmptyState());
        }
    }

    // ── Construction du ticket ────────────────────────────────────

    /**
     * Construit une carte-ticket pour une commande.
     * Design : bande colorée à gauche | contenu détaillé à droite.
     */
    private HBox buildTicketCard(HistoriqueAchat achat) {

        // ══ Carte principale ════════════════════════════════════════
        HBox card = new HBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 5);");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);

        // ══ Bande gauche (couleur + référence verticale) ════════════
        VBox leftBand = new VBox(8);
        leftBand.setPrefWidth(110);
        leftBand.setMinWidth(110);
        leftBand.setAlignment(Pos.CENTER);
        leftBand.setStyle("-fx-background-color: linear-gradient(to bottom, #241197, #6c2a90);"
                + "-fx-background-radius: 16 0 0 16; -fx-padding: 20 10;");

        Label iconLabel = new Label("📦");
        iconLabel.setStyle("-fx-font-size: 28;");

        // Référence courte (ex: RF-20260428)
        String refCourt = achat.getOrderReference() != null
                ? achat.getOrderReference().substring(0, Math.min(achat.getOrderReference().length(), 14))
                : "—";
        Label refLabel = new Label(refCourt);
        refLabel.setStyle("-fx-font-size: 9; -fx-text-fill: rgba(255,255,255,0.85);"
                + "-fx-font-weight: bold; -fx-wrap-text: true;");
        refLabel.setWrapText(true);
        refLabel.setMaxWidth(90);
        refLabel.setAlignment(Pos.CENTER);

        // Date
        String dateStr = achat.getDateAchat() != null
                ? achat.getDateAchat().format(DATE_FMT) : "—";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 8; -fx-text-fill: rgba(255,255,255,0.70);"
                + "-fx-wrap-text: true;");
        dateLabel.setWrapText(true);
        dateLabel.setMaxWidth(90);
        dateLabel.setAlignment(Pos.CENTER);

        leftBand.getChildren().addAll(iconLabel, refLabel, dateLabel);

        // ══ Ligne de perforation (tirets) ═══════════════════════════
        VBox perforation = new VBox();
        perforation.setPrefWidth(16);
        perforation.setMinWidth(16);
        perforation.setAlignment(Pos.CENTER);
        perforation.setStyle("-fx-background-color: #faf9fc;");

        // cercle haut + cercle bas + tirets au centre
        Label circleTop = new Label();
        circleTop.setPrefWidth(14);
        circleTop.setPrefHeight(14);
        circleTop.setStyle("-fx-background-color: #faf9fc; -fx-background-radius: 7;"
                + "-fx-border-color: #e0ddf0; -fx-border-radius: 7; -fx-border-width: 1;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label circleBottom = new Label();
        circleBottom.setPrefWidth(14);
        circleBottom.setPrefHeight(14);
        circleBottom.setStyle("-fx-background-color: #faf9fc; -fx-background-radius: 7;"
                + "-fx-border-color: #e0ddf0; -fx-border-radius: 7; -fx-border-width: 1;");

        perforation.getChildren().addAll(circleTop, spacer, circleBottom);

        // ══ Contenu droit ════════════════════════════════════════════
        VBox rightContent = new VBox(10);
        rightContent.setPadding(new Insets(18, 22, 18, 18));
        HBox.setHgrow(rightContent, Priority.ALWAYS);

        // En-tête : référence complète + badge statut
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label fullRef = new Label("🧾 " + (achat.getOrderReference() != null
                ? achat.getOrderReference() : "—"));
        fullRef.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #241197;");
        HBox.setHgrow(fullRef, Priority.ALWAYS);

        // Badge statut
        String statut = achat.getStatut() != null ? achat.getStatut() : "PAYÉ";
        Label statutBadge = new Label("✅ " + statut);
        statutBadge.setStyle("-fx-background-color: #e8f8f0; -fx-text-fill: #00b894;"
                + "-fx-font-weight: bold; -fx-font-size: 11;"
                + "-fx-background-radius: 10; -fx-padding: 4 10;");

        header.getChildren().addAll(fullRef, statutBadge);

        // Email client
        Label emailLabel = new Label("✉  " + (achat.getCustomerEmail() != null
                ? achat.getCustomerEmail() : "—"));
        emailLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #5a4a72;");

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: rgba(36,17,151,0.08);");

        // Liste des articles
        VBox articlesBox = new VBox(5);
        if (achat.getLignes() == null || achat.getLignes().isEmpty()) {
            Label noLine = new Label("Aucun détail disponible.");
            noLine.setStyle("-fx-font-size: 11; -fx-text-fill: #aaa;");
            articlesBox.getChildren().add(noLine);
        } else {
            for (HistoriqueAchatLigne ligne : achat.getLignes()) {
                articlesBox.getChildren().add(buildLigneRow(ligne));
            }
        }

        // Séparateur bas
        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setMaxWidth(Double.MAX_VALUE);
        sep2.setStyle("-fx-background-color: rgba(36,17,151,0.08);");

        // Pied : total
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label totalLbl = new Label(String.format("💰 Total : %.2f DT", achat.getTotal()));
        totalLbl.setStyle("-fx-font-size: 18; -fx-font-weight: 800; -fx-text-fill: #c9a849;");

        footer.getChildren().add(totalLbl);

        rightContent.getChildren().addAll(header, emailLabel, sep, articlesBox, sep2, footer);

        card.getChildren().addAll(leftBand, perforation, rightContent);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(201,168,73,0.22), 22, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 5);"));

        return card;
    }

    /** Construit une ligne article dans le ticket. */
    private HBox buildLigneRow(HistoriqueAchatLigne ligne) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #faf9fc; -fx-background-radius: 8; -fx-padding: 6 10;");

        // Tiret décoratif
        Label bullet = new Label("▸");
        bullet.setStyle("-fx-text-fill: #6c2a90; -fx-font-size: 12;");

        // Titre article
        Label titre = new Label(ligne.getTitreArticle() != null ? ligne.getTitreArticle() : "Article");
        titre.setStyle("-fx-font-size: 12; -fx-text-fill: #2d1b4e; -fx-font-weight: 600;");
        HBox.setHgrow(titre, Priority.ALWAYS);

        // Magasin (si disponible)
        if (ligne.getNomMagasin() != null && !ligne.getNomMagasin().isBlank()) {
            Label mag = new Label("🏪 " + ligne.getNomMagasin());
            mag.setStyle("-fx-font-size: 10; -fx-text-fill: #6c2a90;");
            row.getChildren().addAll(bullet, titre, mag);
        } else {
            row.getChildren().addAll(bullet, titre);
        }

        // Quantité
        Label qte = new Label("×" + ligne.getQuantite());
        qte.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72; -fx-font-weight: bold;"
                + "-fx-background-color: #ede8f8; -fx-background-radius: 8; -fx-padding: 2 7;");

        // Sous-total
        Label sous = new Label(String.format("%.2f DT", ligne.getSousTotal()));
        sous.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #c9a849;");

        row.getChildren().addAll(qte, sous);
        return row;
    }

    /** Message affiché si aucun achat. */
    private VBox buildEmptyState() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 0, 60, 0));

        Label icon = new Label("🛍️");
        icon.setStyle("-fx-font-size: 52;");

        Label msg = new Label("Aucun achat trouvé");
        msg.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #241197;");

        Label sub = new Label("Vos commandes confirmées apparaîtront ici.");
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #5a4a72;");

        box.getChildren().addAll(icon, msg, sub);
        return box;
    }

    // ── Navigation ────────────────────────────────────────────────

    @FXML
    private void goBackToCheckout() {
        VBox contentHost = (VBox) ticketsBox.getScene().lookup("#contentHost");
        if (contentHost != null) {
            Router.setContent(contentHost, "/ui/front/checkout.fxml");
        }
    }
}
