package tn.rouhfan.services;

import org.junit.jupiter.api.Test;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReponseReclamationServiceTest {

    @Test
    public void testAjouterEtRecuperer() throws SQLException {

        ReclamationService rs = new ReclamationService();
        ReponseReclamationService rrs = new ReponseReclamationService();

        //  1. créer une réclamation (IMPORTANT)
        Reclamation r = new Reclamation(
                "Sujet test",
                "Description test",
                "en_cours",
                new Date(),
                1,
                "Test"
        );

        rs.ajouter(r);

        //  2. utiliser SON ID (auto)
        ReponseReclamation rr = new ReponseReclamation(
                "Test réponse",
                new Date(),
                r.getId()   // ✅ dynamique
        );

        rrs.ajouter(rr);

        // 3. test
        List<ReponseReclamation> list = rrs.recuperer();

        assertFalse(list.isEmpty(), "Les réponses ne doivent pas être vides");
    }
}