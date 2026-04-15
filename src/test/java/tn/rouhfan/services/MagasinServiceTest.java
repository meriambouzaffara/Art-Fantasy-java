package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import tn.rouhfan.entities.Magasin;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MagasinServiceTest {

    private static MagasinService service;
    private static long testId;

    @BeforeAll
    static void setup() {
        service = new MagasinService();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. AJOUTER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("✅ Ajouter un magasin valide")
    void testAjouter() throws SQLException {
        Magasin m = new Magasin(
                "Test Magasin " + System.currentTimeMillis(),
                "12 Avenue Test, Tunis",
                "71000000",
                "test" + System.currentTimeMillis() + "@test.tn",
                36.8065,
                10.1815
        );
        service.ajouter(m);

        // Vérifier qu'il apparaît dans la liste
        List<Magasin> list = service.recuperer();
        Magasin inserted = list.stream()
                .filter(mg -> mg.getNom().equals(m.getNom()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted, "Le magasin doit exister après ajout");
        testId = inserted.getId();
        assertTrue(testId > 0, "L'ID doit être généré et positif");
    }

    @Test
    @Order(2)
    @DisplayName("❌ Ajouter un magasin avec nom null doit lever une exception")
    void testAjouterNomNull() {
        Magasin m = new Magasin(null, "Adresse", "71000000", "test@test.tn", 0.0, 0.0);
        assertThrows(SQLException.class, () -> service.ajouter(m),
                "Un nom null doit lever une SQLException (contrainte NOT NULL)");
    }

    // ─────────────────────────────────────────────────────────────
    // 2. RÉCUPÉRER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("✅ Récupérer tous les magasins — liste non vide")
    void testRecuperer() throws SQLException {
        List<Magasin> list = service.recuperer();
        assertNotNull(list, "La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test
    @Order(4)
    @DisplayName("✅ Tous les magasins ont un nom non null")
    void testRecupererChamps() throws SQLException {
        List<Magasin> list = service.recuperer();
        for (Magasin m : list) {
            assertNotNull(m.getId(),  "ID ne doit pas être null");
            assertNotNull(m.getNom(), "Nom ne doit pas être null");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. FIND BY ID
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("✅ findById retourne le bon magasin")
    void testFindById() throws SQLException {
        Magasin found = service.findById((int) testId);
        assertNotNull(found, "Le magasin doit être trouvé par son ID");
        assertEquals(testId, (long) found.getId());
    }

    @Test
    @Order(6)
    @DisplayName("✅ findById avec ID inexistant retourne null")
    void testFindByIdInexistant() throws SQLException {
        Magasin found = service.findById(999999);
        assertNull(found, "Un ID inexistant doit retourner null");
    }

    // ─────────────────────────────────────────────────────────────
    // 4. MODIFIER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("✅ Modifier le nom du magasin")
    void testModifierNom() throws SQLException {
        Magasin m = service.findById((int) testId);
        assertNotNull(m);

        String nouveauNom = "Magasin Modifié " + System.currentTimeMillis();
        m.setNom(nouveauNom);
        service.modifier(m);

        Magasin updated = service.findById((int) testId);
        assertEquals(nouveauNom, updated.getNom(), "Le nom doit être mis à jour");
    }

    @Test
    @Order(8)
    @DisplayName("✅ Modifier l'adresse et les coordonnées GPS")
    void testModifierAdresseGPS() throws SQLException {
        Magasin m = service.findById((int) testId);
        assertNotNull(m);

        m.setAdresse("Nouvelle Adresse, Sousse");
        m.setLatitude(35.8256);
        m.setLongitude(10.6369);
        service.modifier(m);

        Magasin updated = service.findById((int) testId);
        assertEquals("Nouvelle Adresse, Sousse", updated.getAdresse());
        assertEquals(35.8256, updated.getLatitude(), 0.0001);
        assertEquals(10.6369, updated.getLongitude(), 0.0001);
    }

    @Test
    @Order(9)
    @DisplayName("✅ Modifier l'email et le téléphone")
    void testModifierEmailTel() throws SQLException {
        Magasin m = service.findById((int) testId);
        assertNotNull(m);

        m.setEmail("nouveau" + System.currentTimeMillis() + "@test.tn");
        m.setTel("98765432");
        service.modifier(m);

        Magasin updated = service.findById((int) testId);
        assertEquals(m.getEmail(), updated.getEmail());
        assertEquals("98765432", updated.getTel());
    }

    // ─────────────────────────────────────────────────────────────
    // 5. UNICITÉ / CONTRAINTES
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("✅ Deux magasins peuvent avoir le même nom (pas de contrainte UNIQUE)")
    void testNomNonUnique() {
        String nom = "Doublon_" + System.currentTimeMillis();
        Magasin m1 = new Magasin(nom, "Adresse 1", "71000001", "a@test.tn", 36.0, 10.0);
        Magasin m2 = new Magasin(nom, "Adresse 2", "71000002", "b@test.tn", 36.1, 10.1);
        assertDoesNotThrow(() -> {
            service.ajouter(m1);
            service.ajouter(m2);
            // Nettoyage
            List<Magasin> list = service.recuperer();
            list.stream().filter(m -> m.getNom().equals(nom))
                    .forEach(m -> {
                        try { service.supprimer(m.getId().intValue()); }
                        catch (SQLException ignored) {}
                    });
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 6. SUPPRIMER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("✅ Supprimer le magasin de test")
    void testSupprimer() throws SQLException {
        service.supprimer((int) testId);
        Magasin deleted = service.findById((int) testId);
        assertNull(deleted, "Le magasin doit être null après suppression");
    }

    @Test
    @Order(12)
    @DisplayName("✅ Supprimer un ID inexistant ne lève pas d'exception")
    void testSupprimerIdInexistant() {
        assertDoesNotThrow(() -> service.supprimer(999999),
                "Supprimer un ID inexistant ne doit pas lever d'exception");
    }
}
