package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.OeuvrePaymentService;

public class OeuvreStripePaymentDialogController {

    @FXML private WebView webView;
    @FXML private VBox loadingOverlay;
    @FXML private VBox successContainer;

    private Oeuvre oeuvre;
    private boolean isSuccess = false;
    private OeuvrePaymentService paymentService = new OeuvrePaymentService();

    public void setOeuvre(Oeuvre oeuvre) {
        this.oeuvre = oeuvre;
        if (oeuvre != null) {
            startPayment();
        }
    }

    private void startPayment() {
        new Thread(() -> {
            try {
                String checkoutUrl = paymentService.createCheckoutSession(oeuvre);
                Platform.runLater(() -> loadCheckout(checkoutUrl));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // Show error in loading overlay
                    loadingOverlay.getChildren().clear();
                    Label err = new Label("Erreur lors de l'initialisation: " + e.getMessage());
                    err.setStyle("-fx-text-fill: #dc2626;");
                    loadingOverlay.getChildren().add(err);
                });
            }
        }).start();
    }

    private void loadCheckout(String url) {
        WebEngine engine = webView.getEngine();
        
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                    loadingOverlay.setVisible(false);
                    loadingOverlay.setManaged(false);
            }
        });

        // Détection de la redirection vers Success ou Cancel
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            System.out.println("Navigating to: " + newLoc);
            if (newLoc.startsWith("https://success.rouhelfann.tn")) {
                handlePaymentSuccess();
            } else if (newLoc.startsWith("https://cancel.rouhelfann.tn")) {
                handleClose();
                }
            });

        engine.load(url);
    }

    private void handlePaymentSuccess() {
        this.isSuccess = true;
        webView.setVisible(false);
        webView.setManaged(false);
        successContainer.setVisible(true);
        successContainer.setManaged(true);
        
        // Mettre à jour la base de données
        new Thread(() -> {
            paymentService.processPaymentSuccess(oeuvre);
        }).start();
    }

    @FXML
    private void handleClose() {
        ((Stage) webView.getScene().getWindow()).close();
    }

    @FXML
    private void handleCloseSuccess() {
        ((Stage) successContainer.getScene().getWindow()).close();
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
