package tn.rouhfan.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import tn.rouhfan.entities.User;
import tn.rouhfan.tools.AppLogger;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;

/**
 * Service de reconnaissance faciale utilisant OpenCV DNN.
 * - Détection de visage via Haar Cascade
 * - Extraction d'embedding via OpenCV DNN (OpenFace model)
 * - Comparaison par similarité cosinus
 */
public class FaceRecognitionService {

    private static final double SIMILARITY_THRESHOLD = 0.75;
    private static final int EMBEDDING_SIZE = 128;

    private CascadeClassifier faceDetector;
    private Net embeddingNet;
    private boolean initialized = false;

    private final UserService userService = new UserService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialise OpenCV et charge les modèles.
     */
    public boolean initialize() {
        if (initialized) return true;

        try {
            // Charger la librairie native OpenCV
            nu.pattern.OpenCV.loadLocally();
            AppLogger.info("[FaceRecognition] OpenCV chargé: " + Core.VERSION);

            // Charger le détecteur de visages Haar Cascade
            String haarPath = extractResource("/models/haarcascade_frontalface_default.xml");
            if (haarPath == null) {
                AppLogger.error("[FaceRecognition] Haar cascade introuvable", null);
                return false;
            }
            faceDetector = new CascadeClassifier(haarPath);
            if (faceDetector.empty()) {
                AppLogger.error("[FaceRecognition] Impossible de charger le Haar cascade", null);
                return false;
            }
            AppLogger.info("[FaceRecognition] Haar cascade chargé");

            // Charger le modèle d'embedding DNN (OpenFace)
            String modelPath = extractResource("/models/openface_nn4.small2.v1.t7");
            if (modelPath != null) {
                embeddingNet = Dnn.readNetFromTorch(modelPath);
                AppLogger.info("[FaceRecognition] Modèle d'embedding chargé");
            } else {
                AppLogger.warn("[FaceRecognition] Modèle d'embedding introuvable - mode détection uniquement");
            }

            initialized = true;
            return true;

        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur d'initialisation: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Détecte les visages dans une image.
     * @return Liste des rectangles des visages détectés
     */
    public Rect[] detectFaces(Mat frame) {
        if (!initialized || faceDetector == null) return new Rect[0];

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 5,
                0, new Size(80, 80), new Size());

        gray.release();
        return faces.toArray();
    }

    /**
     * Extrait l'embedding facial d'un visage détecté.
     * @param frame Image source
     * @param faceRect Rectangle du visage
     * @return Vecteur d'embedding (128 dimensions) ou null
     */
    public double[] extractEmbedding(Mat frame, Rect faceRect) {
        if (embeddingNet == null) {
            AppLogger.warn("[FaceRecognition] Modèle d'embedding non chargé");
            return null;
        }

        try {
            // Extraire la région du visage
            Mat face = new Mat(frame, faceRect);

            // Préparer le blob pour le réseau DNN
            Mat blob = Dnn.blobFromImage(face, 1.0 / 255.0,
                    new Size(96, 96), new Scalar(0, 0, 0), true, false);

            embeddingNet.setInput(blob);
            Mat output = embeddingNet.forward();

            // Extraire le vecteur d'embedding
            double[] embedding = new double[EMBEDDING_SIZE];
            for (int i = 0; i < EMBEDDING_SIZE && i < output.cols(); i++) {
                embedding[i] = output.get(0, i)[0];
            }

            // Normaliser le vecteur
            double norm = 0;
            for (double v : embedding) norm += v * v;
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] /= norm;
                }
            }

            face.release();
            blob.release();
            output.release();

            return embedding;

        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur extraction embedding: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Calcule la similarité cosinus entre deux vecteurs d'embedding.
     * @return Valeur entre -1 et 1 (1 = identique)
     */
    public double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0;

        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    /**
     * Compare un embedding avec tous les utilisateurs ayant le face login activé.
     * @param embedding L'embedding à comparer
     * @return L'utilisateur correspondant ou null
     */
    public User findMatchingUser(double[] embedding) {
        if (embedding == null) return null;

        try {
            List<User> users = userService.recuperer();
            User bestMatch = null;
            double bestSimilarity = 0;

            for (User user : users) {
                if (!user.isFaceEnabled() || user.getFaceEmbedding() == null) continue;

                double[] storedEmbedding = deserializeEmbedding(user.getFaceEmbedding());
                if (storedEmbedding == null) continue;

                double similarity = cosineSimilarity(embedding, storedEmbedding);
                AppLogger.info("[FaceRecognition] Comparaison avec " + user.getEmail()
                        + " → similarité: " + String.format("%.4f", similarity));

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = user;
                }
            }

            if (bestSimilarity >= SIMILARITY_THRESHOLD) {
                AppLogger.info("[FaceRecognition] ✅ Match trouvé: " + bestMatch.getEmail()
                        + " (similarité: " + String.format("%.4f", bestSimilarity) + ")");
                return bestMatch;
            } else {
                AppLogger.info("[FaceRecognition] ❌ Aucun match (meilleure similarité: "
                        + String.format("%.4f", bestSimilarity) + ")");
                return null;
            }

        } catch (SQLException e) {
            AppLogger.error("[FaceRecognition] Erreur DB: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Enregistre l'embedding facial d'un utilisateur.
     */
    public boolean enrollFace(User user, double[] embedding) {
        if (embedding == null || user == null) return false;

        try {
            String json = serializeEmbedding(embedding);
            user.setFaceEmbedding(json);
            user.setFaceEnabled(true);
            userService.updateFaceData(user);
            AppLogger.info("[FaceRecognition] Visage enregistré pour: " + user.getEmail());
            return true;
        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur enregistrement visage: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Désactive le login facial d'un utilisateur.
     */
    public boolean disableFace(User user) {
        try {
            user.setFaceEmbedding(null);
            user.setFaceEnabled(false);
            userService.updateFaceData(user);
            AppLogger.info("[FaceRecognition] Face login désactivé pour: " + user.getEmail());
            return true;
        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur désactivation: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sérialise un embedding en JSON.
     */
    public String serializeEmbedding(double[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            AppLogger.error("[FaceRecognition] Erreur sérialisation", e);
            return null;
        }
    }

    /**
     * Désérialise un embedding depuis JSON.
     */
    public double[] deserializeEmbedding(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur désérialisation", e);
            return null;
        }
    }

    /**
     * Extrait une ressource vers un fichier temporaire.
     */
    private String extractResource(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                // Essayer aussi sans le /models/ prefix
                AppLogger.warn("[FaceRecognition] Ressource non trouvée: " + resourcePath);
                return null;
            }

            // Si c'est un fichier réel (pas dans un JAR), retourner le chemin direct
            if ("file".equals(url.getProtocol())) {
                return Paths.get(url.toURI()).toString();
            }

            // Sinon, extraire vers un fichier temp
            String fileName = Paths.get(resourcePath).getFileName().toString();
            Path tempFile = Files.createTempFile("opencv_", "_" + fileName);
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile.toFile().deleteOnExit();
            return tempFile.toString();

        } catch (Exception e) {
            AppLogger.error("[FaceRecognition] Erreur extraction ressource: " + e.getMessage(), e);
            return null;
        }
    }

    public boolean isInitialized() { return initialized; }
    public double getThreshold() { return SIMILARITY_THRESHOLD; }
}
