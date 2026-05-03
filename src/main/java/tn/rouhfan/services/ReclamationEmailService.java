package tn.rouhfan.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class ReclamationEmailService {

    public static void sendEmail(String to, String subject, String messageContent) {

        final String from = "maissanfissi@gmail.com"; // TON EMAIL
        final String password = "qrqw pxew gofy snte"; // ⚠️ PAS ton mdp normal

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject(subject);
            message.setText(messageContent);

            Transport.send(message);

            System.out.println("✅ Email envoyé !");
        } catch (MessagingException e) {
            System.out.println("❌ Erreur email: " + e.getMessage());
        }
    }
}