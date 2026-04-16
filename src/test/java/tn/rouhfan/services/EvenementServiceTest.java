package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tn.rouhfan.entities.Evenement;
import tn.rouhfan.entities.Sponsor;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvenementServiceTest {

    static EvenementService evenementService;
    static SponsorService sponsorService;
    static int evenementId;
    static int sponsorId;

    @BeforeAll
    static void setup() throws SQLException {
        evenementService = new EvenementService();
        sponsorService = new SponsorService();

        Sponsor sponsor = new Sponsor(
                "Test Sponsor Evenement",
                "logo-test.png",
                "Sponsor événementiel",
                "test.evenement@example.com",
                "+21687654321",
                "Sousse, Tunisie",
                new Date()
        );
        sponsorService.ajouter(sponsor);
        sponsorId = sponsor.getId();
    }

    @Test
    @Order(1)
    void testAjouterEvenement() throws SQLException {
        Sponsor sponsor = sponsorService.findById(sponsorId);
        assertNotNull(sponsor);

        Evenement evenement = new Evenement(
                "Concert de Test",
                "Description du concert de test",
                "event-test.jpg",
                "Concert",
                "PLANIFIÉ",
                new Date(System.currentTimeMillis() + 86400000L),
                "Palais des congrès",
                150,
                0,
                null,
                sponsor
        );

        evenementService.ajouter(evenement);
        assertTrue(evenement.getId() > 0, "L'ID de l'événement doit être défini après l'insertion");
        evenementId = evenement.getId();

        List<Evenement> evenements = evenementService.recuperer();
        assertTrue(evenements.stream().anyMatch(ev -> ev.getId() == evenementId));
    }

    @Test
    @Order(2)
    void testModifierEvenement() throws SQLException {
        Evenement evenement = evenementService.findById(evenementId);
        assertNotNull(evenement);

        evenement.setTitre("Concert Modifié");
        evenement.setType("Festival");
        evenementService.modifier(evenement);

        Evenement updated = evenementService.findById(evenementId);
        assertNotNull(updated);
        assertEquals("Concert Modifié", updated.getTitre());
        assertEquals("Festival", updated.getType());
    }

    @Test
    @Order(3)
    void testSupprimerEvenement() throws SQLException {
        evenementService.supprimer(evenementId);
        Evenement deleted = evenementService.findById(evenementId);
        assertNull(deleted, "L'événement doit être supprimé de la base");
    }

    @Test
    @Order(4)
    void testValiderTypeEvenementInvalide() {
        Evenement invalidEvent = new Evenement();
        invalidEvent.setTitre("Titre test");
        invalidEvent.setDateEvent(new Date(System.currentTimeMillis() + 86400000L));
        invalidEvent.setLieu("Test lieu");
        invalidEvent.setType("Type invalide");
        invalidEvent.setCapacite(100);
        invalidEvent.setNbParticipants(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> evenementService.valider(invalidEvent));
        assertTrue(exception.getMessage().contains("Type invalide"));
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (evenementId > 0) {
            Evenement existing = evenementService.findById(evenementId);
            if (existing != null) {
                evenementService.supprimer(evenementId);
            }
        }
        if (sponsorId > 0) {
            Sponsor existingSponsor = sponsorService.findById(sponsorId);
            if (existingSponsor != null) {
                sponsorService.supprimer(sponsorId);
            }
        }
    }
}
