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
        // Créer le répertoire de base s'il n'existe pas
        File uploadsDir = new File(UPLOADS_DIR);
        if (!uploadsDir.exists()) uploadsDir.mkdirs();

        // Sous-répertoires par défaut
        String[] defaults = {"oeuvres", "categories", "evenements", "sponsors", "misc"};
        for (String sub : defaults) {
            new File(UPLOADS_DIR + File.separator + sub).mkdirs();
        }
    }

    /**
     * Résout un chemin de la base de données vers un chemin absolu/URL pour l'affichage.
     * Supporte :
     * - Anciens chemins "/images/..." (redirigés vers uploads/misc/)
     * - Nouveaux chemins "uploads/..."
     * - Recherche dynamique par nom de fichier dans tout le dossier uploads/
     */
    public static String getAbsolutePath(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) return null;

        // Nettoyer le chemin
        String normalizedPath = dbPath.replace("\\", "/");
        if (normalizedPath.startsWith("/")) normalizedPath = normalizedPath.substring(1);

        // Liste des dossiers racines possibles
        String[] possibleRoots = {"src/main/resources", "target/classes", "."};

        // Heuristique pour extraire le chemin relatif de recherche (searchPath)
        String searchPath = normalizedPath;
        if (normalizedPath.contains("uploads/")) {
            searchPath = normalizedPath.substring(normalizedPath.indexOf("uploads/"));
        } else if (normalizedPath.contains("images/")) {
            // Mapping spécial legacy: images/ -> uploads/misc/
            searchPath = "uploads/misc/" + normalizedPath.substring(normalizedPath.indexOf("images/") + 7);
        }

        // 1. Essai direct
        for (String root : possibleRoots) {
            File file = new File(root + File.separator + searchPath);
            if (file.exists()) return file.toURI().toString();
        }

        // 2. Recherche par nom de fichier dans TOUS les sous-dossiers de uploads
        String fileName = searchPath.contains("/") ? searchPath.substring(searchPath.lastIndexOf("/") + 1) : searchPath;

        for (String root : possibleRoots) {
            File uploadsDir = new File(root + File.separator + "uploads");
            if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                // Tenter à la racine de uploads
                File directFile = new File(uploadsDir, fileName);
                if (directFile.exists()) return directFile.toURI().toString();

                // Tenter dans tous les sous-dossiers
                File[] subDirs = uploadsDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        File fallbackFile = new File(subDir, fileName);
                        if (fallbackFile.exists()) return fallbackFile.toURI().toString();
                    }
                }
            }

            // Tenter aussi dans l'ancien dossier images si présent
            File oldImagesDir = new File(root + File.separator + "images");
            if (oldImagesDir.exists()) {
                File fallbackFile = new File(oldImagesDir, fileName);
                if (fallbackFile.exists()) return fallbackFile.toURI().toString();
            }
        }

        // 3. Essai ClassLoader
        try {
            var resource = ImageUtils.class.getResource("/" + searchPath);
            if (resource != null) return resource.toExternalForm();
            resource = ImageUtils.class.getResource("/" + normalizedPath);
            if (resource != null) return resource.toExternalForm();
        } catch (Exception ignored) {}

        System.err.println("⚠️ Image non trouvée : " + dbPath + " (Cherché : " + searchPath + ")");
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
