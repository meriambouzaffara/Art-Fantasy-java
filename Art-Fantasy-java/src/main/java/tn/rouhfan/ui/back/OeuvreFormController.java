package tn.rouhfan.ui.back;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CategorieService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.services.UserService;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class OeuvreFormController implements Initializable {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField prixField;
    @FXML private ComboBox<Categorie> categorieCombo;
    @FXML private TextField imageField;
    @FXML private ComboBox<User> artisteCombo;

    private OeuvreService oeuvreService;
    private CategorieService categorieService;
    private UserService userService;
    private Oeuvre oeuvre;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        oeuvreService = new OeuvreService();
        categorieService = new CategorieService();
        userService = new UserService();
        loadCategories();
        loadArtistes();
    }

    private void loadCategories() {
        try {
            List<Categorie> categories = categorieService.recuperer();
            categorieCombo.getItems().addAll(categories);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadArtistes() {
        try {
            List<User> users = userService.recuperer();
            artisteCombo.getItems().addAll(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setOeuvre(Oeuvre oeuvre) {
        this.oeuvre = oeuvre;
        if (oeuvre != null) {
            titreField.setText(oeuvre.getTitre());
            descriptionField.setText(oeuvre.getDescription());
            prixField.setText(oeuvre.getPrix() != null ? oeuvre.getPrix().toString() : "");
            imageField.setText(oeuvre.getImage());
            categorieCombo.setValue(oeuvre.getCategorie());
            artisteCombo.setValue(oeuvre.getUser());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Oeuvre getOeuvre() {
        return oeuvre;
    }

    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(titreField.getScene().getWindow());
        if (selectedFile != null) {
            imageField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void save(ActionEvent event) {
        try {
            if (oeuvre == null) {
                oeuvre = new Oeuvre();
            }
            oeuvre.setTitre(titreField.getText());
            oeuvre.setDescription(descriptionField.getText());
            oeuvre.setPrix(new BigDecimal(prixField.getText()));
            oeuvre.setImage(imageField.getText());
            oeuvre.setCategorie(categorieCombo.getValue());
            oeuvre.setUser(artisteCombo.getValue());

            if (oeuvre.getId() == 0) {
                oeuvreService.ajouter(oeuvre);
            } else {
                oeuvreService.modifier(oeuvre);
            }
            saved = true;
            closeDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }
}
