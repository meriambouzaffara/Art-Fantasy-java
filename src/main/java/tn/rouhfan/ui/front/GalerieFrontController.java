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
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.ui.back.OeuvreFormController;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

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

    private OeuvreService oeuvreService;
    private CategorieService categorieService;
    
    private ObservableList<Oeuvre> allOeuvres;
    private ObservableList<Categorie> allCategories;
    
    private boolean isCategoryMode = false;
    private String userRole = "PARTICIPANT"; // Default to PARTICIPANT

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        categorieService = new CategorieService();
        
        setupFilters();
        loadData();
    }

    public void setUserRole(String role) {
        this.userRole = role;
        setupRoleUI();
    }

    private void setupRoleUI() {
        if ("ARTIST".equals(userRole) && !isCategoryMode) {
            addOeuvreBtn.setVisible(true);
            addOeuvreBtn.setManaged(true);
        } else {
            addOeuvreBtn.setVisible(false);
            addOeuvreBtn.setManaged(false);
        }
    }

    public void setCategoryMode(boolean isCategoryMode) {
        this.isCategoryMode = isCategoryMode;
        if (isCategoryMode) {
            pageTitle.setText("📚 Nos Catégories");
            pageSubtitle.setText("Explorez l'art par styles et techniques.");
            filterBar.setVisible(false);
            filterBar.setManaged(false);
        } else {
            pageTitle.setText("🖼️ Galerie d'Art");
            pageSubtitle.setText("Découvrez les créations de nos artistes.");
            filterBar.setVisible(true);
            filterBar.setManaged(true);
            setupRoleUI();
        }
        loadData();
    }

    private void setupFilters() {
        // Category Combo display
        categoryFilter.setConverter(new StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomCategorie(); }
            @Override public Categorie fromString(String string) { return null; }
        });

        searchField.textProperty().addListener((obs, old, newValue) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        orderCombo.valueProperty().addListener((obs, old, newValue) -> applyFilters());
        
        sortCombo.setItems(FXCollections.observableArrayList("Titre", "Prix"));
        sortCombo.setValue("Titre");
        
        orderCombo.setItems(FXCollections.observableArrayList("Asc", "Desc"));
        orderCombo.setValue("Asc");
    }

    private void loadData() {
        try {
            if (isCategoryMode) {
                allCategories = FXCollections.observableArrayList(categorieService.recuperer());
                renderCategoryCards();
            } else {
                allOeuvres = FXCollections.observableArrayList(oeuvreService.recuperer());
                allCategories = FXCollections.observableArrayList(categorieService.recuperer());
                
                Categorie all = new Categorie();
                all.setNomCategorie("Toutes");
                categoryFilter.getItems().clear();
                categoryFilter.getItems().add(all);
                categoryFilter.getItems().addAll(allCategories);
                categoryFilter.setValue(all);
                
                applyFilters();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                    sorted.setComparator(isAsc ? Comparator.comparing(Oeuvre::getPrix) : Comparator.comparing(Oeuvre::getPrix).reversed()); 
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
                e.printStackTrace();
            }
        }
    }

    private void renderCategoryCards() {
        cardsPane.getChildren().clear();
        for (Categorie c : allCategories) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/CategoryCard.fxml"));
                Parent card = loader.load();
                CategoryCardController controller = loader.getController();
                controller.setCategory(c, () -> {
                    // Action when category clicked: show artworks of this category
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
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleAddOeuvre() {
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
