package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;
import tn.rouhfan.tools.SessionManager;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QcmPassageController {

    @FXML private Label lblTitreCours;
    @FXML private VBox questionsContainer;
    @FXML private Button btnValider;
    @FXML private Label lblInfo;

    private Cours currentCours;
    private Qcm currentQcm;
    private List<QcmQuestion> questions;
    private List<ToggleGroup> toggleGroups;
    private VBox contentHost;
    private VBox heroSection;

    private final ProgressionService progressionService = new ProgressionService();
    private final CertificatService certificatService = new CertificatService();

    public void initData(Cours cours, VBox contentHost, VBox heroSection) {
        this.currentCours = cours;
        this.contentHost = contentHost;
        this.heroSection = heroSection;
        this.toggleGroups = new ArrayList<>();
        lblTitreCours.setText("QCM : " + cours.getNom());
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            QcmService qcmService = new QcmService();
            currentQcm = qcmService.findByCoursId(currentCours.getId());

            if (currentQcm != null) {
                QcmQuestionService questionService = new QcmQuestionService();
                questions = questionService.recupererParQcm(currentQcm.getId());
                afficherQuestions();
            } else {
                questionsContainer.getChildren().add(new Label("Aucun QCM disponible pour ce cours"));
                btnValider.setDisable(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            btnValider.setDisable(true);
        }
    }

    private void afficherQuestions() {
        questionsContainer.getChildren().clear();
        toggleGroups.clear();

        if (questions == null || questions.isEmpty()) {
            questionsContainer.getChildren().add(new Label("Aucune question disponible"));
            btnValider.setDisable(true);
            return;
        }

        for (int i = 0; i < questions.size(); i++) {
            QcmQuestion q = questions.get(i);
            VBox questionBox = new VBox(10);
            questionBox.setStyle("-fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: white;");

            Label questionLabel = new Label((i + 1) + ". " + q.getQuestion());
            questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            questionBox.getChildren().add(questionLabel);

            try {
                QcmReponseService reponseService = new QcmReponseService();
                List<QcmReponse> reponses = reponseService.recupererParQuestion(q.getId());

                ToggleGroup group = new ToggleGroup();
                toggleGroups.add(group);

                for (QcmReponse reponse : reponses) {
                    RadioButton rb = new RadioButton(reponse.getTexte());
                    rb.setToggleGroup(group);
                    rb.setUserData(reponse);
                    rb.setStyle("-fx-padding: 5 0 5 20;");
                    questionBox.getChildren().add(rb);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            questionsContainer.getChildren().add(questionBox);
        }
    }

    @FXML
    private void handleValider() {
        if (questions == null || questions.isEmpty()) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("Erreur de session", "Veuillez vous reconnecter.");
            return;
        }

        int scoreBrut = calculerScore();
        float pourcentage = (scoreBrut * 100.0f) / questions.size();
        boolean estValide = pourcentage >= currentQcm.getScoreMinRequis();

        try {
            Progression p = new Progression();
            p.setParticipant(currentUser);
            p.setCours(currentCours);
            p.setScore(pourcentage);
            p.setValide(estValide);
            p.setCreatedAt(new Date());

            // --- LOGIQUE DE CONVERSION ROBUSTE DU NIVEAU ---
            int niveauInt = 1;
            String n = currentCours.getNiveau().toLowerCase();

            if (n.contains("inter")) {
                niveauInt = 2;
            } else if (n.contains("avan") || n.contains("expert") || n.contains("3")) {
                niveauInt = 3;
            } else {
                niveauInt = 1; // Débutant
            }

            p.setNiveau(niveauInt);
            System.out.println("DEBUG: Enregistrement Progression - Niveau: " + niveauInt + " | Cours: " + currentCours.getNom());

            // 1. Enregistrer la progression d'abord
            progressionService.ajouter(p);

            // 2. Tenter la génération du certificat
            if (estValide) {
                System.out.println("DEBUG: Appel CertificatService.handleProgression...");
                certificatService.handleProgression(p);
            }

            afficherResultat(scoreBrut, questions.size(), pourcentage, estValide);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Base de données inaccessible : " + e.getMessage());
        }
    }

    private void afficherResultat(int score, int total, float pourcentage, boolean valide) {
        String msg = "Score: " + score + "/" + total + " (" + String.format("%.1f", pourcentage) + "%)\n";
        msg += valide ? "✅ Niveau validé !" : "❌ Échec. Score requis: " + currentQcm.getScoreMinRequis() + "%";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();

        handleRetour();
    }

    private int calculerScore() {
        int score = 0;
        for (ToggleGroup group : toggleGroups) {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected != null) {
                QcmReponse rep = (QcmReponse) selected.getUserData();
                if (rep.isCorrecte()) score++;
            }
        }
        return score;
    }

    @FXML
    private void handleRetour() {
        if (contentHost != null && heroSection != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/Cours2View.fxml"));
                Parent coursView = loader.load();
                heroSection.setVisible(false);
                heroSection.setManaged(false);
                contentHost.setVisible(true);
                contentHost.setManaged(true);
                contentHost.getChildren().setAll(coursView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}