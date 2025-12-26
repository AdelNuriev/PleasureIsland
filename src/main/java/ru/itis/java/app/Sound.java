package ru.itis.java.app;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import java.net.URL;

public class Sound {

    private Clip clip;
    private URL[] soundURL = new URL[30];

    public Sound() {
        soundURL[0] = getClass().getResource("/sound/main-theme.wav");
        soundURL[1] = getClass().getResource("/sound/walking-on-grass.wav");
        soundURL[2] = getClass().getResource("/sound/walking-on-grass.wav");
        soundURL[3] = getClass().getResource("/sound/walking-on-grass.wav");
        soundURL[4] = getClass().getResource("/sound/walking-on-grass.wav");
        soundURL[5] = getClass().getResource("/sound/walking-on-grass.wav");
    }

    public void setFile(int index) {
        try {
            AudioInputStream ias = AudioSystem.getAudioInputStream(soundURL[index]);
            clip = AudioSystem.getClip();
            clip.open(ias);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки звукового эффекта: " + e.getMessage());
        }
    }

    public void play() {
        clip.start();
    }

    public void loop() {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stop() {
        clip.stop();
    }

    public Clip getClip() { return clip; }
    public URL[] getSoundURL() { return soundURL; }
}
