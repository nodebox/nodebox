package nodebox.client;

import ddf.minim.*;
import ddf.minim.analysis.BeatDetect;
import processing.core.PApplet;

public class MinimApplet extends PApplet {
    private Minim minim;
    private AudioPlayer player = null;
    private String fileName;
    private BeatDetect beat;
    private boolean loop;

    public MinimApplet(String fileName, boolean loop) {
        this.fileName = fileName;
        this.loop = loop;
    }

    public void setup() {
        minim = new Minim(this);
        player = minim.loadFile(fileName, 1024);
        beat = new BeatDetect(player.bufferSize(), player.sampleRate());
        if (loop)
            player.loop();
    }

    public AudioPlayer getPlayer() {
        if (player == null) return null;
        return player;
    }

    public boolean isPaused() {
        if (player == null) return false;
        return !player.isPlaying();
    }

    public void pause() {
        if (player == null) return;
        player.pause();
    }

    public void play() {
        if (player == null) return;
        player.play();
    }

    public void mute() {
        if (player == null) return;
        player.mute();
    }

    public void unmute() {
        if (player == null) return;
        player.unmute();
    }

    public boolean isMuted() {
        if (player == null) return false;
        return player.isMuted();
    }

    public BeatDetect getBeatDetect() {
        return beat;
    }

    public void draw() {
        beat.detect(player.mix);
    }

    @Override
    public void stop() {
        if (player != null)
            player.close();
        player = null;
        if (minim != null)
            minim.stop();
    }
}
