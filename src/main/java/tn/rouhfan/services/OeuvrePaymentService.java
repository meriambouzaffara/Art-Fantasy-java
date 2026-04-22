package tn.rouhfan.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.tools.Config;

import java.math.BigDecimal;

public class OeuvrePaymentService {

    public OeuvrePaymentService() {
        // Charge la clé depuis config.properties via notre utilitaire Config
        Stripe.apiKey = Config.get("stripe.secret.key");
    }

    /**
     * Crée une session Checkout Stripe pour l'achat d'une œuvre.
     * @param oeuvre L'œuvre à acheter
     * @return L'URL de la session Checkout
     * @throws StripeException si une erreur survient lors de la communication avec Stripe
     */
    public String createCheckoutSession(Oeuvre oeuvre) throws StripeException {
        Session session = createSession(oeuvre);
        return session.getUrl();
    }

    /**
     * Crée une session Checkout Stripe et retourne l'objet complet
     */
    public Session createSession(Oeuvre oeuvre) throws StripeException {
        // Stripe utilise des montants en centimes (ex: 10.00 DT -> 1000)
        long unitAmount = oeuvre.getPrix().multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://success.rouhelfann.tn") // URL fictive
                .setCancelUrl("https://cancel.rouhelfann.tn")   // URL fictive
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

        return Session.create(params);
    }

    /**
     * Vérifie si une session de paiement a été finalisée avec succès
     * @param sessionId L'identifiant de la session Stripe
     * @return true si le paiement est confirmé
     */
    public boolean isPaymentSuccessful(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return "paid".equalsIgnoreCase(session.getPaymentStatus());
        } catch (StripeException e) {
            System.err.println("❌ Erreur de vérification Stripe: " + e.getMessage());
            return false;
        }
    }
}
