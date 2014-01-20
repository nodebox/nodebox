package nodebox.client;

import ddf.minim.AudioInput;
import ddf.minim.Minim;
import processing.core.PApplet;

public class MinimInputApplet extends PApplet {
    private Minim minim;
    private AudioInput input;

    public void setup() {
        minim = new Minim(this);
        input = minim.getLineIn();
        input.setVolume(0);
        input.setGain(-64);
    }

    public AudioInput getInput() {
        if (input == null) return null;
        return input;
    }

    public void draw() {
    }

    @Override
    public void stop() {
        if (input != null)
            input.close();
        input = null;
        if (minim != null)
            minim.stop();
    }
}
