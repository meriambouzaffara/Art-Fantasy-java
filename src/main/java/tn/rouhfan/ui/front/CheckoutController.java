package tn.rouhfan.ui.front;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import tn.rouhfan.entities.Article;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CheckoutReceiptService;
import tn.rouhfan.services.GmailInvoiceService;
import tn.rouhfan.services.HistoriqueAchatService;
import tn.rouhfan.services.PanierItem;
import tn.rouhfan.services.PanierService;
import tn.rouhfan.ui.Router;
import tn.rouhfan.tools.SessionManager;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class CheckoutController implements Initializable {

    private static final String STRIPE_PAYMENT_URL = "https://buy.stripe.com/test_7sYfZa4rC1Am3xf7mB1wY02";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final DateTimeFormatter ORDER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @FXML private VBox cartItemsBox;
    @FXML private Label emptyCartLabel;
    @FXML private Label totalLabel;
    @FXML private Label cartCountLabel;
    @FXML private TextField emailField;
    @FXML private WebView stripeWebView;
    @FXML private Button payButton;
    @FXML private Button confirmButton;
    @FXML private Label statusLabel;
    @FXML private ImageView qrPreview;
    @FXML private Label qrStatusLabel;

    private final PanierService         panierService         = PanierService.getInstance();
    private final CheckoutReceiptService receiptService        = new CheckoutReceiptService();
    private final GmailInvoiceService    gmailInvoiceService   = new GmailInvoiceService();
    private final HistoriqueAchatService historiqueAchatService = new HistoriqueAchatService();
    private boolean stripeStarted = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renderCart();
        panierService.revisionProperty().addListener((obs, oldValue, newValue) -> renderCart());
        stripeWebView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> detectStripeConfirmation(newUrl));
        stripeWebView.getEngine().loadContent("<html><body style='font-family:Segoe UI,Arial;padding:24px;color:#241197'>"
                + "<h2>Stripe Checkout</h2><p>Le paiement securise s'affichera ici apres clic sur le bouton.</p></body></html>");
    }

    @FXML
    private void goToHistorique() {
        VBox contentHost = (VBox) cartItemsBox.getScene().lookup("#contentHost");
        if (contentHost != null) {
            Router.setContent(contentHost, "/ui/front/front_historique_achats.fxml");
        }
    }

    @FXML
    private void clearCart() {
        panierService.clear();
        statusLabel.setText("Panier vide.");
        confirmButton.setDisable(true);
    }

    @FXML
    private void startStripePayment() {
        if (panierService.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Ajoutez au moins un article avant le paiement.");
            return;
        }
        String email = emailField.getText().trim();
        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Saisissez un email valide pour recevoir la facture.");
            return;
        }

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String checkoutUrl = STRIPE_PAYMENT_URL + "?prefilled_email=" + encodedEmail;
        stripeStarted = true;
        confirmButton.setDisable(false);
        statusLabel.setText("Stripe est charge. Finalisez le paiement dans la WebView, puis confirmez l'achat.");
        stripeWebView.getEngine().load(checkoutUrl);
    }

    @FXML
    private void confirmPayment() {
        if (!stripeStarted) {
            showAlert(Alert.AlertType.WARNING, "Paiement non lance", "Chargez d'abord Stripe et finalisez le paiement.");
            return;
        }
        if (panierService.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Panier vide", "Le panier est vide.");
            return;
        }
        String email = emailField.getText().trim();
        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Saisissez un email valide pour recevoir la facture.");
            return;
        }

        List<PanierItem> items = panierService.snapshot();
        double total = panierService.getTotal();
        String orderReference = "RF-" + LocalDateTime.now().format(ORDER_FORMAT);

        Task<CheckoutReceiptService.ReceiptFiles> sendTask = new Task<>() {
            @Override
            protected CheckoutReceiptService.ReceiptFiles call() throws Exception {
                CheckoutReceiptService.ReceiptFiles files = receiptService.createReceipt(orderReference, email, items, total);
                gmailInvoiceService.sendInvoice(email, orderReference, files.getInvoicePdf(), files.getQrCode(), total);
                return files;
            }
        };

        setBusy(true, "Generation de la facture et envoi du mail...");
        sendTask.setOnSucceeded(event -> {
            setBusy(false, "Paiement confirme. Facture envoyee a " + email + ".");
            CheckoutReceiptService.ReceiptFiles files = sendTask.getValue();
            showQrPreview(files.getQrCode());

            // ── Enregistrement dans l'historique des achats ──────
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                Long userId = Long.valueOf((currentUser != null) ? currentUser.getId() : null);
                historiqueAchatService.enregistrerAchat(
                        orderReference, email, items, total, userId);
            } catch (Exception ex) {
                System.err.println("[Checkout] Avertissement : impossible d'enregistrer l'historique : "
                        + ex.getMessage());
                // On ne bloque pas l'utilisateur si l'historique échoue
            }
            // ────────────────────────────────────────────────────

            panierService.clear();
            confirmButton.setDisable(true);
            stripeStarted = false;
        });
        sendTask.setOnFailed(event -> {
            Throwable ex = sendTask.getException();
            setBusy(false, "Paiement a confirmer, mais l'envoi mail a echoue: " + (ex != null ? ex.getMessage() : ""));
            showAlert(Alert.AlertType.ERROR, "Erreur mail", ex != null ? ex.getMessage() : "Erreur inconnue");
        });

        Thread thread = new Thread(sendTask, "checkout-mail-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        List<PanierItem> items = panierService.snapshot();
        boolean empty = items.isEmpty();
        emptyCartLabel.setVisible(empty);
        emptyCartLabel.setManaged(empty);
        cartCountLabel.setText(panierService.getItemCount() + " article" + (panierService.getItemCount() > 1 ? "s" : ""));
        totalLabel.setText(String.format("Total: %.2f DT", panierService.getTotal()));

        for (PanierItem item : items) {
            cartItemsBox.getChildren().add(buildCartRow(item));
        }
    }

    private HBox buildCartRow(PanierItem item) {
        Article article = item.getArticle();
        Label title = new Label(article != null && article.getTitre() != null ? article.getTitre() : "Article");
        title.setWrapText(true);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #241197;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label quantity = new Label("x" + item.getQuantity());
        quantity.setStyle("-fx-text-fill: #5a4a72; -fx-font-weight: bold;");

        Label subtotal = new Label(String.format("%.2f DT", item.getSubtotal()));
        subtotal.setStyle("-fx-text-fill: #c9a849; -fx-font-weight: bold;");

        Button minus = compactButton("-");
        minus.setOnAction(e -> panierService.decrement(item));
        Button plus = compactButton("+");
        plus.setOnAction(e -> panierService.increment(item));
        Button remove = compactButton("X");
        remove.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #d63031; -fx-font-weight: bold; -fx-background-radius: 8;");
        remove.setOnAction(e -> panierService.remove(item));

        HBox row = new HBox(8, title, minus, quantity, plus, subtotal, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10; -fx-background-color: #faf9fc; -fx-background-radius: 10;");
        return row;
    }

    private Button compactButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(30);
        button.setStyle("-fx-background-color: #f0eef5; -fx-text-fill: #241197; -fx-font-weight: bold; -fx-background-radius: 8;");
        return button;
    }

    private void detectStripeConfirmation(String url) {
        if (url == null) {
            return;
        }
        String normalized = url.toLowerCase();
        if (normalized.contains("success")
                || normalized.contains("paid")
                || normalized.contains("complete")
                || normalized.contains("redirect_status=succeeded")) {
            statusLabel.setText("Stripe indique une confirmation. Vous pouvez envoyer la facture.");
            confirmButton.setDisable(false);
        }
    }

    private void setBusy(boolean busy, String message) {
        payButton.setDisable(busy);
        confirmButton.setDisable(busy || !stripeStarted);
        statusLabel.setText(message);
    }

    private void showQrPreview(File qrFile) {
        qrPreview.setImage(new Image(qrFile.toURI().toString()));
        qrPreview.setVisible(true);
        qrPreview.setManaged(true);
        qrStatusLabel.setText("QR code genere et joint au mail. Son scan affiche les informations privees du paiement.");
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
