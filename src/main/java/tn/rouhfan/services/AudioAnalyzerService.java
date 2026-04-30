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
    private double highFrequencySum = 0;
    private long startTime = 0;
    private java.util.Random random = new java.util.Random();

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
                if (currentBassAvg > lastBassAverage + 5.0) {
                    bassSpikes++;
                }
                lastBassAverage = currentBassAvg;

                // 3. Hautes fréquences (Bandes 100 à 127) pour la clarté/détail
                double currentHighSum = 0;
                for (int i = 100; i < 127; i++) {
                    currentHighSum += magnitudes[i];
                }
                highFrequencySum += (currentHighSum / 27.0);

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

        double finalAvgMagnitude = totalMagnitude / sampleCount;
        double finalBassSpikes = bassSpikes;
        double finalHighAvg = highFrequencySum / sampleCount;

        // --- POOLS DE MOTS-CLÉS ---
        String[] shapesPool;
        String[] stylesPool;
        String[] moodPool;
        
        // 1. Rythme (Basses) -> Formes et Énergie
        if (finalBassSpikes > 15) { // Électro, Rock intense
            shapesPool = new String[]{"sharp geometric structures", "shattered glass patterns", "exploding polygons", "jagged neon lines", "aggressive glitch architecture"};
            moodPool = new String[]{"electrifying", "frenetic", "powerful", "rebellious", "chaotic"};
        } else if (finalBassSpikes > 5) { // Pop, Jazz, Tempo modéré
            shapesPool = new String[]{"intertwined flowing lines", "pulsating waves", "dynamic curves", "vibrant swirls", "rhythmic geometric patterns"};
            moodPool = new String[]{"upbeat", "expressive", "lively", "harmonious", "soulful"};
        } else { // Calme, Classique
            shapesPool = new String[]{"ethereal mist", "soft floating spheres", "gentle organic ripples", "undulating silk textures", "drifting celestial clouds"};
            moodPool = new String[]{"zen", "meditative", "dreamy", "serene", "mystical"};
        }

        // 2. Intensité (Volume) -> Style Artistique
        if (finalAvgMagnitude > -25) { // Fort
            stylesPool = new String[]{"maximalism", "cyberpunk aesthetic", "abstract expressionism", "futuristic 3D surrealism", "vibrant street art"};
        } else if (finalAvgMagnitude > -35) { // Moyen
            stylesPool = new String[]{"contemporary impressionism", "lush fantasy illustration", "expressive oil painting", "modern digital fusion", "art nouveau style"};
        } else { // Doux
            stylesPool = new String[]{"minimalist watercolor", "ethereal digital painting", "soft focus impressionism", "delicate pencil sketch with wash", "fine art photography style"};
        }

        // 3. Clarté (Hautes fréquences) -> Couleurs et Détails
        String colors;
        if (finalHighAvg > -35) { // Très clair/cristallin
            String[] colorPool = {"prismatic rainbow hues", "glowing neon accents on dark background", "iridescent pearls and silver", "electric cyan and magenta"};
            colors = colorPool[random.nextInt(colorPool.length)];
        } else { // Plus sombre ou sourd
            String[] colorPool = {"deep amber and charcoal", "muted earth tones and copper", "velvet indigo and gold", "dark moody gradients with soft highlights"};
            colors = colorPool[random.nextInt(colorPool.length)];
        }

        // --- SÉLECTION ALÉATOIRE ---
        String chosenShape = shapesPool[random.nextInt(shapesPool.length)];
        String chosenStyle = stylesPool[random.nextInt(stylesPool.length)];
        String chosenMood = moodPool[random.nextInt(moodPool.length)];
        
        // Texture supplémentaire basée sur les hautes fréquences
        String texture = (finalHighAvg > -40) ? "intricate crystalline details, sharp focus" : "soft blurred textures, painterly strokes";

        // Construction du prompt final
        String prompt = "A unique " + chosenStyle + " masterpiece, " + chosenMood + " atmosphere, featuring " + 
                chosenShape + ", with " + colors + ", " + texture + ", inspired by music, " +
                "highly detailed, 8k resolution, cinematic lighting, masterpiece, trending on artstation";

        // Réinitialiser
        totalMagnitude = 0;
        bassSpikes = 0;
        highFrequencySum = 0;
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
