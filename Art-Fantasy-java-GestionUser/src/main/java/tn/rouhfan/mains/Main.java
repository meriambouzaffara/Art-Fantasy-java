package tn.rouhfan.mains;

import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {

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

        MagasinService ms = new MagasinService();
        ArticleService as = new ArticleService();

        try {

            // =========================
            // USER
            // =========================
            User dbUser = us.findByEmail("maissa@test.com");
            if (dbUser == null) {
                User u = new User("maissa", "nfissi", "maissa@test.com", "123456", "[\"ROLE_ADMIN\"]", "actif", true,
                        "admin");
                us.ajouter(u);
                dbUser = us.findByEmail("maissa@test.com");
            }
            User dbRania = us.findByEmail("rania@test.com");
            if (dbRania == null) {
                User ur = new User("rania", "Test", "rania@test.com", "password123", "[\"ROLE_USER\"]", "actif", true, "artiste");
                us.ajouter(ur);
                dbRania = us.findByEmail("rania@test.com");
                System.out.println("Utilisateur 'rania' ajouté !");
            }


            // =========================
            // MODIFIER USER PAR ID
            // =========================
            int idToModify = 27;
            User userToModif = us.findById(idToModify);
            if (userToModif != null) {
                userToModif.setNom("888888");
                userToModif.setPrenom("888888888");
                us.modifier(userToModif);
                System.out.println("Utilisateur 888 " + idToModify + " modifié avec succès.");
            } else {
                System.out.println("Utilisateur 888 " + idToModify + " non trouvé.");
            }

//            // =========================
//            // MODIFIER USER
//            // =========================
//            if (dbUser != null) {
//                dbUser.setNom("jjjjjia_Modifie");
//                dbUser.setPrenom("Ayari_Modifie");
//                dbUser.setEmail("dhia_" + System.currentTimeMillis() + "@test.com");
//                us.modifier(dbUser);
//                System.out.println("Utilisateur modifié");
//            }

            // =========================
            // SUPPRIMER USER
            // =========================
            int idToSupprimer = 55;
            System.out.println("Tentative de suppression de l'utilisateur ID: " + idToSupprimer);





            // =========================
            // MAGASIN
            // =========================
            Magasin mag = new Magasin("Magasin Test", "Tunis", "12345678", "mag@test.com", 36.8, 10.1);
            ms.ajouter(mag);

            Magasin existingMag = ms.recuperer().get(0);

            Article art = new Article("Article Test", 50.0, 10, "Desc", null, "img.png", existingMag);
            as.ajouter(art);

            // =========================
            // SPONSOR + EVENT
            // =========================
            Sponsor s = new Sponsor("Orange", "logo.png", "desc", "mail@gmail.com", "123", "Tunis", new Date());
            ss.ajouter(s);

            Evenement e = new Evenement("Formation", "desc", "img", "Culture", "actif", new Date(), "Marsa", 300, 0,
                    null, s);
            es.ajouter(e);

            // ==========================================
            // 4. TEST COURS (Modifié pour utiliser l'utilisateur existant)
            // ==========================================
            System.out.println("\n===== TEST COURS =====");
            Cours cours = new Cours();
            if (dbRania != null) {
                // On ajoute un timestamp au nom pour pouvoir tester plusieurs fois sans erreur de titre unique si elle existe
                cours.setNom("Formation Java " + new Date().getTime());
                cours.setDescription("Découverte de la programmation orientée objet");
                cours.setNiveau("Débutant");
                cours.setDuree("12h");
                cours.setStatut("actif");
                cours.setContenu("Module 1 : Les bases...");
                cours.setArtiste(dbRania);

                coursService.ajouter(cours);
                System.out.println(" Cours ajouté avec succès. ID : " + cours.getId());
            }

            // ==========================================
            // 5. TEST QCM (Ajouté)
            // ==========================================
            System.out.println("\n===== TEST QCM =====");
            if (cours.getId() != 0) {
                Qcm qcm = new Qcm();
                qcm.setScoreMinRequis(70.0f);
                qcm.setDureeMinutes(30);
                qcm.setCours(cours);

                qcmService.ajouter(qcm);
                System.out.println(" QCM ajouté pour le cours : " + cours.getNom());
            }

            // ==========================================
            // 6. TEST CERTIFICAT
            // ==========================================
            System.out.println("\n===== TEST CERTIFICAT =====");
            if (dbUser != null && cours.getId() != 0) {
                Certificat certificat = new Certificat();
                certificat.setNom("Certificat de Réussite Artiste");
                certificat.setNiveau("Débutant");
                certificat.setScore(new BigDecimal("85.00"));
                certificat.setDateObtention(new Date());
                certificat.setCours(cours);
                certificat.setParticipant(dbUser);

                certificatService.ajouter(certificat);
                System.out.println(" Certificat créé pour : " + dbUser.getNom());
            }

            System.out.println("\n===== TEST CATEGORIE / OEUVRE / FAVORIS =====");
            // =========================
            // CATEGORIE + OEUVRE
            // =========================
            Categorie c = new Categorie("Peinture", "img.png");
            cs.ajouter(c);

            Oeuvre o = new Oeuvre("desc", "titre", new BigDecimal("100"), "ok", "img.png", false, null, dbUser, c);
            os.ajouter(o);

            if (dbUser != null) {
                fs.toggle(dbUser.getId(), o.getId());
            }

//            // =========================
//            // AFFICHAGE USERS
//            // =========================
//            List<User> users = us.recuperer();
//            for (User user : users) {
//                System.out.println(user);
//            }
//            System.out.println("\n Liste des événements :");
//            List<Evenement> events = es.recuperer();
//            for (Evenement ev : events) {
//                System.out.println(ev);
//                if (ev.getSponsor() != null) {
//                    System.out.println("   ➜ Sponsor: " + ev.getSponsor().getNom());
//                }
//            }
//
//            System.out.println("\n Liste des catégories :");
//            List<Categorie> categories = cs.recuperer();
//            for (Categorie cat : categories) {
//                System.out.println(cat);
//            }
//            System.out.println("\n Liste des réclamations :");
//            List<Reclamation> recs = rs.recuperer();
//            for (Reclamation rec : recs) {
//                System.out.println(rec);
//            }
//
//            System.out.println("\n Liste des réponses aux réclamations :");
//            List<ReponseReclamation> reps = rrs.recuperer();
//            for (ReponseReclamation rep : reps) {
//                System.out.println(rep);
//            }
//            System.out.println("\n Liste des magasins :");
//            List<Magasin> mags = ms.recuperer();
//            for (Magasin m : mags) {
//                System.out.println(m);
//            }
//
//            // =========================
//            // AFFICHAGE ARTICLES
//            // =========================
//            System.out.println("\n Liste des articles :");
//            List<Article> articles = as.recuperer();
//            for (Article a : articles) {
//                System.out.println(a);
//            }
//
//            System.out.println("\n Liste des sponsors :");
//            List<Sponsor> sponsors = ss.recuperer();
//            for (Sponsor sp : sponsors) {
//                System.out.println(sp);
//            }
//
//            System.out.println("\n Liste des oeuvres :");
//            List<Oeuvre> oeuvres = os.recuperer();
//            for (Oeuvre oe : oeuvres) {
//                System.out.println(oe);
//                if (oe.getCategorie() != null) {
//                    System.out.println("   ➜ Categorie: " + oe.getCategorie().getNomCategorie());
//                }
//                if (oe.getUser() != null) {
//                    System.out.println("   ➜ User: " + oe.getUser().getNom());
//                }
//            }
//            if (dbUser != null) {
//                System.out.println("\n Liste des favoris (userId=" + dbUser.getId() + ") :");
//                List<Favoris> favoris = fs.recupererParUser(dbUser.getId());
//                for (Favoris fav : favoris) {
//                    System.out.println(fav);
//                }
//            }


        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        }
    }
}