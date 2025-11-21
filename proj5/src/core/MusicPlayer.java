package core;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;


public class MusicPlayer {
    private Clip clip;

    public void playLoop(String filepath) {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filepath));
            clip = AudioSystem.getClip();
            clip.open(audio);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-20.0f);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()){
            clip.stop();
            clip.close();
        }
    }
}
