package tn.rouhfan.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import tn.rouhfan.entities.Oeuvre;

import java.math.BigDecimal;

public class OeuvrePaymentService {

    private static final String STRIPE_SECRET_KEY = "YOUR_STRIPE_SECRET_KEY"; // TODO: Use environment variables for security

    public OeuvrePaymentService() {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    /**
     * Crée une session Checkout Stripe pour l'achat d'une œuvre.
     * @param oeuvre L'œuvre à acheter
     * @return L'URL de la session Checkout
     * @throws StripeException si une erreur survient lors de la communication avec Stripe
     */
    public String createCheckoutSession(Oeuvre oeuvre) throws StripeException {
        // Stripe utilise des montants en centimes (ex: 10.00 DT -> 1000)
        long unitAmount = oeuvre.getPrix().multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://success.rouhelfann.tn") // URL fictive pour le succès
                .setCancelUrl("https://cancel.rouhelfann.tn")   // URL fictive pour l'annulation
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur") // Utiliser EUR ou USD pour les tests
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

        Session session = Session.create(params);
        return session.getUrl();
    }
}
