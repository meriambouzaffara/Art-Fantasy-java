package tn.rouhfan.services;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

public class AudioAnalyzerService {

    public interface AnalysisCallback {
        void onAnalysisComplete(String prompt);
        void onError(String error);
    }

    private MediaPlayer mediaPlayer;
    private double totalMagnitude = 0;
    private double bassSpikes = 0;
    private int sampleCount = 0;
    private double lastBassAverage = -60.0;
    private long startTime = 0;

    // Durée d'analyse (en ms)
    private static final long ANALYSIS_DURATION = 8000; 

    public void analyze(File audioFile, AnalysisCallback callback) {
        try {
            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setAudioSpectrumInterval(0.1); // Analyse toutes les 100ms
            mediaPlayer.setAudioSpectrumNumBands(128); // 128 bandes de fréquence

            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                if (startTime == 0) startTime = System.currentTimeMillis();

                // 1. Calcul de l'intensité globale (moyenne des magnitudes)
                double currentMagnitudeSum = 0;
                for (float mag : magnitudes) {
                    currentMagnitudeSum += mag; // Valeurs négatives (ex: -60 à 0 dB)
                }
                double avgMagnitude = currentMagnitudeSum / magnitudes.length;
                totalMagnitude += avgMagnitude;

                // 2. Calcul du rythme (Basses fréquences - Bandes 0 à 10)
                double currentBassSum = 0;
                for (int i = 0; i < 10; i++) {
                    currentBassSum += magnitudes[i];
                }
                double currentBassAvg = currentBassSum / 10.0;

                // Si on a un pic de basse significatif
                if (currentBassAvg > lastBassAverage + 5.0) {
                    bassSpikes++;
                }
                lastBassAverage = currentBassAvg;

                sampleCount++;

                // Vérifier si la durée d'analyse est écoulée
                if (System.currentTimeMillis() - startTime >= ANALYSIS_DURATION) {
                    stopAndGeneratePrompt(callback);
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (sampleCount > 0) {
                    stopAndGeneratePrompt(callback);
                } else {
                    callback.onError("Le fichier audio est trop court.");
                }
            });

            mediaPlayer.setOnError(() -> {
                callback.onError("Erreur lors de la lecture du fichier audio: " + mediaPlayer.getError().getMessage());
            });

            // Lancer la lecture (avec le son actif pour l'immersion)
            mediaPlayer.setVolume(0.5);
            mediaPlayer.play();

        } catch (Exception e) {
            callback.onError("Impossible d'analyser le fichier: " + e.getMessage());
        }
    }

    private void stopAndGeneratePrompt(AnalysisCallback callback) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
                    mediaPlayer.stop();
                }
                mediaPlayer.dispose();
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }

        double finalAvgMagnitude = totalMagnitude / sampleCount; // Ex: -30 dB (Fort), -50 dB (Calme)
        double finalBassSpikes = bassSpikes; // Nombre de pics sur la durée

        // Dérivation des mots-clés
        String shapes;
        String style;
        String colors;
        String mood;

        // Rythme -> Formes
        if (finalBassSpikes > 15) { // Rythme très marqué (Rock, Electro)
            shapes = "sharp geometric shapes, shattered lines, chaotic geometry";
            style = "dynamic, abstract, high energy, cyberpunk";
            mood = "energetic and powerful";
        } else if (finalBassSpikes > 5) { // Rythme modéré (Pop, Jazz)
            shapes = "flowing curves mixed with structured lines";
            style = "modern art, vibrant, expressive";
            mood = "uplifting and engaging";
        } else { // Rythme lent (Classique, Ambiance)
            shapes = "soft organic curves, fluid elements, dreamlike clouds";
            style = "ethereal painting, soft focus, fantasy illustration";
            mood = "peaceful, calm, and majestic";
        }

        // Intensité -> Couleurs (Magnitudes en JavaFX sont souvent entre -60 et 0)
        if (finalAvgMagnitude > -25) { // Son très fort/dense
            colors = "vibrant contrasting colors, neon glowing colors, highly saturated";
        } else if (finalAvgMagnitude > -35) { // Son moyen
            colors = "rich warm colors, deep hues, balanced palette";
        } else { // Son doux/clairsemé
            colors = "pastel colors, soft watercolor palette, muted tones";
        }

        // Construction du prompt final
        String prompt = "A beautiful artwork inspired by music, " + mood + " atmosphere, " +
                style + ", " + shapes + ", " + colors + ", masterpiece, highly detailed, 8k resolution, trending on artstation";

        // Réinitialiser
        totalMagnitude = 0;
        bassSpikes = 0;
        sampleCount = 0;
        startTime = 0;

        callback.onAnalysisComplete(prompt);
    }
    
    public void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
                    mediaPlayer.stop();
                }
                mediaPlayer.dispose();
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }
}
