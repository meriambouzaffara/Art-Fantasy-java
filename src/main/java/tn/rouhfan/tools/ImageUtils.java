package tn.rouhfan.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ImageUtils {

    private static final String IMAGES_DIR = "src/main/resources/images";
    private static final String IMAGES_RELATIVE = "/images/";

    static {
        // Créer le répertoire s'il n'existe pas
        File dir = new File(IMAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Copie une image vers le répertoire des ressources
     * @param sourcePath - chemin source de l'image
     * @return chemin relatif de l'image copiée
     */
    public static String copyImage(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return "";
        }

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                System.err.println("❌ Fichier source n'existe pas: " + sourcePath);
                return "";
            }

            // Générer un nom unique
            String extension = getFileExtension(sourceFile);
            String fileName = UUID.randomUUID() + "." + extension;
            String destPath = IMAGES_DIR + File.separator + fileName;

            // Copier le fichier
            Files.copy(
                    sourceFile.toPath(),
                    Paths.get(destPath)
            );

            System.out.println("✅ Image copiée: " + destPath);
            return IMAGES_RELATIVE + fileName;

        } catch (IOException e) {
            System.err.println("❌ Erreur copie image: " + e.getMessage());
            return "";
        }
    }

    /**
     * Récupère l'extension d'un fichier
     */
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "png";
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * Supprime une image du répertoire des ressources
     */
    public static void deleteImage(String relativePath) {
        if (relativePath == null || relativePath.isEmpty() || !relativePath.startsWith(IMAGES_RELATIVE)) {
            return;
        }

        try {
            String fileName = relativePath.replace(IMAGES_RELATIVE, "");
            File file = new File(IMAGES_DIR + File.separator + fileName);
            if (file.exists()) {
                Files.delete(file.toPath());
                System.out.println("✅ Image supprimée: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur suppression image: " + e.getMessage());
        }
    }

    /**
     * Vérifie si c'est une image valide
     */
    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists()) return false;
        
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".gif") || 
               name.endsWith(".bmp");
    }

    /**
     * Obtient le chemin complet pour afficher l'image
     */
    public static String getImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        
        if (relativePath.startsWith("/images/")) {
            String fileName = relativePath.replace("/images/", "");
            File file = new File(IMAGES_DIR + File.separator + fileName);
            if (file.exists()) {
                return file.toURI().toString();
            }
        }
        
        return null;
    }
}
