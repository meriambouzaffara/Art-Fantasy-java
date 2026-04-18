package tn.rouhfan.ui.back;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Favoris;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.FavorisService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.front.OeuvreCardController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

public class FavorisController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Categorie> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private FlowPane cardsPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Button exportPDFButton;

    private FavorisService favorisService = new FavorisService();
    private CategorieService categorieService = new CategorieService();
    private List<Oeuvre> allFavorites = new ArrayList<>();
    private List<Oeuvre> currentFilteredOeuvres = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        loadFavoris();
        
        // Gérer la visibilité du bouton PDF selon le rôle
        String role = SessionManager.getInstance().getRole();
        if ("ROLE_PARTICIPANT".equals(role)) {
            exportPDFButton.setVisible(false);
            exportPDFButton.setManaged(false); // Pour ne pas laisser d'espace vide
        }
    }

    private void setupFilters() {
        categoryFilter.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });

        // Listeners pour mise à jour automatique
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        orderCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        try {
            List<Categorie> categories = categorieService.recuperer();
            Categorie all = new Categorie();
            all.setNomCategorie("Toutes");
            categoryFilter.getItems().add(all);
            categoryFilter.getItems().addAll(categories);
            categoryFilter.setValue(all);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadFavoris() {
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            List<Favoris> favorisList = favorisService.recupererParUser(userId);
            allFavorites = favorisList.stream()
                    .map(Favoris::getOeuvre)
                    .collect(Collectors.toList());
            
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        Categorie selectedCat = categoryFilter.getValue();

        List<Oeuvre> filtered = allFavorites.stream()
                .filter(o -> o.getTitre().toLowerCase().contains(search))
                .filter(o -> selectedCat == null || selectedCat.getNomCategorie().equals("Toutes")
                        || (o.getCategorie() != null && o.getCategorie().getIdCategorie() == selectedCat.getIdCategorie()))
                .collect(Collectors.toList());

        // Tri
        String sort = sortCombo.getValue();
        String order = orderCombo.getValue();
        boolean isAsc = "Asc".equalsIgnoreCase(order);

        Comparator<Oeuvre> comparator;
        if ("Prix".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(o -> o.getPrix() != null ? o.getPrix() : BigDecimal.ZERO);
        } else {
            comparator = Comparator.comparing(Oeuvre::getTitre, String.CASE_INSENSITIVE_ORDER);
        }

        if (!isAsc) comparator = comparator.reversed();

        currentFilteredOeuvres = filtered.stream().sorted(comparator).collect(Collectors.toList());

        displayOeuvres(currentFilteredOeuvres);
    }

    @FXML
    private void handleBackToGalerie() {
        javafx.scene.layout.Pane host = (javafx.scene.layout.Pane) searchField.getScene().lookup("#contentHost");
        if (host != null) {
            tn.rouhfan.ui.Router.setContent(host, "/ui/front/GalerieFront.fxml");
        }
    }

    @FXML
    private void handleExportPDF() {
        if (currentFilteredOeuvres.isEmpty()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("Mes_Favoris_RouhElFann.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(searchField.getScene().getWindow());
        if (file == null) return;

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Titre Principal
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new java.awt.Color(36, 17, 151));
            Paragraph title = new Paragraph("Œuvres favoris — Rouh el Fann", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(10f);
            document.add(title);

            // Date d'exportation
            com.lowagie.text.Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 12, java.awt.Color.BLACK);
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph meta = new Paragraph("Liste exportée le " + dateStr, metaFont);
            meta.setSpacingAfter(20f);
            document.add(meta);

            // Tableau
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{3f, 1.5f, 2f, 2.5f});

            // En-têtes
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.BLACK);
            java.awt.Color headBg = new java.awt.Color(240, 238, 245);
            String[] headers = {"Titre", "Prix (DT)", "Catégorie", "Publié par"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(headBg);
                cell.setPadding(10);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            // Données
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, java.awt.Color.BLACK);
            for (Oeuvre o : currentFilteredOeuvres) {
                table.addCell(new PdfPCell(new Phrase(o.getTitre(), bodyFont))).setPadding(8);
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", o.getPrix()), bodyFont))).setPadding(8);
                table.addCell(new PdfPCell(new Phrase(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "N/A", bodyFont))).setPadding(8);
                
                String author = "Inconnu";
                if (o.getUser() != null) {
                    author = o.getUser().getNom() + " " + (o.getUser().getPrenom() != null ? o.getUser().getPrenom() : "");
                }
                table.addCell(new PdfPCell(new Phrase(author, bodyFont))).setPadding(8);
            }

            document.add(table);
            document.close();

            showInfo("Succès", "Votre liste de favoris a été exportée en PDF avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur d'export", "Une erreur est survenue lors de la génération du PDF : " + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void displayOeuvres(List<Oeuvre> oeuvres) {
        cardsPane.getChildren().clear();
        String role = SessionManager.getInstance().getRole();

        for (Oeuvre o : oeuvres) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreCard.fxml"));
                Parent card = loader.load();
                
                OeuvreCardController controller = loader.getController();
                controller.setOeuvre(o, role, this::loadFavoris);
                
                cardsPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



