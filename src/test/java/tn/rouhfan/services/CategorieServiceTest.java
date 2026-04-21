package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import tn.rouhfan.entities.Categorie;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategorieServiceTest {

    private static CategorieService service;
    private static int testId;

    @BeforeAll
    static void setup() {
        service = new CategorieService();
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        Categorie c = new Categorie();
        c.setNomCategorie("Test Unique " + System.currentTimeMillis());
        c.setImageCategorie("test.png");
        
        service.ajouter(c);
        testId = c.getIdCategorie();
        
        assertTrue(testId > 0, "L'ID doit être généré");
    }

    @Test
    @Order(2)
    void testRecuperer() throws SQLException {
        List<Categorie> list = service.recuperer();
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test
    @Order(3)
    void testUnicite() {
        Categorie c = new Categorie();
        c.setNomCategorie("Nom Existant"); // À adapter selon vos données réelles si nécessaire
        
        // On essaie d'ajouter deux fois le même nom
        Categorie c1 = new Categorie();
        String nom = "Unique_" + System.currentTimeMillis();
        c1.setNomCategorie(nom);
        
        assertDoesNotThrow(() -> service.ajouter(c1));
        
        Categorie c2 = new Categorie();
        c2.setNomCategorie(nom);
        
        assertThrows(SQLException.class, () -> service.ajouter(c2), "Devrait lever une exception pour nom en double");
    }

    @Test
    @Order(4)
    void testModifier() throws SQLException {
        Categorie c = service.findById(testId);
        assertNotNull(c);
        
        c.setNomCategorie("Nom Modifié " + System.currentTimeMillis());
        service.modifier(c);
        
        Categorie updated = service.findById(testId);
        assertEquals(c.getNomCategorie(), updated.getNomCategorie());
    }

    @Test
    @Order(5)
    void testSupprimer() throws SQLException {
        service.supprimer(testId);
        Categorie c = service.findById(testId);
        assertNull(c, "La catégorie devrait être supprimée");
    }
}
