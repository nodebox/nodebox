package nodebox.client;

import ddf.minim.AudioInput;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import processing.core.PApplet;

public class MinimInputApplet extends PApplet {
    private Minim minim;
    private AudioInput input;
    private BeatDetect beat;

    public void setup() {
        minim = new Minim(this);
        input = minim.getLineIn();
        beat = new BeatDetect(input.bufferSize(), input.sampleRate());
        input.setVolume(0);
        input.setGain(-64);
    }

    public AudioInput getInput() {
        if (input == null) return null;
        return input;
    }

    public BeatDetect getBeatDetect() {
        return beat;
    }

    public void draw() {
        beat.detect(input.mix);
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
