package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.UserService;

import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoursServiceTest {
    static CoursService service;
    static UserService userService;
    static int idCoursTest;
    static int idArtisteExistant;

    @BeforeAll
    static void setup() {
        service = new CoursService();
        userService = new UserService();
        System.out.println("=== DÉBUT DES TESTS COURS ===");
    }

    @Test
    @Order(1)
    void testAjouterCours() throws SQLException {
        List<User> users = userService.recuperer();
        assertFalse(users.isEmpty(), "Il doit y avoir au moins un utilisateur");

        idArtisteExistant = users.get(0).getId();
        System.out.println("Artiste utilisé ID: " + idArtisteExistant);

        Cours c = new Cours();
        c.setNom("TestCours");
        c.setDescription("Description test");
        c.setNiveau("Débutant");
        c.setDuree("10h");
        c.setStatut("Brouillon");
        c.setContenu("Contenu test");
        c.setArtiste(users.get(0));

        service.ajouter(c);

        List<Cours> cours = service.recuperer();
        assertFalse(cours.isEmpty());

        boolean trouve = cours.stream().anyMatch(crs -> crs.getNom().equals("TestCours"));
        assertTrue(trouve, "Le cours ajouté doit être trouvé");

        idCoursTest = cours.get(cours.size() - 1).getId();
        System.out.println("ID cours ajouté: " + idCoursTest);
    }

    @Test
    @Order(2)
    void testModifierCours() throws SQLException {
        assertNotEquals(0, idArtisteExistant, "L'ID de l'artiste doit être défini");

        Cours c = new Cours();
        c.setId(idCoursTest);
        c.setNom("CoursModifie");
        c.setDescription("Description modifiée");
        c.setNiveau("Avancé");
        c.setDuree("20h");
        c.setStatut("Publié");
        c.setContenu("Contenu modifié");

        User artiste = new User();
        artiste.setId(idArtisteExistant);
        c.setArtiste(artiste);

        service.modifier(c);

        Cours coursModifie = service.findById(idCoursTest);
        assertNotNull(coursModifie, "Le cours doit exister après modification");
        assertEquals("CoursModifie", coursModifie.getNom(), "Le nom doit être modifié");
        assertEquals("Avancé", coursModifie.getNiveau(), "Le niveau doit être modifié");
        assertEquals("Publié", coursModifie.getStatut(), "Le statut doit être modifié");

        System.out.println("✅ Cours modifié avec succès");
    }

    @Test
    @Order(3)
    void testSupprimerCours() throws SQLException {
        service.supprimer(idCoursTest);

        Cours cours = service.findById(idCoursTest);
        assertNull(cours, "Le cours ne doit plus exister après suppression");

        System.out.println("✅ Cours supprimé avec succès");
    }
}