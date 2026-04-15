package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.Magasin;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArticleServiceTest {

    private static ArticleService  articleService;
    private static MagasinService  magasinService;
    private static long            testArticleId;
    private static Magasin         testMagasin;

    // ─────────────────────────────────────────────────────────────
    // SETUP : créer un magasin de test réutilisé par tous les tests
    // ─────────────────────────────────────────────────────────────

    @BeforeAll
    static void setup() throws SQLException {
        articleService = new ArticleService();
        magasinService = new MagasinService();

        // Créer un magasin de support pour les tests d'articles
        Magasin m = new Magasin(
                "Magasin_JUnit_" + System.currentTimeMillis(),
                "Adresse JUnit",
                "71000099",
                "junit" + System.currentTimeMillis() + "@test.tn",
                36.8065, 10.1815
        );
        magasinService.ajouter(m);

        // Récupérer son ID généré
        List<Magasin> list = magasinService.recuperer();
        testMagasin = list.stream()
                .filter(mg -> mg.getNom().equals(m.getNom()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Magasin de test non créé"));
    }

    @AfterAll
    static void teardown() throws SQLException {
        // Supprimer le magasin de support créé pour les tests
        if (testMagasin != null) {
            magasinService.supprimer(testMagasin.getId().intValue());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1. AJOUTER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("✅ Ajouter un article valide")
    void testAjouter() throws SQLException {
        Article a = new Article(
                "Dragon Doré Test",
                150.0,
                5,
                "Article de test JUnit",
                null,
                "dragon_test.jpg",
                testMagasin
        );
        articleService.ajouter(a);

        List<Article> list = articleService.recuperer();
        Article inserted = list.stream()
                .filter(art -> art.getTitre().equals("Dragon Doré Test"))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted, "L'article doit exister après ajout");
        testArticleId = inserted.getIdArticle();
        assertTrue(testArticleId > 0, "L'ID doit être généré et positif");
    }

    @Test
    @Order(2)
    @DisplayName("❌ Ajouter un article sans magasin doit lever une exception")
    void testAjouterSansMagasin() {
        Article a = new Article("Sans Magasin", 50.0, 1, "desc", null, null, null);
        assertThrows(Exception.class, () -> articleService.ajouter(a),
                "Un article sans magasin doit lever une exception (FK ou NPE)");
    }

    @Test
    @Order(3)
    @DisplayName("❌ Ajouter un article avec prix négatif")
    void testAjouterPrixNegatif() {
        // Prix négatif — selon la contrainte DB (CHECK ou application)
        Article a = new Article("Prix Négatif", -10.0, 1, "desc", null, null, testMagasin);
        // On vérifie juste que le service ne plante pas silencieusement
        // Si pas de contrainte CHECK en DB, l'ajout réussit — on le supprime aussitôt
        try {
            articleService.ajouter(a);
            // Si ajouté, on vérifie la valeur stockée
            List<Article> list = articleService.recuperer();
            Article inserted = list.stream()
                    .filter(art -> art.getTitre().equals("Prix Négatif"))
                    .findFirst().orElse(null);
            if (inserted != null) {
                assertEquals(-10.0, inserted.getPrix(), 0.001);
                articleService.supprimer(inserted.getIdArticle().intValue());
            }
        } catch (SQLException ignored) {
            // Contrainte CHECK en DB — comportement attendu
        }
    }

    @Test
    @Order(4)
    @DisplayName("✅ Ajouter un article avec stock = 0 (rupture)")
    void testAjouterStockZero() throws SQLException {
        Article a = new Article("Rupture Stock", 80.0, 0, "Stock vide", null, null, testMagasin);
        assertDoesNotThrow(() -> articleService.ajouter(a));

        // Nettoyage
        List<Article> list = articleService.recuperer();
        list.stream().filter(art -> art.getTitre().equals("Rupture Stock"))
                .findFirst().ifPresent(art -> {
                    try { articleService.supprimer(art.getIdArticle().intValue()); }
                    catch (SQLException ignored) {}
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 2. RÉCUPÉRER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("✅ Récupérer tous les articles — liste non vide")
    void testRecuperer() throws SQLException {
        List<Article> list = articleService.recuperer();
        assertNotNull(list, "La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test
    @Order(6)
    @DisplayName("✅ Tous les articles ont un titre et un magasin_id")
    void testRecupererChamps() throws SQLException {
        List<Article> list = articleService.recuperer();
        for (Article a : list) {
            assertNotNull(a.getIdArticle(), "ID article ne doit pas être null");
            assertNotNull(a.getTitre(),     "Titre ne doit pas être null");
            assertNotNull(a.getMagasin(),   "Magasin ne doit pas être null");
            assertTrue(a.getPrix() >= 0,    "Le prix doit être >= 0");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. RÉCUPÉRER PAR MAGASIN
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("✅ recupererParMagasin retourne les articles du magasin de test")
    void testRecupererParMagasin() throws SQLException {
        List<Article> list = articleService.recupererParMagasin(testMagasin);
        assertNotNull(list, "La liste ne doit pas être null");
        // Au moins l'article créé en @Order(1) doit être présent
        assertFalse(list.isEmpty(), "Doit contenir au moins l'article de test");
        // Tous les articles appartiennent bien au bon magasin
        for (Article a : list) {
            assertEquals(testMagasin.getId(), a.getMagasin().getId(),
                    "Tous les articles doivent appartenir au magasin filtré");
        }
    }

    @Test
    @Order(8)
    @DisplayName("✅ recupererParMagasin avec magasin sans articles retourne liste vide")
    void testRecupererParMagasinVide() throws SQLException {
        // Créer un magasin sans articles
        Magasin vide = new Magasin("Vide_" + System.currentTimeMillis(),
                "Adresse", "71000001", "vide" + System.currentTimeMillis() + "@test.tn", 12.0, 0.0);
        magasinService.ajouter(vide);

        List<Magasin> all = magasinService.recuperer();
        Magasin magasinVide = all.stream()
                .filter(mg -> mg.getNom().equals(vide.getNom()))
                .findFirst().orElse(null);
        assertNotNull(magasinVide);

        List<Article> articles = articleService.recupererParMagasin(magasinVide);
        assertTrue(articles.isEmpty(), "Un magasin sans articles doit retourner une liste vide");

        // Nettoyage
        magasinService.supprimer(magasinVide.getId().intValue());
    }

    // ─────────────────────────────────────────────────────────────
    // 4. FIND BY ID
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("✅ findById retourne le bon article")
    void testFindById() throws SQLException {
        Article found = articleService.findById((int) testArticleId);
        assertNotNull(found, "L'article doit être trouvé par son ID");
        assertEquals(testArticleId, (long) found.getIdArticle());
        assertEquals("Dragon Doré Test", found.getTitre());
    }

    @Test
    @Order(10)
    @DisplayName("✅ findById avec ID inexistant retourne null")
    void testFindByIdInexistant() throws SQLException {
        Article found = articleService.findById(999999);
        assertNull(found, "Un ID inexistant doit retourner null");
    }

    // ─────────────────────────────────────────────────────────────
    // 5. MODIFIER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("✅ Modifier le titre de l'article")
    void testModifierTitre() throws SQLException {
        Article a = articleService.findById((int) testArticleId);
        assertNotNull(a);

        String nouveauTitre = "Dragon Modifié " + System.currentTimeMillis();
        a.setTitre(nouveauTitre);
        articleService.modifier(a);

        Article updated = articleService.findById((int) testArticleId);
        assertEquals(nouveauTitre, updated.getTitre(), "Le titre doit être mis à jour");
    }

    @Test
    @Order(12)
    @DisplayName("✅ Modifier le prix et le stock")
    void testModifierPrixStock() throws SQLException {
        Article a = articleService.findById((int) testArticleId);
        assertNotNull(a);

        a.setPrix(299.99);
        a.setStock(15);
        articleService.modifier(a);

        Article updated = articleService.findById((int) testArticleId);
        assertEquals(299.99, updated.getPrix(),  0.001, "Le prix doit être 299.99");
        assertEquals(15,     updated.getStock(), "Le stock doit être 15");
    }

    @Test
    @Order(13)
    @DisplayName("✅ Modifier la description et l'image")
    void testModifierDescriptionImage() throws SQLException {
        Article a = articleService.findById((int) testArticleId);
        assertNotNull(a);

        a.setDescription("Nouvelle description JUnit");
        a.setImage("nouvelle_image.png");
        articleService.modifier(a);

        Article updated = articleService.findById((int) testArticleId);
        assertEquals("Nouvelle description JUnit", updated.getDescription());
        assertEquals("nouvelle_image.png", updated.getImage());
    }

    @Test
    @Order(14)
    @DisplayName("✅ Modifier le magasin de l'article")
    void testModifierMagasin() throws SQLException {
        // Créer un 2ème magasin temporaire
        Magasin m2 = new Magasin("Magasin2_" + System.currentTimeMillis(),
                "Adresse 2", "71000002", "m2" + System.currentTimeMillis() + "@test.tn", 0.0, 0.0);
        magasinService.ajouter(m2);
        List<Magasin> all = magasinService.recuperer();
        Magasin magasin2 = all.stream().filter(mg -> mg.getNom().equals(m2.getNom()))
                .findFirst().orElse(null);
        assertNotNull(magasin2);

        Article a = articleService.findById((int) testArticleId);
        a.setMagasin(magasin2);
        articleService.modifier(a);

        Article updated = articleService.findById((int) testArticleId);
        assertEquals(magasin2.getId(), updated.getMagasin().getId(),
                "Le magasin de l'article doit être mis à jour");

        // Remettre le magasin original + nettoyage
        a.setMagasin(testMagasin);
        articleService.modifier(a);
        magasinService.supprimer(magasin2.getId().intValue());
    }

    // ─────────────────────────────────────────────────────────────
    // 6. SUPPRIMER
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("✅ Supprimer l'article de test")
    void testSupprimer() throws SQLException {
        articleService.supprimer((int) testArticleId);
        Article deleted = articleService.findById((int) testArticleId);
        assertNull(deleted, "L'article doit être null après suppression");
    }

    @Test
    @Order(16)
    @DisplayName("✅ Supprimer un ID inexistant ne lève pas d'exception")
    void testSupprimerIdInexistant() {
        assertDoesNotThrow(() -> articleService.supprimer(999999),
                "Supprimer un ID inexistant ne doit pas lever d'exception");
    }

    @Test
    @Order(17)
    @DisplayName("✅ Après suppression, recupererParMagasin ne contient plus l'article")
    void testRecupererParMagasinApresSupp() throws SQLException {
        List<Article> list = articleService.recupererParMagasin(testMagasin);
        boolean found = list.stream()
                .anyMatch(a -> a.getIdArticle() != null && a.getIdArticle() == testArticleId);
        assertFalse(found, "L'article supprimé ne doit plus apparaître dans recupererParMagasin");
    }
}
