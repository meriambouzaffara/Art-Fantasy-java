package tn.rouhfan.services;

import tn.rouhfan.entities.ActivityLog;
import tn.rouhfan.tools.AppLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Service d'export des logs d'activité en fichier CSV.
 *
 * Génère un fichier CSV compatible Excel (UTF-8 BOM, séparateur point-virgule)
 * contenant les logs d'activité filtrés.
 *
 * ⚠️ Les données sensibles (clés API, tokens) sont déjà nettoyées
 * par ActivityLogService avant stockage.
 */
public class LogExportService {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Exporte une liste de logs d'activité en fichier CSV.
     *
     * @param logs Liste des logs à exporter
     * @param file Fichier de destination
     * @return Nombre de logs exportés
     * @throws Exception En cas d'erreur d'écriture
     */
    public int exportToCSV(List<ActivityLog> logs, File file) throws Exception {
        if (logs == null || logs.isEmpty()) {
            throw new IllegalArgumentException("Aucun log à exporter.");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            // BOM UTF-8 pour compatibilité Excel
            writer.print("\uFEFF");

            // En-tête CSV
            writer.println("ID;Date;User ID;Action;Niveau;Détails");

            // Données
            for (ActivityLog log : logs) {
                String dateStr = log.getTimestamp() != null ? SDF.format(log.getTimestamp()) : "";

                writer.println(
                        log.getId() + ";" +
                        dateStr + ";" +
                        log.getUserId() + ";" +
                        escapeCsv(log.getActionType()) + ";" +
                        escapeCsv(log.getLevel()) + ";" +
                        escapeCsv(log.getDetails())
                );
            }

            AppLogger.info("[LogExport] " + logs.size() + " logs exportés vers: " + file.getAbsolutePath());
            return logs.size();
        }
    }

    /**
     * Échappe les caractères spéciaux pour le format CSV.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        // Remplacer les retours à la ligne par des espaces
        value = value.replace("\n", " ").replace("\r", " ");
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
