package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.rouhfan.entities.Sponsor;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SponsorServiceTest {

    static SponsorService sponsorService;
    static int sponsorId;

    @BeforeAll
    static void setup() {
        sponsorService = new SponsorService();
    }

    @Test
    @Order(1)
    void testAjouterSponsor() throws SQLException {
        Sponsor sponsor = new Sponsor(
                "Test Sponsor UI",
                "logo-test.png",
                "Sponsor de test",
                "test.sponsor@example.com",
                "+21612345678",
                "Tunis, Tunisie",
                new Date()
        );

        sponsorService.ajouter(sponsor);
        assertTrue(sponsor.getId() > 0, "L'ID du sponsor doit être défini après l'insertion");
        sponsorId = sponsor.getId();

        List<Sponsor> sponsors = sponsorService.recuperer();
        assertFalse(sponsors.isEmpty());
        assertTrue(sponsors.stream().anyMatch(s -> s.getId() == sponsorId));
    }

    @Test
    @Order(2)
    void testModifierSponsor() throws SQLException {
        Sponsor sponsor = sponsorService.findById(sponsorId);
        assertNotNull(sponsor, "Le sponsor de test doit exister avant la modification");

        sponsor.setNom("Test Sponsor Modifié");
        sponsor.setEmail("modifie.sponsor@example.com");
        sponsorService.modifier(sponsor);

        Sponsor updated = sponsorService.findById(sponsorId);
        assertNotNull(updated);
        assertEquals("Test Sponsor Modifié", updated.getNom());
        assertEquals("modifie.sponsor@example.com", updated.getEmail());
    }

    @Test
    @Order(3)
    void testSupprimerSponsor() throws SQLException {
        sponsorService.supprimer(sponsorId);
        Sponsor deleted = sponsorService.findById(sponsorId);
        assertNull(deleted, "Le sponsor doit être supprimé de la base");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (sponsorId > 0) {
            Sponsor existing = sponsorService.findById(sponsorId);
            if (existing != null) {
                sponsorService.supprimer(sponsorId);
            }
        }
    }
}
