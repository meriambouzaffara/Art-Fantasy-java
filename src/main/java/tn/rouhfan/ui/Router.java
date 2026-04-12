package tn.rouhfan.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.io.IOException;

public final class Router {

    private Router() {
    }

    public static void setContent(Pane host, String fxmlPath) {
        try {
            java.net.URL url = Router.class.getResource(fxmlPath);
            if (url == null) {
                System.err.println("Router: FXML not found: " + fxmlPath);
                return;
            }
            Node view = FXMLLoader.load(url);
            host.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Router: Failed to load view " + fxmlPath);
            e.printStackTrace();
        }
    }
}
