package tn.rouhfan.services;

import io.github.cdimascio.dotenv.Dotenv;
import tn.rouhfan.tools.Config;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

public class TicketEmailService {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    public void sendTicket(String recipientEmail, String eventName, File ticketPdf) throws MessagingException {
        String gmailUser = readConfig("GMAIL_USER", "gmail.user", "motaz.sammoud11@gmail.com");
        String gmailPassword = readConfig("GMAIL_APP_PASSWORD", "gmail.app.password", "");

        if (isBlank(gmailUser) || isBlank(gmailPassword)) {
            throw new MessagingException("Configuration Gmail manquante: ajoutez GMAIL_USER et GMAIL_APP_PASSWORD dans .env ou config.properties.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(gmailUser, gmailPassword);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gmailUser));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Votre ticket pour l'événement : " + eventName, "UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(buildHtmlBody(eventName));
        multipart.addBodyPart(buildAttachment(ticketPdf, "Ticket-" + eventName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf"));

        message.setContent(multipart);
        Transport.send(message);
    }

    private MimeBodyPart buildHtmlBody(String eventName) throws MessagingException {
        MimeBodyPart body = new MimeBodyPart();
        String html = "<div style=\"font-family:Segoe UI,Arial,sans-serif;color:#2d1b4e\">"
                + "<h2 style=\"color:#241197\">Confirmation de votre participation</h2>"
                + "<p>Bonjour,</p>"
                + "<p>Nous vous confirmons votre participation à l'événement <b>" + escapeHtml(eventName) + "</b>.</p>"
                + "<p>Veuillez trouver en pièce jointe votre ticket PDF.</p>"
                + "<p>Merci et à très bientôt,<br/>L'équipe Rouh el Fann</p>"
                + "</div>";
        body.setContent(html, "text/html; charset=UTF-8");
        return body;
    }

    private MimeBodyPart buildAttachment(File file, String fileName) throws MessagingException {
        MimeBodyPart attachment = new MimeBodyPart();
        FileDataSource dataSource = new FileDataSource(file);
        attachment.setDataHandler(new DataHandler(dataSource));
        attachment.setFileName(fileName);
        return attachment;
    }

    private String readConfig(String envName, String propertyName, String defaultValue) {
        String value = System.getenv(envName);
        if (isBlank(value)) {
            value = DOTENV.get(envName);
        }
        if (isBlank(value)) {
            value = Config.get(propertyName, "");
        }
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
