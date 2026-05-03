package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.OeuvreIA;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.OeuvreIAService;
import tn.rouhfan.tools.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class IAOeuvresFrontController implements Initializable {

    @FXML private FlowPane cardsPane;
    @FXML private VBox emptyState;

    private OeuvreIAService service = new OeuvreIAService();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadData();
    }

    private void loadData() {
        if (currentUser == null) return;
        cardsPane.getChildren().clear();
        try {
            List<OeuvreIA> list = service.recupererParUser(currentUser.getId());
            if (list.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                for (OeuvreIA o : list) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/OeuvreIACard.fxml"));
                    Parent card = loader.load();
                    OeuvreIACardController ctrl = loader.getController();
                    ctrl.setOeuvre(o, this::loadData);
                    cardsPane.getChildren().add(card);
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateWithIA() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/IAFormDialog.fxml"));
            Parent root = loader.load();
            IAFormDialogController ctrl = loader.getController();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Création d'œuvre par IA");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (ctrl.isGenerated()) {
                loadData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleCreateWithMusic() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/front/MusicArtWizardDialog.fxml"));
            Parent root = loader.load();
            MusicArtWizardController ctrl = loader.getController();
            ctrl.setRefreshCallback(this::loadData);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("🎵 Musique vers Art");
            
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
