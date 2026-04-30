package tn.rouhfan.services;

import io.github.cdimascio.dotenv.Dotenv;
import tn.rouhfan.tools.AppLogger;
import tn.rouhfan.tools.Config;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service d'envoi d'emails pour le module utilisateur.
 * Utilise Gmail SMTP (même configuration que GmailInvoiceService).
 *
 * Fonctionnalités :
 * - Envoi de mail de vérification de compte
 * - Envoi de mail de réinitialisation de mot de passe
 * - Envoi de mails génériques
 */
public class UserEmailService {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    /**
     * Envoie un email de vérification de compte.
     */
    public boolean sendVerificationEmail(String recipientEmail, String userName, String token) {
        String subject = "🔐 Vérifiez votre compte Rouh el Fann";
        String html = "<div style=\"font-family:'Segoe UI',Arial,sans-serif; max-width:500px; margin:0 auto; "
                + "background:#faf9fc; border-radius:16px; padding:40px; border:1px solid #e8e4f0;\">"
                + "<h2 style=\"color:#241197; text-align:center;\">✨ Bienvenue sur Rouh el Fann</h2>"
                + "<p style=\"color:#2d1b4e; font-size:15px;\">Bonjour <b>" + escapeHtml(userName) + "</b>,</p>"
                + "<p style=\"color:#5a4a72;\">Merci de vous être inscrit ! Pour activer votre compte, "
                + "utilisez le code de vérification ci-dessous :</p>"
                + "<div style=\"background:#241197; color:white; font-size:28px; font-weight:bold; "
                + "text-align:center; padding:20px; border-radius:12px; letter-spacing:6px; margin:20px 0;\">"
                + escapeHtml(token)
                + "</div>"
                + "<p style=\"color:#5a4a72; font-size:13px;\">Ce code expire dans <b>24 heures</b>.</p>"
                + "<p style=\"color:#5a4a72; font-size:12px;\">Si vous n'avez pas créé de compte, ignorez ce mail.</p>"
                + "<hr style=\"border:none; border-top:1px solid #e8e4f0; margin:20px 0;\">"
                + "<p style=\"color:#9b8fb5; font-size:11px; text-align:center;\">© Rouh el Fann — Plateforme Artistique</p>"
                + "</div>";

        return sendHtmlEmail(recipientEmail, subject, html);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe.
     */
    public boolean sendPasswordResetEmail(String recipientEmail, String userName, String token) {
        String subject = "🔑 Réinitialisation de votre mot de passe — Rouh el Fann";
        String html = "<div style=\"font-family:'Segoe UI',Arial,sans-serif; max-width:500px; margin:0 auto; "
                + "background:#faf9fc; border-radius:16px; padding:40px; border:1px solid #e8e4f0;\">"
                + "<h2 style=\"color:#241197; text-align:center;\">🔑 Réinitialisation du mot de passe</h2>"
                + "<p style=\"color:#2d1b4e; font-size:15px;\">Bonjour <b>" + escapeHtml(userName) + "</b>,</p>"
                + "<p style=\"color:#5a4a72;\">Vous avez demandé la réinitialisation de votre mot de passe. "
                + "Utilisez le code ci-dessous :</p>"
                + "<div style=\"background:linear-gradient(to right, #241197, #6c2a90); color:white; "
                + "font-size:28px; font-weight:bold; text-align:center; padding:20px; border-radius:12px; "
                + "letter-spacing:6px; margin:20px 0;\">"
                + escapeHtml(token)
                + "</div>"
                + "<p style=\"color:#5a4a72; font-size:13px;\">Ce code expire dans <b>30 minutes</b>.</p>"
                + "<p style=\"color:#d63031; font-size:13px;\">⚠️ Si vous n'avez pas fait cette demande, "
                + "changez immédiatement votre mot de passe.</p>"
                + "<hr style=\"border:none; border-top:1px solid #e8e4f0; margin:20px 0;\">"
                + "<p style=\"color:#9b8fb5; font-size:11px; text-align:center;\">© Rouh el Fann — Plateforme Artistique</p>"
                + "</div>";

        return sendHtmlEmail(recipientEmail, subject, html);
    }

    /**
     * Envoie un email HTML générique.
     */
    public boolean sendHtmlEmail(String recipientEmail, String subject, String htmlContent) {
        String gmailUser = readConfig("GMAIL_USER", "gmail.user", "");
        String gmailPassword = readConfig("GMAIL_APP_PASSWORD", "gmail.app.password", "");

        if (isBlank(gmailUser) || isBlank(gmailPassword)) {
            AppLogger.warn("[UserEmailService] Configuration Gmail manquante — email simulé pour: " + recipientEmail);
            // Mode simulation : afficher dans la console
            System.out.println("══════════════════════════════════════════");
            System.out.println("📧 EMAIL SIMULÉ");
            System.out.println("To: " + recipientEmail);
            System.out.println("Subject: " + subject);
            System.out.println("══════════════════════════════════════════");
            return true; // Simulation réussie
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            final String user = gmailUser;
            final String pass = gmailPassword;

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(gmailUser));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            AppLogger.info("[UserEmailService] Email envoyé à: " + recipientEmail + " | Sujet: " + subject);
            return true;

        } catch (MessagingException e) {
            AppLogger.error("[UserEmailService] Échec envoi email à " + recipientEmail, e);
            return false;
        }
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
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
