package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private PieChart categoryPieChart;
    @FXML private Label totalLabel;
    @FXML private ListView<String> historyListView;

    private ReclamationService rs = new ReclamationService();
    private ReponseReclamationService reponseService = new ReponseReclamationService();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDashboardData();
    }

    private void loadDashboardData() {
        try {
            List<Reclamation> reclamations = rs.recuperer();
            List<ReponseReclamation> allReponses = reponseService.recuperer();

            Map<String, Integer> categoryCount = new HashMap<>();
            int totalList = 0;
            ObservableList<String> historyItems = FXCollections.observableArrayList();

            for (Reclamation r : reclamations) {
                totalList++;
                String cat = r.getCategorie() != null ? r.getCategorie() : "Inconnue";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);

                StringBuilder sb = new StringBuilder();
                sb.append("📅 ").append(sdf.format(r.getCreatedAt()))
                        .append(" | [").append(cat.toUpperCase()).append("] ")
                        .append("Utilisateur ID: ").append(r.getAuteurId())
                        .append(" - Sujet: '").append(r.getSujet()).append("' (").append(r.getStatut()).append(")");

                boolean hasResponse = false;
                for (ReponseReclamation rep : allReponses) {
                    if (rep.getReclamationId() == r.getId()) {
                        sb.append("\n  ↳ 💬 Réponse : ").append(rep.getMessage());
                        hasResponse = true;
                    }
                }
                if (!hasResponse) {
                    sb.append("\n  ↳ En attente de traitement.");
                }

                historyItems.add(sb.toString());
            }

            totalLabel.setText("Total des réclamations/avis : " + totalList);
            historyListView.setItems(historyItems);

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            categoryPieChart.setData(pieChartData);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
