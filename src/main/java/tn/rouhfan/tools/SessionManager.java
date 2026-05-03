package tn.rouhfan.tools;

import tn.rouhfan.entities.User;

/**
 * Singleton qui stocke l'utilisateur connecté pendant toute la session.
 * Permet d'accéder au user courant depuis n'importe quel contrôleur.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Enregistre l'utilisateur connecté dans la session.
     */
    public void login(User user) {
        this.currentUser = user;
    }

    /**
     * Déconnecte l'utilisateur (vide la session).
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Retourne l'utilisateur actuellement connecté, ou null.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Vérifie si un utilisateur est connecté.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Extrait le rôle principal de l'utilisateur connecté.
     * Le champ roles est au format JSON : "[\"ROLE_ADMIN\"]"
     * Retourne le premier rôle trouvé, ou "ROLE_USER" par défaut.
     */
    public String getRole() {
        if (currentUser == null || currentUser.getRoles() == null) {
            return "ROLE_USER";
        }
        String roles = currentUser.getRoles().trim();
        // Enlever les crochets et guillemets: ["ROLE_ADMIN"] → ROLE_ADMIN
        roles = roles.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "")
                .trim();

        // S'il y a plusieurs rôles séparés par des virgules, prendre le premier
        if (roles.contains(",")) {
            roles = roles.split(",")[0].trim();
        }
        return roles.isEmpty() ? "ROLE_USER" : roles;
    }

    /**
     * Retourne le nom complet de l'utilisateur connecté.
     */
    public String getFullName() {
        if (currentUser == null) {
            return "Utilisateur";
        }
        return currentUser.getPrenom() + " " + currentUser.getNom();
    }

    /**
     * Vérifie si l'utilisateur connecté possède un rôle spécifique.
     */
    public boolean hasRole(String role) {
        if (currentUser == null || currentUser.getRoles() == null) {
            return false;
        }
        return currentUser.getRoles().contains(role);
    }

    /**
     * Vérifie la sécurité et redirige vers le login si non autorisé.
     * @return true si l'accès est autorisé, false sinon
     */
    public boolean checkAccess(String requiredRole) {
        if (!isLoggedIn()) {
            System.err.println("[SECURITY] Accès refusé: aucun utilisateur connecté");
            return false;
        }
        if (!hasRole(requiredRole)) {
            System.err.println("[SECURITY] Accès refusé: rôle requis=" + requiredRole + ", rôle actuel=" + getRole());
            return false;
        }
        return true;
    }
}
