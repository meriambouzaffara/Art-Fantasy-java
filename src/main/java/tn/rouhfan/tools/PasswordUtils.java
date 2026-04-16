package tn.rouhfan.tools;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire pour le hachage et la vérification des mots de passe.
 * Compatible avec les hash Symfony ($2y$13$...) et Java ($2a$...).
 */
public class PasswordUtils {

    /**
     * Hache un mot de passe en clair avec BCrypt.
     * Génère un hash compatible avec Symfony ($2y$13$...).
     */
    public static String hashPassword(String plainPassword) {
        // BCrypt.gensalt(13) pour correspondre au cost factor de Symfony ($2y$13$)
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(13));
        // Remplacer $2a$ par $2y$ pour compatibilité Symfony
        return hash.replace("$2a$", "$2y$");
    }

    /**
     * Vérifie un mot de passe en clair contre un hash BCrypt.
     * Supporte les hash Symfony ($2y$) et Java ($2a$).
     * Supporte aussi les mots de passe stockés en clair (ancien code).
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        // Si le hash commence par $2y$ ou $2a$ → c'est un hash BCrypt
        if (hashedPassword.startsWith("$2y$") || hashedPassword.startsWith("$2a$")) {
            try {
                // BCrypt Java utilise $2a$, Symfony utilise $2y$ → on normalise
                String normalizedHash = hashedPassword.replace("$2y$", "$2a$");
                return BCrypt.checkpw(plainPassword, normalizedHash);
            } catch (Exception e) {
                System.err.println("[PasswordUtils] Erreur BCrypt: " + e.getMessage());
                return false;
            }
        }

        // Sinon, comparaison en texte clair (ancien code)
        return plainPassword.equals(hashedPassword);
    }
}
