package tn.rouhfan.mains;

import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // ======= Initialisation des services =======
        UserService us = new UserService();
        SponsorService ss = new SponsorService();
        EvenementService es = new EvenementService();
        CategorieService cs = new CategorieService();
        OeuvreService os = new OeuvreService();
        FavorisService fs = new FavorisService();
        ReclamationService rs = new ReclamationService();
        ReponseReclamationService rrs = new ReponseReclamationService();

        CoursService coursService = new CoursService();
        CertificatService certificatService = new CertificatService();
        QcmService qcmService = new QcmService();

        // 🔥 AJOUT SERVICES
        MagasinService ms = new MagasinService();
        ArticleService as = new ArticleService();

        try {
            // ==========================================
            // USER
            // ==========================================
            User dbUser = us.findByEmail("maissa@test.com");
            if (dbUser == null) {
                User u = new User("maissa", "nfissi", "maissa@test.com", "123456", "[\"ROLE_ADMIN\"]", "actif", true, "admin");
                us.ajouter(u);
                dbUser = us.findByEmail("maissa@test.com");
                System.out.println("✅ Utilisateur ajouté !");
            }

            User dbRania = us.findByEmail("rania@test.com");
            if (dbRania == null) {
                User ur = new User("rania", "Test", "rania@test.com", "password123", "[\"ROLE_USER\"]", "actif", true, "artiste");
                us.ajouter(ur);
                dbRania = us.findByEmail("rania@test.com");
            }

            // =========================
            // SPONSOR
            // =========================
            Sponsor s = new Sponsor("Tunisie Booking", "logo.png", "desc", "tunbook@gmail.com", "12345888", "Tunis", new Date());
            ss.ajouter(s);
            System.out.println("\n Liste des sponsors :");
            List<Sponsor> sponsors = ss.recuperer();
            for (Sponsor sp : sponsors) {
                System.out.println(sp);
            }
            // =========================
            // EVENEMENT
            // =========================
            Evenement e = new Evenement("Formation", "desc", "img", "Culture", "actif", new Date(), "Marsa", 300, 0, null, s);
            es.ajouter(e);
            System.out.println("\n Liste des événements :");
            List<Evenement> evenements = es.recuperer();
            for (Evenement ev : evenements) {
                System.out.println(ev);
            }
            // =========================
            // MAGASIN (AJOUT)
            // =========================
            Magasin mag = new Magasin(
                    "Magasin Test",
                    "Tunis Centre",
                    "12345678",
                    "mag@test.com",
                    36.8,
                    10.1
            );
            ms.ajouter(mag);
            System.out.println("✅ Magasin ajouté !");

            // =========================
            // ARTICLE (AJOUT)
            // =========================
            Magasin existingMag = ms.recuperer().get(0);

            Article art = new Article(
                    "Article Test",
                    50.0,
                    10,
                    "Description test",
                    null,
                    "image.png",
                    existingMag
            );
            as.ajouter(art);
            System.out.println("✅ Article ajouté !");

            // =========================
            // AFFICHAGE MAGASINS
            // =========================
            System.out.println("\n Liste des magasins :");
            List<Magasin> mags = ms.recuperer();
            for (Magasin m : mags) {
                System.out.println(m);
            }

            // =========================
            // AFFICHAGE ARTICLES
            // =========================
            System.out.println("\n Liste des articles :");
            List<Article> articles = as.recuperer();
            for (Article a : articles) {
                System.out.println(a);
            }

            // =========================
            // CATEGORIE
            // =========================
            Categorie c = new Categorie("Peinture", "img.png");
            cs.ajouter(c);
            System.out.println("\n Liste des catégories :");
            List<Categorie> categories = cs.recuperer();
            for (Categorie cat : categories) {
                System.out.println(cat);
            }
            // =========================
            // OEUVRE
            // =========================
            Oeuvre o = new Oeuvre(
                    "Description oeuvre",
                    "Test Oeuvre",
                    new BigDecimal("100.00"),
                    "disponible",
                    "uploads/oeuvres/test.png",
                    false,
                    null,
                    dbUser,
                    c
            );
            os.ajouter(o);
            System.out.println("\n Liste des œuvres :");
            List<Oeuvre> oeuvres = os.recuperer();
            for (Oeuvre oe : oeuvres) {
                System.out.println(oe);
            }
            // =========================
            // FAVORIS
            // =========================
            if (dbUser != null) {
                fs.toggle(dbUser.getId(), o.getId());
            }

            // =========================
            // AFFICHAGE USERS
            // =========================
            System.out.println("\n Liste des utilisateurs :");
            for (User user : us.recuperer()) {
                System.out.println(user);
            }

            // =========================
            // RECLAMATION
            // =========================
            Reclamation r = new Reclamation(
                    "Problème livraison",
                    "Commande non reçue",
                    "en_cours",
                    new Date(),
                    dbUser.getId(),
                    "Livraison"
            );
            rs.ajouter(r);

            // =========================
            // REPONSE
            // =========================
            ReponseReclamation rr = new ReponseReclamation(
                    "Réponse sous 48h",
                    new Date(),
                    r.getId()
            );
            rrs.ajouter(rr);

        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}