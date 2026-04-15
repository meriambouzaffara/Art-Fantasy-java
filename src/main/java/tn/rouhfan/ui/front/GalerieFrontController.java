package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.tools.SessionManager;
import tn.rouhfan.ui.back.OeuvreFormController;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GalerieFrontController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private TextField searchField;
    @FXML private ComboBox<Categorie> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private FlowPane cardsPane;
    @FXML private Button addOeuvreBtn;
    @FXML private HBox filterBar;
    @FXML private VBox categoryFilterBox;
    @FXML private VBox sortBox;
    @FXML private VBox orderBox;

    // Recherche catégories
    @FXML private HBox categorySearchBar;
    @FXML private TextField categorySearchField;

    private OeuvreService oeuvreService;
    private CategorieService categorieService;
    
    private ObservableList<Oeuvre> allOeuvres;
    private ObservableList<Categorie> allCategories;
    
    private boolean isCategoryMode = false;
    private User currentUser;
    private String userRole = "ROLE_PARTICIPANT";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        categorieService = new CategorieService();
        
        SessionManager session = SessionManager.getInstance();
        currentUser = session.getCurrentUser();
        if (currentUser != null) {
            userRole = session.getRole();
            System.out.println("[GalerieFront] User connecté: " + currentUser.getNom() + " | Rôle: " + userRole);
        } else {
            userRole = "ROLE_ANONYMOUS";
            System.out.println("[GalerieFront] Aucun user connecté");
        }
        
        setupFilters();
    }

    private void setupRoleUI() {
        boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
        boolean isArtiste = userRole != null && (userRole.toUpperCase().contains("ARTIST") || userRole.toUpperCase().contains("ARTISTE"));
        boolean canAddOeuvre = !isCategoryMode && (isAdmin || isArtiste);
        
        addOeuvreBtn.setVisible(canAddOeuvre);
        addOeuvreBtn.setManaged(canAddOeuvre);
    }

    public void setCategoryMode(boolean isCategoryMode) {
        this.isCategoryMode = isCategoryMode;
        if (isCategoryMode) {
            pageTitle.setText("📚 Nos Catégories");
            pageSubtitle.setText("Explorez l'art par styles et techniques.");
            filterBar.setVisible(false);
            filterBar.setManaged(false);
            // Afficher la barre de recherche catégories
            categorySearchBar.setVisible(true);
            categorySearchBar.setManaged(true);
        } else {
            pageTitle.setText("🖼️ Galerie d'Art");
            pageSubtitle.setText("Découvrez les créations de nos artistes.");
            filterBar.setVisible(true);
            filterBar.setManaged(true);
            categorySearchBar.setVisible(false);
            categorySearchBar.setManaged(false);
            setupRoleUI();
        }
        loadData();
    }

    private void setupFilters() {
        categoryFilter.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });

        searchField.textProperty().addListener((obs, old, newValue) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        orderCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        
        // Recherche catégories en temps réel
        categorySearchField.textProperty().addListener((obs, old, newValue) -> filterCategories(newValue));

        sortCombo.setItems(FXCollections.observableArrayList("Titre", "Prix"));
        sortCombo.setValue("Titre");
        
        orderCombo.setItems(FXCollections.observableArrayList("Asc", "Desc"));
        orderCombo.setValue("Asc");
    }

    private void loadData() {
        try {
            if (isCategoryMode) {
                allCategories = FXCollections.observableArrayList(categorieService.recuperer());
                System.out.println("[GalerieFront] Catégories chargées: " + allCategories.size());
                renderCategoryCards();
            } else {
                allOeuvres = FXCollections.observableArrayList(oeuvreService.recuperer());
                allCategories = FXCollections.observableArrayList(categorieService.recuperer());
                System.out.println("[GalerieFront] Oeuvres chargées: " + allOeuvres.size() + " | Catégories: " + allCategories.size());
                
                Categorie all = new Categorie();
                all.setNomCategorie("Toutes");
                categoryFilter.getItems().clear();
                categoryFilter.getItems().add(all);
                categoryFilter.getItems().addAll(allCategories);
                categoryFilter.setValue(all);
                
                applyFilters();
            }
        } catch (SQLException e) {
            System.err.println("[GalerieFront] ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Filtre les catégories affichées en fonction du texte de recherche
     */
    private void filterCategories(String searchText) {
        if (!isCategoryMode || allCategories == null) return;
        
        String search = (searchText == null) ? "" : searchText.toLowerCase().trim();
        
        if (search.isEmpty()) {
            renderCategoryCards();
        } else {
            ObservableList<Categorie> filtered = FXCollections.observableArrayList(
                allCategories.stream()
                    .filter(c -> c.getNomCategorie().toLowerCase().contains(search))
                    .collect(Collectors.toList())
            );
            renderFilteredCategoryCards(filtered);
        }
    }

    private void applyFilters() {
        if (isCategoryMode || allOeuvres == null) return;

        FilteredList<Oeuvre> filtered = new FilteredList<>(allOeuvres, o -> {
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean matchesSearch = o.getTitre().toLowerCase().contains(search);
            
            Categorie selected = categoryFilter.getValue();
            boolean matchesCat = selected == null || selected.getNomCategorie().equals("Toutes") 
                || (o.getCategorie() != null && o.getCategorie().getIdCategorie() == selected.getIdCategorie());
            
            return matchesSearch && matchesCat;
        });

        SortedList<Oeuvre> sorted = new SortedList<>(filtered);
        String sort = sortCombo.getValue();
        String order = orderCombo.getValue();
        boolean isAsc = "Asc".equals(order);

        if (sort != null) {
            switch (sort) {
                case "Titre": 
                    sorted.setComparator(isAsc ? Comparator.comparing(Oeuvre::getTitre) : Comparator.comparing(Oeuvre::getTitre).reversed()); 
                    break;
                case "Prix": 
                    sorted.setComparator(isAsc ? Comparator.comparing(o -> o.getPrix() != null ? o.getPrix() : java.math.BigDecimal.ZERO) : Comparator.comparing((Oeuvre o) -> o.getPrix() != null ? o.getPrix() : java.math.BigDecimal.ZERO).reversed()); 
                    break;
            }
        }

        renderOeuvreCards(sorted);
    }

    private void renderOeuvreCards(ObservableList<Oeuvre> oeuvres) {
        cardsPane.getChildren().clear();
        for (Oeuvre o : oeuvres) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreCard.fxml"));
                Parent card = loader.load();
                OeuvreCardController controller = loader.getController();
                controller.setOeuvre(o, userRole, this::loadData);
                cardsPane.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("[GalerieFront] Erreur chargement carte oeuvre: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void renderCategoryCards() {
        renderFilteredCategoryCards(allCategories);
    }

    private void renderFilteredCategoryCards(ObservableList<Categorie> categories) {
        cardsPane.getChildren().clear();
        for (Categorie c : categories) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/CategoryCard.fxml"));
                Parent card = loader.load();
                CategoryCardController controller = loader.getController();
                controller.setCategory(c, () -> {
                    setCategoryMode(false);
                    for (Categorie cat : categoryFilter.getItems()) {
                        if (cat.getIdCategorie() == c.getIdCategorie()) {
                            categoryFilter.setValue(cat);
                            break;
                        }
                    }
                });
                cardsPane.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("[GalerieFront] Erreur chargement carte catégorie: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleAddOeuvre() {
        boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
        boolean isArtiste = userRole != null && (userRole.toUpperCase().contains("ARTIST") || userRole.toUpperCase().contains("ARTISTE"));

        if (!isAdmin && !isArtiste) {
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/back/OeuvreFormDialog.fxml"));
            Parent root = loader.load();
            OeuvreFormController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Publier une œuvre");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            if (controller.isSaved()) loadData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
