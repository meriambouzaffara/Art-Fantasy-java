package tn.esprit.services;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;


import tn.esprit.entities.Personne;
import tn.esprit.services.PersonneServices;

import java.sql.SQLException;
import java.util.List;



@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonneServiceTest {
    static PersonneServices service;

    @BeforeAll
    static void setup() {
        service = new PersonneServices();
    }

    static int idPersonneTest;

    @Test
    @Order(1)
    void testAjouterPersonne() throws SQLException {
        Personne p = new Personne("TestNom", "TestPrenom",22);
        service.ajouter(p);
        List<Personne> personnes = service.afficher();
        assertFalse(personnes.isEmpty());
        assertTrue(
                personnes.stream().anyMatch(pers -> pers.getNom().equals("TestNom")
                )
        );
        idPersonneTest = personnes.get(personnes.size()-1).getId();
        System.out.println(idPersonneTest);
    }

    @Test
    @Order(2)
    void testModifierPersonne() throws SQLException {
        Personne p = new Personne();
        p.setId(idPersonneTest);
        p.setNom("NomModifie");
        p.setPrenom("PrenomModifie");
        service.modifier(p);
        List<Personne> personnes = service.afficher();
        boolean trouve = personnes.stream()
                .anyMatch(per -> per.getNom().equals("NomModifie"));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerPersonne() throws SQLException {
        service.supprimer(idPersonneTest);
        List<Personne> personnes = service.afficher();
        boolean existe = personnes.stream()
                .anyMatch(p -> p.getId() == idPersonneTest);
        assertFalse(existe);
    }
    @AfterEach
    void cleanUp() throws SQLException {
        List<Personne> personnes = service.afficher();
        if (!personnes.isEmpty()) {
            Personne last = personnes.get(personnes.size() - 1);
            service.supprimer(last.getId());
        }
    }

}
