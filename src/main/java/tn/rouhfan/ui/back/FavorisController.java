package tn.rouhfan.ui.back;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Favoris;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.FavorisService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.front.OeuvreCardController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FavorisController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Categorie> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private FlowPane cardsPane;
    @FXML private ScrollPane scrollPane;

    private FavorisService favorisService = new FavorisService();
    private CategorieService categorieService = new CategorieService();
    private List<Oeuvre> allFavorites = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        loadFavoris();
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

        List<Oeuvre> sorted = filtered.stream().sorted(comparator).collect(Collectors.toList());

        displayOeuvres(sorted);
    }

    @FXML
    private void handleBackToGalerie() {
        javafx.scene.layout.Pane host = (javafx.scene.layout.Pane) searchField.getScene().lookup("#contentHost");
        if (host != null) {
            tn.rouhfan.ui.Router.setContent(host, "/ui/front/GalerieFront.fxml");
        }
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



