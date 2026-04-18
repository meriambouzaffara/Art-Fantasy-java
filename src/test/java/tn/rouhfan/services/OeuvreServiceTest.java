package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import tn.rouhfan.entities.Categorie;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OeuvreServiceTest {

    private static OeuvreService service;
    private static int testId;
    private static User testUser;
    private static Categorie testCategorie;

    @BeforeAll
    static void setup() throws SQLException {
        service = new OeuvreService();

        // Récupérer un utilisateur existant (supposé déjà présent en base)
        testUser = new UserService().recuperer().get(0);

        // Créer une catégorie de test si elle n'existe pas
        CategorieService categorieService = new CategorieService();
        List<Categorie> categories = categorieService.recuperer();
        if (categories.isEmpty()) {
            testCategorie = new Categorie();
            testCategorie.setNomCategorie("CatTest" + System.currentTimeMillis());
            testCategorie.setImageCategorie("image_cat.png");
            categorieService.ajouter(testCategorie);
            testCategorie = categorieService.recuperer().get(0);
        } else {
            testCategorie = categories.get(0);
        }
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        Oeuvre o = new Oeuvre();
        o.setTitre("Titre Test " + System.currentTimeMillis());
        o.setDescription("Description Test " + System.currentTimeMillis());
        o.setPrix(new BigDecimal("150.00"));
        o.setStatut("disponible");
        o.setImage("image.png");

        o.setUser(testUser);
        o.setCategorie(testCategorie);

        service.ajouter(o);
        testId = o.getId();

        assertTrue(testId > 0, "L'ID doit être généré");
    }

    @Test
    @Order(2)
    void testRecuperer() throws SQLException {
        List<Oeuvre> list = service.recuperer();
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test
    @Order(3)
    void testModifier() throws SQLException {
        Oeuvre o = service.findById(testId);
        assertNotNull(o);

        o.setTitre("Titre Modifié " + System.currentTimeMillis());
        service.modifier(o);

        Oeuvre updated = service.findById(testId);
        assertEquals(o.getTitre(), updated.getTitre());
    }

    @Test
    @Order(4)
    void testSupprimer() throws SQLException {
        service.supprimer(testId);
        Oeuvre o = service.findById(testId);
        assertNull(o, "L'œuvre devrait être supprimée");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (testId > 0) {
            Oeuvre existing = service.findById(testId);
            if (existing != null) {
                service.supprimer(testId);
                System.out.println("Nettoyage final : Œuvre supprimée (" + testId + ")");
            }
        }
    }
}
