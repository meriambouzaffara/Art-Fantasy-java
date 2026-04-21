package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.FavorisService;
import tn.rouhfan.tools.ImageUtils;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.front.OeuvreCardController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class FavorisController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Categorie> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private FlowPane cardsPane;
    @FXML private ScrollPane scrollPane;
    @FXML private Button exportPDFButton;

    private FavorisService favorisService;
    private CategorieService categorieService;
    private ObservableList<Oeuvre> favorisList;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        favorisService = new FavorisService();
        categorieService = new CategorieService();
        currentUser = SessionManager.getInstance().getCurrentUser();

        setupFilters();
        loadFavoris();

        // Gérer la visibilité du bouton PDF : masqué pour les Participants
        String role = SessionManager.getInstance().getRole();
        if ("ROLE_PARTICIPANT".equals(role)) {
            exportPDFButton.setVisible(false);
            exportPDFButton.setManaged(false);
        }
    }

    private void setupFilters() {
        categoryFilter.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        orderCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        try {
            Categorie all = new Categorie();
            all.setNomCategorie("Toutes");
            categoryFilter.getItems().add(all);
            categoryFilter.getItems().addAll(categorieService.recuperer());
            categoryFilter.setValue(all);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadFavoris() {
        if (currentUser == null) return;
        try {
            favorisList = FXCollections.observableArrayList(favorisService.getFavoriteOeuvres(currentUser.getId()));
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        if (favorisList == null) return;

        FilteredList<Oeuvre> filtered = new FilteredList<>(favorisList, o -> {
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean matchesSearch = o.getTitre().toLowerCase().contains(search);

            Categorie selected = categoryFilter.getValue();
            boolean matchesCat = selected == null || selected.getNomCategorie().equals("Toutes")
                    || (o.getCategorie() != null && o.getCategorie().getIdCategorie() == selected.getIdCategorie());

            return matchesSearch && matchesCat;
        });

        SortedList<Oeuvre> sorted = new SortedList<>(filtered);
        String sort = sortCombo.getValue();
        boolean isAsc = "Asc".equals(orderCombo.getValue());

        if (sort != null) {
            Comparator<Oeuvre> comparator;
            if ("Prix".equals(sort)) {
                comparator = Comparator.comparing(o -> o.getPrix() != null ? o.getPrix() : BigDecimal.ZERO);
            } else {
                comparator = Comparator.comparing(Oeuvre::getTitre, String.CASE_INSENSITIVE_ORDER);
            }
            if (!isAsc) comparator = comparator.reversed();
            sorted.setComparator(comparator);
        }

        renderCards(sorted);
    }

    private void renderCards(ObservableList<Oeuvre> oeuvres) {
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

    @FXML
    private void handleBackToGalerie() {
        javafx.scene.layout.Pane host = (javafx.scene.layout.Pane) searchField.getScene().lookup("#contentHost");
        if (host != null) {
            tn.rouhfan.ui.Router.setContent(host, "/ui/front/GalerieFront.fxml");
        }
    }

    @FXML
    private void handleExportPDF() {
        if (currentUser == null || favorisList == null || favorisList.isEmpty()) return;
        
        try {
            java.io.File file = new java.io.File("Mes_Favoris_" + currentUser.getNom() + ".pdf");
            favorisService.exportFavoritesToPDF(currentUser.getId(), file.getAbsolutePath());
            
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF");
            alert.setHeaderText("Exportation réussie");
            alert.setContentText("Vos favoris ont été exportés dans : " + file.getAbsolutePath());
            alert.show();
            
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
