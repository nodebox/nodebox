package nodebox.client;

import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;

public class ProcessingFrame extends JFrame {
    private PApplet applet;

    public ProcessingFrame() {
        super("Processing Frame");
        setLayout(new BorderLayout());
        applet = new Applet();
        add(applet, BorderLayout.CENTER);
        applet.init();
        setSize(500, 500);
    }

    private class Applet extends PApplet {
        public void setup() {
            size(500, 500);
            noStroke();
            fill(255);
            smooth();
        }

        public void draw() {
            background(150, 150, 200);
            translate(250, 250);
            rotate(radians(frameCount * 3));
            rect(0, 0, 100, 100);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new ProcessingFrame();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

