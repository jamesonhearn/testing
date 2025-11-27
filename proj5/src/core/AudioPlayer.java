package core;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioPlayer {

    private Clip loopClip;
    private final List<Clip> effectsClips = new ArrayList<>();
    private final Random random = new Random();

    private long lastFootstepTime = 0;
    private static final long FOOTSTEP_COOLDOWN_MS = 240;

    // ------------------------------------------------------
    // Utility: Try to obtain a Clip safely
    // ------------------------------------------------------
    private Clip tryGetClip() {
        try {
            return AudioSystem.getClip();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            System.err.println("[Audio] No audio output device available. Sound disabled.");
            return null;
        }
    }

    // ------------------------------------------------------
    // Load a single WAV safely
    // ------------------------------------------------------
    private Clip loadClip(String filepath, float gainDb) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            Clip clip = tryGetClip();
            if (clip == null) return null;  // no device → silent mode

            try {
                clip.open(audio);
            } catch (LineUnavailableException e) {
                System.err.println("[Audio] Failed to open audio line: " + filepath);
                return null;
            }

            setVolume(clip, gainDb);
            return clip;

        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("[Audio] Unable to load WAV: " + filepath);
            return null;
        }
    }

    // ------------------------------------------------------
    // Set volume (if supported)
    // ------------------------------------------------------
    private void setVolume(Clip clip, float gainDb) {
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(gainDb);
        }
    }

    // ------------------------------------------------------
    // Public API
    // ------------------------------------------------------

    public void play(String filepath) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            loopClip = tryGetClip();
            if (loopClip == null) return;  // no device → silent mode

            try {
                loopClip.open(audio);
            } catch (LineUnavailableException e) {
                System.err.println("[Audio] Cannot open clip for: " + filepath);
                return;
            }

            setVolume(loopClip, -15f);
            loopClip.start();

        } catch (Exception e) {
            System.err.println("[Audio] Failed to play: " + filepath);
        }
    }

    public void playLoop(String filepath) {
        stopLoop();

        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            loopClip = tryGetClip();
            if (loopClip == null) return;

            try {
                loopClip.open(audio);
            } catch (LineUnavailableException e) {
                System.err.println("[Audio] Cannot open loop clip: " + filepath);
                return;
            }

            setVolume(loopClip, -15f);
            loopClip.loop(Clip.LOOP_CONTINUOUSLY);
            loopClip.start();

        } catch (Exception e) {
            System.err.println("[Audio] Failed to play loop: " + filepath);
        }
    }

    public void loadEffects(String... filepaths) {
        for (String path : filepaths) {
            Clip clip = loadClip(path, -15f);
            if (clip != null) {
                effectsClips.add(clip);
            }
        }
    }

    public void playRandomEffect() {
        long now = System.currentTimeMillis();
        if (now - lastFootstepTime < FOOTSTEP_COOLDOWN_MS) return;
        lastFootstepTime = now;

        if (effectsClips.isEmpty()) return;

        Clip clip = effectsClips.get(random.nextInt(effectsClips.size()));
        if (clip == null) return;

        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    public void stop() {
        if (loopClip != null && loopClip.isRunning()) {
            loopClip.stop();
            loopClip.close();
        }
    }

    private void stopLoop() {
        if (loopClip != null && loopClip.isRunning()) {
            loopClip.stop();
        }
        if (loopClip != null) {
            loopClip.close();
            loopClip = null;
        }
    }

    public void playThenCallback(String filepath, Runnable onComplete) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            loopClip = tryGetClip();
            if (loopClip == null) return;

            try {
                loopClip.open(audio);
            } catch (LineUnavailableException e) {
                System.err.println("[Audio] Cannot open clip for callback: " + filepath);
                return;
            }

            setVolume(loopClip, -15f);

            loopClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    loopClip.close();
                    if (onComplete != null) onComplete.run();
                }
            });

            loopClip.start();

        } catch (Exception e) {
            System.err.println("[Audio] Failed to playThenCallback: " + filepath);
        }
    }

    // Optional cleanup if needed (not required)
    private void closeEffects() {
        for (Clip clip : effectsClips) {
            if (clip != null && clip.isOpen()) {
                clip.stop();
                clip.close();
            }
        }
        effectsClips.clear();
    }
}
