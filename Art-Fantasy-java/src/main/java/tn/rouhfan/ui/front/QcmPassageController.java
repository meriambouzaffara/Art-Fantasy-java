package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.rouhfan.entities.Cours;
import tn.rouhfan.entities.Qcm;
import tn.rouhfan.entities.QcmQuestion;
import tn.rouhfan.entities.QcmReponse;
import tn.rouhfan.services.QcmService;
import tn.rouhfan.services.QcmQuestionService;
import tn.rouhfan.services.QcmReponseService;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QcmPassageController {

    @FXML private Label lblTitreCours;
    @FXML private VBox questionsContainer;
    @FXML private Button btnValider;
    @FXML private Label lblInfo; // Ajouté pour les messages d'information

    private Cours currentCours;
    private Qcm currentQcm;
    private List<QcmQuestion> questions;
    private List<ToggleGroup> toggleGroups;
    private VBox contentHost;
    private VBox heroSection;

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
                System.out.println("QCM trouvé avec ID: " + currentQcm.getId());
                QcmQuestionService questionService = new QcmQuestionService();
                questions = questionService.recupererParQcm(currentQcm.getId());
                System.out.println("Nombre de questions trouvées: " + (questions != null ? questions.size() : 0));
                afficherQuestions();
            } else {
                System.err.println("Aucun QCM trouvé pour le cours ID: " + currentCours.getId());
                questionsContainer.getChildren().add(new Label("Aucun QCM disponible pour ce cours"));
                btnValider.setDisable(true);
                if (lblInfo != null) lblInfo.setText("Aucun QCM configuré pour ce cours");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            questionsContainer.getChildren().add(new Label("Erreur lors du chargement des questions: " + e.getMessage()));
            btnValider.setDisable(true);
            if (lblInfo != null) lblInfo.setText("Erreur: " + e.getMessage());
        }
    }

    private void afficherQuestions() {
        questionsContainer.getChildren().clear();
        toggleGroups.clear();

        if (questions == null || questions.isEmpty()) {
            questionsContainer.getChildren().add(new Label("Aucune question disponible pour ce QCM"));
            btnValider.setDisable(true);
            if (lblInfo != null) lblInfo.setText("Aucune question trouvée");
            return;
        }

        int totalReponses = 0;

        for (int i = 0; i < questions.size(); i++) {
            QcmQuestion q = questions.get(i);
            VBox questionBox = new VBox(10);
            questionBox.setStyle("-fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: white;");

            Label questionLabel = new Label((i+1) + ". " + q.getQuestion());
            questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            questionBox.getChildren().add(questionLabel);

            // Récupérer les réponses pour cette question
            try {
                QcmReponseService reponseService = new QcmReponseService();
                List<QcmReponse> reponses = reponseService.recupererParQuestion(q.getId());

                System.out.println("Question '" + q.getQuestion() + "' a " + (reponses != null ? reponses.size() : 0) + " réponses");
                totalReponses += (reponses != null ? reponses.size() : 0);

                if (reponses == null || reponses.isEmpty()) {
                    Label noReponse = new Label("  ⚠️ Aucune réponse disponible pour cette question");
                    noReponse.setStyle("-fx-text-fill: orange; -fx-padding: 5 0 5 20;");
                    questionBox.getChildren().add(noReponse);
                } else {
                    ToggleGroup group = new ToggleGroup();
                    toggleGroups.add(group);

                    for (QcmReponse reponse : reponses) {
                        RadioButton rb = new RadioButton(reponse.getTexte());
                        rb.setToggleGroup(group);
                        rb.setUserData(reponse);
                        rb.setStyle("-fx-padding: 5 0 5 20;");
                        questionBox.getChildren().add(rb);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Label errorLabel = new Label("  ❌ Erreur chargement des réponses: " + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 5 0 5 20;");
                questionBox.getChildren().add(errorLabel);
            }

            questionsContainer.getChildren().add(questionBox);
        }

        if (lblInfo != null) {
            lblInfo.setText(questions.size() + " question(s) - " + totalReponses + " réponse(s) disponible(s)");
        }
    }

    @FXML
    private void handleValider() {
        if (questions == null || questions.isEmpty()) {
            showError("Erreur", "Aucune question à valider");
            return;
        }

        int score = calculerScore();
        int total = questions.size();

        float pourcentage = (score * 100.0f) / total;
        boolean valide = pourcentage >= currentQcm.getScoreMinRequis();

        String message = "Votre score: " + score + "/" + total + " (" + String.format("%.1f", pourcentage) + "%)\n";
        message += "Score minimum requis: " + currentQcm.getScoreMinRequis() + "%\n\n";

        if (valide) {
            message += "✅ Félicitations ! QCM validé !";
        } else {
            message += "❌ Désolé, QCM non validé.";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat du QCM");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        handleRetour();
    }

    private int calculerScore() {
        int score = 0;

        for (int i = 0; i < questions.size() && i < toggleGroups.size(); i++) {
            ToggleGroup group = toggleGroups.get(i);
            RadioButton selected = (RadioButton) group.getSelectedToggle();

            if (selected != null && selected.getUserData() instanceof QcmReponse) {
                QcmReponse reponse = (QcmReponse) selected.getUserData();
                if (reponse.isCorrecte()) {
                    score++;
                }
            }
        }

        System.out.println("Score calculé: " + score + "/" + questions.size());
        return score;
    }

    @FXML
    private void handleRetour() {
        if (contentHost != null && heroSection != null) {
            try {
                URL fxmlLocation = getClass().getResource("/ui/front/Cours2View.fxml");
                if (fxmlLocation != null) {
                    FXMLLoader loader = new FXMLLoader(fxmlLocation);
                    Parent coursView = loader.load();

                    heroSection.setVisible(false);
                    heroSection.setManaged(false);
                    contentHost.setVisible(true);
                    contentHost.setManaged(true);
                    contentHost.getChildren().clear();
                    contentHost.getChildren().add(coursView);
                }
            } catch (IOException e) {
                e.printStackTrace();
                showError("Erreur", "Impossible de revenir à la liste des cours");
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}