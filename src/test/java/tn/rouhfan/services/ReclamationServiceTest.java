package tn.rouhfan.services;

import org.junit.jupiter.api.Test;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.services.ReclamationService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReclamationServiceTest {

    @Test
    public void testAjouterEtRecuperer() throws SQLException {

        ReclamationService rs = new ReclamationService();

        Reclamation r = new Reclamation(
                "Test Sujet",
                "Test Description",
                "en_attente",
                new Date(),
                1,
                "test"
        );

        rs.ajouter(r);

        List<Reclamation> list = rs.recuperer();

        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test
    public void testSupprimer() throws SQLException {

        ReclamationService rs = new ReclamationService();

        Reclamation r = new Reclamation(
                "A supprimer",
                "Test",
                "en_attente",
                new Date(),
                1,
                "test"
        );

        rs.ajouter(r);

        int id = r.getId();

        rs.supprimer(id);

        List<Reclamation> list = rs.recuperer();

        boolean exists = list.stream().anyMatch(rec -> rec.getId() == id);

        assertFalse(exists, "La réclamation doit être supprimée");
    }
}