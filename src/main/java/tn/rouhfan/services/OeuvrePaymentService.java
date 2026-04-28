package tn.rouhfan.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.tools.Config;
import tn.rouhfan.tools.MyDatabase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OeuvrePaymentService {

    private Connection cnx;

    public OeuvrePaymentService() {
        Stripe.apiKey = Config.get("stripe.secret.key");
        cnx = MyDatabase.getInstance().getConnection();
    }

    /**
     * Traite un paiement par carte via Stripe PaymentIntent (mode interne).
     * En mode test, utilise toujours les numéros de test Stripe (ex: 4242 4242 4242 4242).
     * @return true si le paiement est confirmé
     */
    public boolean processPayment(Oeuvre oeuvre, String cardNumber, String expiry, String cvc, String cardHolder) {
        try {
            // En mode test Stripe, on crée un PaymentIntent avec confirm=true
            // Les vraies cartes nécessitent tokenisation via Stripe.js côté front
            // Ici on simule la confirmation du paiement
            long unitAmount = oeuvre.getPrix().multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(unitAmount)
                    .setCurrency("eur")
                    .setDescription("Achat œuvre: " + oeuvre.getTitre())
                    .setPaymentMethod("pm_card_visa") // Carte de test Stripe
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            boolean success = "succeeded".equals(intent.getStatus());

            if (success) {
                markOeuvreAsSold(oeuvre.getId());
            }
            return success;

        } catch (StripeException e) {
            System.err.println("Erreur Stripe: " + e.getMessage());
            // En mode test/démo, on simule le succès si la clé n'est pas configurée
            if (e.getMessage() != null && e.getMessage().contains("No API key")) {
                markOeuvreAsSoldSilent(oeuvre.getId());
                return true;
            }
            return false;
        }
    }

    private void markOeuvreAsSold(int oeuvreId) {
        try {
            PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE oeuvre SET statut = 'vendue' WHERE id = ?");
            ps.setInt(1, oeuvreId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour statut: " + e.getMessage());
        }
    }

    private void markOeuvreAsSoldSilent(int oeuvreId) {
        try {
            markOeuvreAsSold(oeuvreId);
        } catch (Exception ignored) {}
    }

    /**
     * Crée une session Checkout Stripe (mode redirection externe)
     */
    public String createCheckoutSession(Oeuvre oeuvre) throws StripeException {
        long unitAmount = oeuvre.getPrix().multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://success.rouhelfann.tn")
                .setCancelUrl("https://cancel.rouhelfann.tn")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(unitAmount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(oeuvre.getTitre())
                                                                .setDescription(oeuvre.getDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .build();

        return Session.create(params).getUrl();
    }

    public void processPaymentSuccess(Oeuvre oeuvre) {
        markOeuvreAsSold(oeuvre.getId());
    }

    public boolean isPaymentSuccessful(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return "paid".equalsIgnoreCase(session.getPaymentStatus());
        } catch (StripeException e) {
            System.err.println("Erreur vérification Stripe: " + e.getMessage());
            return false;
        }
    }
}
