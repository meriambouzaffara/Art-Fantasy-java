package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.Magasin;
import tn.rouhfan.services.ArticleService;
import tn.rouhfan.services.MagasinService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ArticleController implements Initializable {

    // ── FXML barre ────────────────────────────────────────────────
    @FXML private TextField         searchField;
    @FXML private ComboBox<Magasin> filterMagasin;
    @FXML private Label             countLabel;
    @FXML private Label             contextLabel;
    @FXML private FlowPane          articlesFlow;
    @FXML private VBox              emptyMessage;

    // ── FXML formulaire ───────────────────────────────────────────
    @FXML private VBox              formPanel;
    @FXML private Label             formTitle;
    @FXML private TextField         titreField;
    @FXML private TextField         referenceField;
    @FXML private TextField         prixField;
    @FXML private TextField         stockField;
    @FXML private TextArea          descriptionField;
    @FXML private TextField         imageField;
    @FXML private ComboBox<Magasin> magasinCombo;
    @FXML private Label             errorLabel;

    // ── État ──────────────────────────────────────────────────────
    private ArticleService          articleService;
    private MagasinService          magasinService;
    private ObservableList<Article>  allArticles;
    private ObservableList<Magasin>  allMagasins;
    private boolean isEditMode       = false;
    private Article selectedForEdit  = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        articleService = new ArticleService();
        magasinService = new MagasinService();
        loadMagasins();
        loadAndRender();
        setupCombos();
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC : appelé depuis MagasinController
    // ─────────────────────────────────────────────────────────────
    public void filterByMagasin(Magasin m) {
        if (m == null) return;
        contextLabel.setText("Magasin : " + m.getNom());
        filterMagasin.getItems().stream()
                .filter(opt -> opt.getId() != null && opt.getId().equals(m.getId()))
                .findFirst()
                .ifPresent(opt -> filterMagasin.getSelectionModel().select(opt));
        List<Article> filtered = allArticles.stream()
                .filter(a -> a.getMagasin() != null
                        && m.getId() != null
                        && m.getId().equals(a.getMagasin().getId()))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    // ── Retour vers Magasins ──────────────────────────────────────
    @FXML
    private void goBackToMagasins(ActionEvent event) {
        try {
            VBox contentHost = (VBox) articlesFlow.getScene().lookup("#contentHost");
            Label pageTitle  = (Label) articlesFlow.getScene().lookup("#pageTitle");
            Node view = FXMLLoader.load(getClass().getResource("/ui/back/MagasinView.fxml"));
            if (pageTitle != null) pageTitle.setText("🖌️ Gestion Magasins");
            contentHost.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Chargement ────────────────────────────────────────────────
    private void loadMagasins() {
        try { allMagasins = FXCollections.observableArrayList(magasinService.recuperer()); }
        catch (SQLException e) { allMagasins = FXCollections.observableArrayList(); }
    }

    private void loadAndRender() {
        try {
            List<Article> raw = articleService.recuperer();
            for (Article a : raw) {
                if (a.getMagasin() != null && a.getMagasin().getId() != null) {
                    allMagasins.stream()
                            .filter(mg -> mg.getId().equals(a.getMagasin().getId()))
                            .findFirst().ifPresent(a::setMagasin);
                }
            }
            allArticles = FXCollections.observableArrayList(raw);
            renderCards(allArticles);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger : " + e.getMessage());
        }
    }

    // ── Rendu cartes ──────────────────────────────────────────────
    private void renderCards(List<Article> articles) {
        articlesFlow.getChildren().clear();
        boolean empty = articles == null || articles.isEmpty();
        emptyMessage.setVisible(empty);
        emptyMessage.setManaged(empty);
        articlesFlow.setVisible(!empty);
        articlesFlow.setManaged(!empty);
        if (!empty) {
            countLabel.setText(articles.size() + " article(s)");
            articles.forEach(a -> articlesFlow.getChildren().add(buildCard(a)));
        } else {
            countLabel.setText("0 article");
        }
    }

    private VBox buildCard(Article a) {
        VBox card = new VBox(0);
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 4);");

        // ── Bandeau coloré ─────────────────────────────────────
        StackPane banner = new StackPane();
        banner.setPrefHeight(100);
        banner.setStyle(
                "-fx-background-color: " + getGradient(a) + ";" +
                        "-fx-background-radius: 16 16 0 0;");

        Label icon = new Label(getIcon(a));
        icon.setStyle("-fx-font-size: 42;");

        // Badge stock
        Label stockBadge = new Label(getStockText(a.getStock()));
        stockBadge.setStyle(
                "-fx-background-color: " + getStockColor(a.getStock()) + ";" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;" +
                        "-fx-background-radius: 12; -fx-padding: 3 10;");
        StackPane.setAlignment(stockBadge, Pos.TOP_RIGHT);
        stockBadge.setTranslateX(-10);
        stockBadge.setTranslateY(10);
        banner.getChildren().addAll(icon, stockBadge);

        // ── Contenu texte ───────────────────────────────────────
        VBox content = new VBox(5);
        content.setStyle("-fx-padding: 12 14 4 14;");

        Label titre = new Label(a.getTitre() != null ? a.getTitre() : "—");
        titre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #241197;");
        titre.setWrapText(true);
        titre.setMaxWidth(232);

        String descTxt = a.getDescription() != null && !a.getDescription().isEmpty()
                ? (a.getDescription().length() > 55
                ? a.getDescription().substring(0, 52) + "..." : a.getDescription()) : "—";
        Label desc = new Label(descTxt);
        desc.setStyle("-fx-font-size: 11; -fx-text-fill: #5a4a72;");
        desc.setWrapText(true);
        desc.setMaxWidth(232);

        Label magNom = new Label("🏪 " + (a.getMagasin() != null && a.getMagasin().getNom() != null
                ? a.getMagasin().getNom() : "—"));
        magNom.setStyle("-fx-font-size: 11; -fx-text-fill: #6c2a90; -fx-font-weight: 500;");

        Label prix = new Label(String.format("%.2f DT", a.getPrix()));
        prix.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #c9a849;");

        content.getChildren().addAll(titre, desc, magNom, prix);

        // ── Boutons modifier / supprimer ────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setStyle("-fx-padding: 10 14 14 14;");

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setStyle(
                "-fx-background-color: #f0f2f9; -fx-text-fill: #241197; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 7 0; -fx-cursor: hand;");
        editBtn.setOnAction(e -> startEdit(a));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle(
                "-fx-background-color: #fff0f0; -fx-text-fill: #d63031; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 7 12; -fx-cursor: hand;" +
                        "-fx-border-color: #d63031; -fx-border-radius: 8;");
        deleteBtn.setOnAction(e -> deleteArticle(a));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(banner, content, actions);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(201,168,73,0.25), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(36,17,151,0.10), 14, 0, 0, 4);"));

        return card;
    }

    // ── Actions barre ─────────────────────────────────────────────
    @FXML private void refresh(ActionEvent e) {
        searchField.clear();
        filterMagasin.getSelectionModel().selectFirst();
        contextLabel.setText("Tous les articles");
        loadMagasins(); loadAndRender(); setupCombos(); hideForm(null);
    }
    @FXML private void onSearch()                     { applyFilters(); }
    @FXML private void onFilterMagasin(ActionEvent e) { applyFilters(); }

    private void applyFilters() {
        String q  = searchField.getText().trim().toLowerCase();
        Magasin mf = filterMagasin.getSelectionModel().getSelectedItem();
        List<Article> f = allArticles.stream().filter(a -> {
            boolean txt = q.isEmpty()
                    || (a.getTitre()       != null && a.getTitre().toLowerCase().contains(q))
                    || (a.getDescription() != null && a.getDescription().toLowerCase().contains(q));
            boolean mag = mf == null || "Tous les magasins".equals(mf.getNom())
                    || (a.getMagasin() != null && mf.getId() != null
                    && mf.getId().equals(a.getMagasin().getId()));
            return txt && mag;
        }).collect(Collectors.toList());
        renderCards(f);
    }

    // ── CRUD ──────────────────────────────────────────────────────
    @FXML private void showAddForm(ActionEvent e) {
        isEditMode=false; selectedForEdit=null;
        formTitle.setText("➕ Nouvel Article");
        clearForm(); setFormVisible(true);
    }

    private void startEdit(Article a) {
        isEditMode=true; selectedForEdit=a;
        formTitle.setText("✏️ Modifier — " + a.getTitre());
        populateForm(a); setFormVisible(true);
    }

    private void deleteArticle(Article a) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Supprimer"); c.setHeaderText("Supprimer « " + a.getTitre() + " » ?");
        c.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { articleService.supprimer(a.getIdArticle().intValue()); loadAndRender(); }
                catch (SQLException ex) { showAlert(Alert.AlertType.ERROR,"Erreur",ex.getMessage()); }
            }
        });
    }

    @FXML
    private void saveArticle(ActionEvent event) {
        hideError();
        String titre=titreField.getText().trim(), prixTx=prixField.getText().trim(),
                stTx=stockField.getText().trim();
        Magasin mag = magasinCombo.getSelectionModel().getSelectedItem();
        if (titre.isEmpty()) { showFormError("Titre obligatoire."); return; }
        if (mag == null)     { showFormError("Sélectionnez un magasin."); return; }
        double prix; int stock;
        try { prix=Double.parseDouble(prixTx); if(prix<0) throw new NumberFormatException(); }
        catch(NumberFormatException e) { showFormError("Prix invalide."); return; }
        try { stock=Integer.parseInt(stTx); if(stock<0) throw new NumberFormatException(); }
        catch(NumberFormatException e) { showFormError("Stock invalide."); return; }
        String reference = referenceField.getText().trim();
        try {
            if (isEditMode && selectedForEdit != null) {
                selectedForEdit.setTitre(titre);
                selectedForEdit.setReference(reference.isEmpty() ? null : reference);
                selectedForEdit.setPrix(prix);
                selectedForEdit.setStock(stock);
                selectedForEdit.setDescription(descriptionField.getText().trim());
                selectedForEdit.setImage(imageField.getText().trim());
                selectedForEdit.setMagasin(mag);
                articleService.modifier(selectedForEdit);
            } else {
                Article newArticle = new Article(titre, prix, stock,
                        descriptionField.getText().trim(), null,
                        imageField.getText().trim(), mag);
                newArticle.setReference(reference.isEmpty() ? null : reference);
                articleService.ajouter(newArticle);
            }
            loadAndRender(); setFormVisible(false); clearForm();
        } catch (SQLException e) { showFormError("Erreur DB : " + e.getMessage()); }
    }

    @FXML private void hideForm(ActionEvent e) { setFormVisible(false); clearForm(); hideError(); }

    private void setupCombos() {
        ObservableList<Magasin> opts = FXCollections.observableArrayList();
        Magasin tous = new Magasin(); tous.setNom("Tous les magasins");
        opts.add(tous); opts.addAll(allMagasins);
        filterMagasin.setItems(opts);
        filterMagasin.getSelectionModel().selectFirst();
        magasinCombo.setItems(allMagasins);
        javafx.util.StringConverter<Magasin> conv = new javafx.util.StringConverter<>() {
            @Override public String toString(Magasin m)   { return m!=null&&m.getNom()!=null?m.getNom():""; }
            @Override public Magasin fromString(String s) { return null; }
        };
        filterMagasin.setConverter(conv);
        magasinCombo.setConverter(conv);
    }

    // ── Utilitaires visuels ───────────────────────────────────────
    private String getGradient(Article a) {
        if (a.getMagasin() == null || a.getMagasin().getId() == null)
            return "linear-gradient(to bottom right, #241197, #6c2a90)";
        String[] g = {
                "linear-gradient(to bottom right, #241197, #6c2a90)",
                "linear-gradient(to bottom right, #6c2a90, #e84393)",
                "linear-gradient(to bottom right, #c9a849, #e4c76a)",
                "linear-gradient(to bottom right, #0984e3, #74b9ff)",
                "linear-gradient(to bottom right, #00b894, #55efc4)"
        };
        return g[(int)(a.getMagasin().getId() % g.length)];
    }

    private String getIcon(Article a) {
        if (a.getTitre() == null) return "🎨";
        String t = a.getTitre().toLowerCase();
        if (t.contains("peinture") || t.contains("tableau")) return "🖼️";
        if (t.contains("sculpture"))                          return "🗿";
        if (t.contains("dragon"))                             return "🐉";
        if (t.contains("bijou"))                              return "💎";
        if (t.contains("livre"))                              return "📚";
        if (t.contains("poster") || t.contains("affiche"))   return "📜";
        return "🎨";
    }

    private String getStockText(Integer s)  { if(s==null||s==0)return "Rupture"; if(s<=3)return s+" restant"+(s>1?"s":""); return "En stock"; }
    private String getStockColor(Integer s) { if(s==null||s==0)return "#d63031"; if(s<=3)return "#e17055"; return "#00b894"; }

    // ── Utilitaires formulaire ────────────────────────────────────
    private void populateForm(Article a) {
        titreField.setText(a.getTitre()!=null?a.getTitre():"");
        referenceField.setText(a.getReference()!=null?a.getReference():"");
        prixField.setText(String.valueOf(a.getPrix()));
        stockField.setText(String.valueOf(a.getStock()!=null?a.getStock():0));
        descriptionField.setText(a.getDescription()!=null?a.getDescription():"");
        imageField.setText(a.getImage()!=null?a.getImage():"");
        if (a.getMagasin()!=null)
            allMagasins.stream().filter(m->m.getId()!=null&&m.getId().equals(a.getMagasin().getId()))
                    .findFirst().ifPresent(m->magasinCombo.getSelectionModel().select(m));
    }
    private void clearForm()               { titreField.clear();referenceField.clear();prixField.clear();stockField.clear();descriptionField.clear();imageField.clear();magasinCombo.getSelectionModel().clearSelection(); }
    private void setFormVisible(boolean v) { formPanel.setVisible(v); formPanel.setManaged(v); }
    private void showFormError(String msg) { errorLabel.setText("⚠️ "+msg); errorLabel.setVisible(true); errorLabel.setManaged(true); }
    private void hideError()               { errorLabel.setVisible(false); errorLabel.setManaged(false); }
    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a=new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
