package tn.rouhfan.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Starting application...");
            java.net.URL fxmlLocation = getClass().getResource("/ui/front/FrontBase.fxml");
            if (fxmlLocation == null) {
                throw new RuntimeException("CRITICAL ERROR: FrontBase.fxml not found at /ui/front/FrontBase.fxml");
            }
            System.out.println("Loading FXML from: " + fxmlLocation);
            
            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);
            stage.setTitle("Rouh el Fann");
            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(680);
            stage.show();
            System.out.println("Application started successfully!");
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("CRITICAL ERROR DURING APPLICATION START");
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
