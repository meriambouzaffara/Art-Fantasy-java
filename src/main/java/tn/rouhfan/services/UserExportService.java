package tn.rouhfan.services;

import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Service d'export des données utilisateurs en CSV.
 * Inclut : id, nom, prénom, email, rôle, date création, statut, type.
 */
public class UserExportService {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /**
     * Exporte une liste d'utilisateurs en fichier CSV.
     * @param users liste des utilisateurs à exporter
     * @param file fichier de destination
     * @return nombre d'utilisateurs exportés
     */
    public int exportToCSV(List<User> users, File file) throws Exception {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Aucun utilisateur à exporter.");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            // BOM UTF-8 pour Excel
            writer.print("\uFEFF");

            // En-tête
            writer.println("ID;Nom;Prénom;Email;Rôle;Statut;Type;Date de création");

            // Données
            for (User u : users) {
                String role = cleanRole(u.getRoles());
                String dateStr = u.getCreatedAt() != null ? SDF.format(u.getCreatedAt()) : "";

                writer.println(
                        u.getId() + ";" +
                        escapeCsv(u.getNom()) + ";" +
                        escapeCsv(u.getPrenom()) + ";" +
                        escapeCsv(u.getEmail()) + ";" +
                        escapeCsv(role) + ";" +
                        escapeCsv(u.getStatut()) + ";" +
                        escapeCsv(u.getType()) + ";" +
                        dateStr
                );
            }

            AppLogger.info("[UserExport] " + users.size() + " utilisateurs exportés vers: " + file.getAbsolutePath());
            return users.size();
        }
    }

    /**
     * Nettoie le format du rôle (enlève les crochets et guillemets JSON).
     */
    private String cleanRole(String roles) {
        if (roles == null) return "";
        return roles.replace("[", "").replace("]", "")
                .replace("\"", "").replace("'", "").trim();
    }

    /**
     * Échappe les caractères spéciaux CSV.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
