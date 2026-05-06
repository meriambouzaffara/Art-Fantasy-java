package tn.rouhfan.tools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Système de logs centralisé pour l'application Rouh el Fann.
 * Utilise java.util.logging avec sortie console + fichier.
 *
 * Usage :
 *   AppLogger.info("Message");
 *   AppLogger.warn("Attention...");
 *   AppLogger.error("Erreur critique", exception);
 *   AppLogger.auth("LOGIN_SUCCESS", "user@mail.com");
 */
public class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("RouhElFann");
    private static final String LOG_FILE = "app.log";
    private static boolean initialized = false;

    static {
        initLogger();
    }

    /**
     * Initialise le logger avec un FileHandler et un ConsoleHandler personnalisés.
     */
    private static synchronized void initLogger() {
        if (initialized) return;
        try {
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);

            // Console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new AppLogFormatter());
            LOGGER.addHandler(consoleHandler);

            // File handler (append mode)
            try {
                FileHandler fileHandler = new FileHandler(LOG_FILE, 5 * 1024 * 1024, 3, true);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new AppLogFormatter());
                LOGGER.addHandler(fileHandler);
            } catch (IOException e) {
                System.err.println("[AppLogger] Impossible de créer le fichier de log: " + e.getMessage());
            }

            initialized = true;
            LOGGER.info("=== Application Rouh el Fann démarrée ===");
        } catch (Exception e) {
            System.err.println("[AppLogger] Erreur d'initialisation: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    //  Méthodes de log principales
    // ═══════════════════════════════════════

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        LOGGER.fine(message);
    }

    // ═══════════════════════════════════════
    //  Logs spécialisés Authentification
    // ═══════════════════════════════════════

    /**
     * Log une action d'authentification.
     * @param action ex: LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, SIGNUP, PASSWORD_RESET
     * @param details ex: email de l'utilisateur
     */
    public static void auth(String action, String details) {
        LOGGER.info("[AUTH] " + action + " | " + details);
    }

    /**
     * Log une action utilisateur (modification profil, etc.)
     */
    public static void userAction(String action, int userId, String details) {
        LOGGER.info("[USER:" + userId + "] " + action + " | " + details);
    }

    /**
     * Log une erreur de sécurité.
     */
    public static void security(String message) {
        LOGGER.warning("[SECURITY] " + message);
    }

    /**
     * Log une action liée à l'IA (Hugging Face API).
     * ⚠️ Ne jamais inclure de clé API ou de token dans le message.
     */
    public static void ai(String action, String details) {
        LOGGER.info("[AI] " + action + " | " + details);
    }

    // ═══════════════════════════════════════
    //  Formatter personnalisé
    // ═══════════════════════════════════════

    /**
     * Formateur de log personnalisé avec timestamp lisible.
     */
    private static class AppLogFormatter extends Formatter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(sdf.format(new Date(record.getMillis())));
            sb.append(" [").append(record.getLevel().getName()).append("] ");
            sb.append(record.getMessage());
            if (record.getThrown() != null) {
                sb.append(" | Exception: ").append(record.getThrown().getMessage());
            }
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }
}
