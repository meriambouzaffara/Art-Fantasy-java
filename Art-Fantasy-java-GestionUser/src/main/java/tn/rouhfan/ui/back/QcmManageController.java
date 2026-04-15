package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.rouhfan.entities.*;
import tn.rouhfan.services.*;
import java.sql.SQLException;

public class QcmManageController {

    @FXML private Label lblTitreCours;
    @FXML private TableView<QcmQuestion> questionTable;
    @FXML private TableColumn<QcmQuestion, String> colQuestion;
    @FXML private TableColumn<QcmQuestion, Void> colAction;
    @FXML private TextArea taQuestion;
    @FXML private TextField tfR1, tfR2, tfR3, tfR4;
    @FXML private CheckBox checkR1, checkR2, checkR3, checkR4;

    private Qcm currentQcm;
    private final QcmService qcmService = new QcmService();
    private final QcmQuestionService questionService = new QcmQuestionService();
    private final ObservableList<QcmQuestion> questionList = FXCollections.observableArrayList();

    public void setCours(Cours cours) {
        lblTitreCours.setText("Gestion QCM : " + cours.getNom());
        try {
            this.currentQcm = qcmService.getOrCreateQcm(cours.getId());
            setupTable();
            refreshTable();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupTable() {
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Supprimer");
            {
                btnDelete.setStyle("-fx-background-color: #f38ba8; -fx-text-fill: white;");
                btnDelete.setOnAction(event -> handleDeleteQuestion(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
        questionTable.setItems(questionList);
    }

    @FXML
    private void handleSaveQuestion() {
        if (taQuestion.getText().trim().isEmpty()) return;

        QcmQuestion q = new QcmQuestion();
        q.setQcm(currentQcm);
        q.setQuestion(taQuestion.getText().trim());

        ajouterReponse(q, tfR1.getText(), checkR1.isSelected());
        ajouterReponse(q, tfR2.getText(), checkR2.isSelected());
        ajouterReponse(q, tfR3.getText(), checkR3.isSelected());
        ajouterReponse(q, tfR4.getText(), checkR4.isSelected());

        try {
            questionService.ajouter(q);
            clearForm();
            refreshTable();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void ajouterReponse(QcmQuestion q, String texte, boolean isCorrect) {
        if (texte != null && !texte.trim().isEmpty()) {
            QcmReponse r = new QcmReponse();
            r.setTexte(texte);
            r.setCorrecte(isCorrect);
            r.setQuestion(q);
            q.getReponses().add(r);
        }
    }

    private void handleDeleteQuestion(QcmQuestion q) {
        try {
            questionService.supprimer(q.getId());
            refreshTable();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshTable() throws SQLException {
        questionList.setAll(questionService.recupererParQcm(currentQcm.getId()));
    }

    private void clearForm() {
        taQuestion.clear();
        tfR1.clear(); tfR2.clear(); tfR3.clear(); tfR4.clear();
        checkR1.setSelected(false); checkR2.setSelected(false);
        checkR3.setSelected(false); checkR4.setSelected(false);
    }
}