package tn.rouhfan.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.Qcm;
import tn.rouhfan.entities.QcmQuestion;
import tn.rouhfan.services.CoursService;
import tn.rouhfan.services.QcmService;
import tn.rouhfan.services.QcmQuestionService;

import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QcmServiceTest {
    static QcmService qcmService;
    static QcmQuestionService questionService;
    static CoursService coursService;
    static int idQcmTest;
    static int idQuestionTest;
    static int idCoursExistant;

    @BeforeAll
    static void setup() {
        qcmService = new QcmService();
        questionService = new QcmQuestionService();
        coursService = new CoursService();
        System.out.println("=== DÉBUT DES TESTS QCM ===");
    }

    @Test
    @Order(1)
    void testGetOrCreateQcm() throws SQLException {
        List<Cours> cours = coursService.recuperer();
        assertFalse(cours.isEmpty(), "Il doit y avoir au moins un cours");

        idCoursExistant = cours.get(0).getId();
        System.out.println("Cours utilisé ID: " + idCoursExistant);

        Qcm qcm = qcmService.getOrCreateQcm(idCoursExistant);
        assertNotNull(qcm, "Le QCM ne doit pas être null");

        idQcmTest = qcm.getId();
        System.out.println("ID QCM: " + idQcmTest);
    }

    @Test
    @Order(2)
    void testAjouterQuestion() throws SQLException {
        Qcm qcm = new Qcm();
        qcm.setId(idQcmTest);

        QcmQuestion question = new QcmQuestion();
        question.setQcm(qcm);
        question.setQuestion("Question test ?");

        questionService.ajouter(question);

        List<QcmQuestion> questions = questionService.recupererParQcm(idQcmTest);
        assertFalse(questions.isEmpty(), "La liste des questions ne doit pas être vide");

        boolean trouve = questions.stream().anyMatch(q -> q.getQuestion().equals("Question test ?"));
        assertTrue(trouve, "La question ajoutée doit être trouvée");

        idQuestionTest = questions.get(questions.size() - 1).getId();
        System.out.println("ID question ajoutée: " + idQuestionTest);
    }

    @Test
    @Order(3)
    void testModifierQuestion() throws SQLException {
        QcmQuestion question = new QcmQuestion();
        question.setId(idQuestionTest);
        question.setQuestion("Question modifiée ?");

        questionService.modifier(question);

        List<QcmQuestion> questions = questionService.recupererParQcm(idQcmTest);
        boolean trouve = questions.stream().anyMatch(q -> q.getQuestion().equals("Question modifiée ?"));
        assertTrue(trouve, "La question modifiée doit être trouvée");

        System.out.println("✅ Question modifiée avec succès");
    }

    @Test
    @Order(4)
    void testSupprimerQuestion() throws SQLException {
        questionService.supprimer(idQuestionTest);

        List<QcmQuestion> questions = questionService.recupererParQcm(idQcmTest);
        boolean existe = questions.stream().anyMatch(q -> q.getId() == idQuestionTest);
        assertFalse(existe, "La question ne doit plus exister");

        System.out.println("✅ Question supprimée avec succès");
    }
}