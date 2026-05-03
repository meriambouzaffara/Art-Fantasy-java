package tn.rouhfan.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.cdimascio.dotenv.Dotenv;

public class TwilioSmsService {
    
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    // Identifiants Twilio
    public static final String ACCOUNT_SID = dotenv.get("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = dotenv.get("TWILIO_AUTH_TOKEN");
    public static final String FROM_NUMBER = dotenv.get("TWILIO_FROM_NUMBER");

    static {
        // Initialiser Twilio lors du chargement de la classe
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    /**
     * Envoie un SMS à un numéro spécifique.
     * @param toPhoneNumber Le numéro de téléphone de destination (doit inclure le code pays, ex: +216...).
     * @param body Le contenu du message.
     */
    public void sendSms(String toPhoneNumber, String body) {
        if (toPhoneNumber == null || toPhoneNumber.trim().isEmpty()) {
            System.err.println("Numéro de téléphone invalide pour l'envoi du SMS.");
            return;
        }
        
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(FROM_NUMBER),
                    body
            ).create();
            
            System.out.println("SMS envoyé avec succès ! SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
