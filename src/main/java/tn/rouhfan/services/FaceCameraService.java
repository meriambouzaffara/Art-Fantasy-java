package tn.rouhfan.services;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tn.rouhfan.tools.AppLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de gestion de la caméra pour la reconnaissance faciale.
 * Version avec détection d'index automatique et conversion ligne par ligne.
 */
public class FaceCameraService {

    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private final FaceRecognitionService faceService;

    private Rect[] lastDetectedFaces = new Rect[0];
    private Mat lastFrame;

    public FaceCameraService(FaceRecognitionService faceService) {
        this.faceService = faceService;
    }

    public boolean startCamera(ImageView imageView, FaceDetectionCallback onFaceDetected) {
        if (cameraActive) return true;

        try {
            capture = new VideoCapture();
            
            // TENTATIVE : On essaie l'index 1 (souvent iVCam) puis l'index 0
            int[] indices = {1, 0, 2}; 
            boolean opened = false;
            
            for (int idx : indices) {
                AppLogger.info("[Camera] Tentative d'ouverture index: " + idx);
                opened = capture.open(idx, Videoio.CAP_DSHOW);
                if (opened && capture.isOpened()) {
                    // Vérifier si on arrive à lire un frame
                    Mat test = new Mat();
                    if (capture.read(test) && !test.empty()) {
                        AppLogger.info("[Camera] ✅ Caméra fonctionnelle trouvée sur l'index " + idx);
                        test.release();
                        break;
                    }
                    test.release();
                    capture.release();
                    opened = false;
                }
            }

            if (!opened) {
                AppLogger.error("[Camera] Aucune caméra fonctionnelle trouvée sur les index 0, 1, 2", null);
                return false;
            }

            AppLogger.info("[Camera] Résolution: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH) + "x" + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

            cameraActive = true;
            AtomicInteger frameCount = new AtomicInteger(0);

            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(() -> {
                if (!cameraActive || capture == null) return;

                Mat frame = new Mat();
                try {
                    if (capture.read(frame) && !frame.empty()) {
                        // Miroir
                        Core.flip(frame, frame, 1);

                        // Détection
                        lastDetectedFaces = faceService.detectFaces(frame);
                        if (lastFrame != null) lastFrame.release();
                        lastFrame = frame.clone();

                        // Overlay
                        for (Rect face : lastDetectedFaces) {
                            Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(36, 17, 151), 3);
                        }

                        // Conversion
                        Image fxImage = matToFxImageSafe(frame);
                        
                        if (fxImage != null) {
                            Platform.runLater(() -> {
                                imageView.setImage(fxImage);
                                if (onFaceDetected != null) {
                                    onFaceDetected.onDetection(lastDetectedFaces.length);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    // Log silencieux pour ne pas spammer
                } finally {
                    frame.release();
                }
            }, 500, 100, TimeUnit.MILLISECONDS); // 10 FPS pour maximiser la compatibilité

            return true;
        } catch (Exception e) {
            AppLogger.error("[Camera] Erreur init: " + e.getMessage(), e);
            return false;
        }
    }

    public void stopCamera() {
        cameraActive = false;
        if (timer != null) {
            timer.shutdown();
        }
        if (capture != null) {
            capture.release();
            capture = null;
        }
    }

    /**
     * Conversion ligne par ligne pour éviter les problèmes de stride/alignement mémoire
     */
    private Image matToFxImageSafe(Mat mat) {
        try {
            int width = mat.cols();
            int height = mat.rows();
            
            Mat bgra = new Mat();
            Imgproc.cvtColor(mat, bgra, Imgproc.COLOR_BGR2BGRA);

            WritableImage wimg = new WritableImage(width, height);
            byte[] rowBuffer = new byte[width * 4];
            
            for (int y = 0; y < height; y++) {
                bgra.get(y, 0, rowBuffer);
                wimg.getPixelWriter().setPixels(0, y, width, 1, 
                    PixelFormat.getByteBgraInstance(), rowBuffer, 0, width * 4);
            }
            
            bgra.release();
            return wimg;
        } catch (Exception e) {
            return null;
        }
    }

    public double[] captureAndExtract() {
        if (lastFrame == null || lastDetectedFaces.length == 0) return null;
        return faceService.extractEmbedding(lastFrame, lastDetectedFaces[0]);
    }

    public interface FaceDetectionCallback {
        void onDetection(int faceCount);
    }
}
