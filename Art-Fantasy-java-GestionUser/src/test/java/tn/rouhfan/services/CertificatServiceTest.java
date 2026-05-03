package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import tn.rouhfan.entities.Certificat;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CertificatService;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.UserService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CertificatServiceTest {
    static CertificatService service;
    static CoursService coursService;
    static UserService userService;
    static int idCertificatTest;
    static int idCoursExistant;
    static int idUserExistant;

    @BeforeAll
    static void setup() throws SQLException {
        service = new CertificatService();
        coursService = new CoursService();
        userService = new UserService();
        System.out.println("=== DÉBUT DES TESTS CERTIFICAT ===");

        // Récupérer un cours existant
        List<Cours> cours = coursService.recuperer();
        if (!cours.isEmpty()) {
            idCoursExistant = cours.get(0).getId();
            System.out.println("Cours utilisé ID: " + idCoursExistant);
        }

        // Récupérer un utilisateur existant
        List<User> users = userService.recuperer();
        if (!users.isEmpty()) {
            idUserExistant = users.get(0).getId();
            System.out.println("Utilisateur utilisé ID: " + idUserExistant);
        }
    }

    @Test
    @Order(1)
    void testAjouterCertificat() throws SQLException {
        assertNotEquals(0, idCoursExistant, "Un cours doit exister");
        assertNotEquals(0, idUserExistant, "Un utilisateur doit exister");

        Cours cours = new Cours();
        cours.setId(idCoursExistant);

        User user = new User();
        user.setId(idUserExistant);

        Certificat c = new Certificat();
        c.setNom("TestCertificat");
        c.setNiveau("Débutant");
        c.setScore(new BigDecimal("75.5"));
        c.setDateObtention(new Date());
        c.setCours(cours);
        c.setParticipant(user);

        service.ajouter(c);

        List<Certificat> certificats = service.recuperer();
        assertFalse(certificats.isEmpty(), "La liste des certificats ne doit pas être vide");

        boolean trouve = certificats.stream().anyMatch(cert -> cert.getNom().equals("TestCertificat"));
        assertTrue(trouve, "Le certificat ajouté doit être trouvé");

        idCertificatTest = certificats.get(certificats.size() - 1).getId();
        System.out.println("ID certificat ajouté: " + idCertificatTest);
    }

    @Test
    @Order(2)
    void testModifierCertificat() throws SQLException {
        Cours cours = new Cours();
        cours.setId(idCoursExistant);

        User user = new User();
        user.setId(idUserExistant);

        Certificat c = new Certificat();
        c.setId(idCertificatTest);
        c.setNom("CertificatModifie");
        c.setNiveau("Avancé");
        c.setScore(new BigDecimal("90.0"));
        c.setDateObtention(new Date());
        c.setCours(cours);
        c.setParticipant(user);

        service.modifier(c);

        List<Certificat> certificats = service.recuperer();
        boolean trouve = certificats.stream().anyMatch(cert -> cert.getNom().equals("CertificatModifie"));
        assertTrue(trouve, "Le certificat modifié doit être trouvé");

        System.out.println("✅ Certificat modifié avec succès");
    }

    @Test
    @Order(3)
    void testSupprimerCertificat() throws SQLException {
        service.supprimer(idCertificatTest);

        List<Certificat> certificats = service.recuperer();
        boolean existe = certificats.stream().anyMatch(c -> c.getId() == idCertificatTest);
        assertFalse(existe, "Le certificat ne doit plus exister");

        System.out.println("✅ Certificat supprimé avec succès");
    }
}