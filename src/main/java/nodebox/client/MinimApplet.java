package nodebox.client;

import ddf.minim.*;
import processing.core.PApplet;

public class MinimApplet extends PApplet {
    private Minim minim;
    private AudioPlayer player = null;
    private String fileName;
    private boolean loop;

    public MinimApplet(String fileName, boolean loop) {
        this.fileName = fileName;
        this.loop = loop;
    }

    public void setup() {
        minim = new Minim(this);
        player = minim.loadFile(fileName, 1024);
        if (loop)
            player.loop();
    }

    public AudioBuffer getMix() {
        if (player == null) return null;
        return player.mix;
    }

    public AudioPlayer getPlayer() {
        if (player == null) return null;
        return player;
    }

    public void draw() {
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
