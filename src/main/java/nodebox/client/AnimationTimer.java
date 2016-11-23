package nodebox.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnimationTimer implements ActionListener {

    private NodeBoxDocument document;
    private Timer timer;
    private boolean doingAction = false;
    private double frame;

    public AnimationTimer(NodeBoxDocument document) {
        this.document = document;
        timer = new Timer(1000 / 60, this);
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void setFrame(double frame) {
        this.doingAction = true;
        this.frame = frame;
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        // Timer has fired.
        if(this.doingAction) {
            this.doingAction = false;
            document.setFrame(this.frame);
            timer.stop();
        }
        else {
            document.nextFrame();
        }

    }
}
