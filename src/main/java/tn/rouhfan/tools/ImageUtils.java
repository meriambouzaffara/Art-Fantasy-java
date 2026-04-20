package tn.rouhfan.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ImageUtils {

    public static final String RESOURCES_DIR = "src/main/resources";
    public static final String UPLOADS_DIR = "src/main/resources/uploads";
    private static final String IMAGES_RELATIVE = "/images/";

    static {
        // Créer les répertoires s'ils n'existent pas
        File uploadsDir = new File(UPLOADS_DIR);
        if (!uploadsDir.exists()) uploadsDir.mkdirs();
        
        new File(UPLOADS_DIR + "/oeuvres").mkdirs();
        new File(UPLOADS_DIR + "/categories").mkdirs();
    }

    /**
     * Résout un chemin de la base de données vers un chemin absolu/URL pour l'affichage
     * Gère les anciens chemins "/images/..." et les nouveaux "uploads/..."
     */
    public static String getAbsolutePath(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) return null;

        // Nettoyer le chemin (remplacer les antislashes par des slashes)
        String normalizedPath = dbPath.replace("\\", "/");
        
        // Liste des dossiers racines possibles où chercher l'image
        String[] possibleRoots = {
            "src/main/resources",
            "target/classes",
            "." // Racine du projet
        };

        // Heuristique pour les anciens chemins absolus : 
        // Si le chemin contient "uploads/" ou "images/", on extrait la partie intéressante
        String searchPath = normalizedPath;
        if (normalizedPath.contains("/uploads/")) {
            searchPath = normalizedPath.substring(normalizedPath.indexOf("uploads/"));
        } else if (normalizedPath.contains("/images/")) {
            searchPath = normalizedPath.substring(normalizedPath.indexOf("images/"));
        } else if (normalizedPath.contains("uploads/")) {
            searchPath = normalizedPath.substring(normalizedPath.indexOf("uploads/"));
        }

        for (String root : possibleRoots) {
            File file = new File(root + File.separator + searchPath);
            if (file.exists()) {
                return file.toURI().toString();
            }
        }
        
        // --- NOUVEAU : Fallback de recherche par nom de fichier dans uploads ---
        // Si l'image n'est pas trouvée au chemin exact, on cherche si elle existe dans uploads/oeuvres ou uploads/categories
        if (searchPath.contains("/")) {
            String fileName = searchPath.substring(searchPath.lastIndexOf("/") + 1);
            String[] subDirs = {"uploads/oeuvres", "uploads/categories", "uploads", "images"};
            
            for (String root : possibleRoots) {
                for (String subDir : subDirs) {
                    File fallbackFile = new File(root + File.separator + subDir + File.separator + fileName);
                    if (fallbackFile.exists()) {
                        return fallbackFile.toURI().toString();
                    }
                }
            }
        }

        // Si non trouvé, tenter le chemin d'origine tel quel (cas des absolus encore valides)
        File originalFile = new File(dbPath);
        if (originalFile.exists()) {
            return originalFile.toURI().toString();
        }

        // Si non trouvé dans les dossiers racines, essayer via le ClassLoader (pour les JARs)
        try {
            var resource = ImageUtils.class.getResource("/" + searchPath);
            if (resource != null) return resource.toExternalForm();
            
            resource = ImageUtils.class.getResource(searchPath);
            if (resource != null) return resource.toExternalForm();
        } catch (Exception e) {
            // Ignorer les erreurs de ressource
        }

        System.err.println("⚠️ Image non trouvée (Tentatives sur : " + searchPath + ") | Path d'origine : " + dbPath);
        return null;
    }

    /**
     * Alias pour getAbsolutePath pour assurer la compatibilité avec le reste du code
     */
    public static String getImageUrl(String dbPath) {
        return getAbsolutePath(dbPath);
    }

    /**
     * Sauvegarde une image uploadée dans le dossier approprié dans resources
     * @param sourceFile Le fichier source choisi par l'utilisateur
     * @param subDir "oeuvres" ou "categories"
     * @return Le chemin relatif à stocker en base de données (ex: uploads/oeuvres/name.png)
     */
    public static String saveUpload(File sourceFile, String subDir) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) return null;

        String originalName = sourceFile.getName();
        // Nettoyer les anciens UUIDs pour éviter l'accumulation (pattern 8-4-4-4-12)
        String cleanedName = originalName.replaceAll("^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_)+", "");
        String fileName = UUID.randomUUID().toString() + "_" + cleanedName;
        File destDir = new File(UPLOADS_DIR + File.separator + subDir);
        if (!destDir.exists()) destDir.mkdirs();

        File destFile = new File(destDir, fileName);
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // On retourne le chemin relatif attendu: uploads/sous-dossier/nom
        return "uploads" + "/" + subDir + "/" + fileName;
    }

    /**
     * Pour compatibilité avec l'ancien code si nécessaire
     * @deprecated Utiliser saveUpload pour les nouvelles oeuvres/categories
     */
    @Deprecated
    public static String copyImage(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) return "";
        try {
            File sourceFile = new File(sourcePath);
            return saveUpload(sourceFile, "misc").replace("uploads/misc/", IMAGES_RELATIVE);
        } catch (IOException e) {
            System.err.println("❌ Erreur copie image: " + e.getMessage());
            return "";
        }
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) return "png";
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    public static void deleteImage(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) return;
        try {
            String fullPath = getAbsolutePath(dbPath);
            if (fullPath != null) {
                File file = new File(new java.net.URI(fullPath));
                if (file.exists()) Files.delete(file.toPath());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression image: " + e.getMessage());
        }
    }

    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".gif") || 
               name.endsWith(".bmp");
    }
}
