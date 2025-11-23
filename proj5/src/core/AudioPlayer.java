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
    private static final long FOOTSTEP_COOLDOWN_MS = 240;   // ~8 steps/sec

    public void play(String filepath) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            loopClip = AudioSystem.getClip();
            loopClip.open(audio);

            FloatControl volume = (FloatControl) loopClip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-15.0f);
            loopClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playLoop(String filepath) {
        stopLoop();
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            loopClip = AudioSystem.getClip();
            loopClip.open(audio);

            FloatControl volume = (FloatControl) loopClip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-15.0f);

            loopClip.loop(Clip.LOOP_CONTINUOUSLY);
            loopClip.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // load however many effects available
    public void loadEffects(String... filepaths) {
        for (String path : filepaths) {
            Clip clip = loadClip(path, -15.0f);
            if (clip != null) {
                effectsClips.add(clip);
            }
        }
    }

    public void playRandomEffect() {

        long now = System.currentTimeMillis();
        if (now - lastFootstepTime < FOOTSTEP_COOLDOWN_MS) {
            return;
        }

        lastFootstepTime = now;

        if (effectsClips.isEmpty()) {
            return;
        }
        Clip clip = effectsClips.get(random.nextInt(effectsClips.size()));
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }


    private Clip loadClip(String filepath, float gainDb) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            setVolume(clip, gainDb);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setVolume(Clip clip, float gainDb) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(gainDb);
        }
    }



    public void stop() {
        if (loopClip != null && loopClip.isRunning()){
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
