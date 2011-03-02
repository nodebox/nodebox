package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class AnimationBar extends JPanel implements ChangeListener {

    public static Image animationBackground;
    private static final int ANIMATION_BAR_HEIGHT = 27;

    static {
        try {
            animationBackground = ImageIO.read(new File("res/animation-background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final NodeBoxDocument document;
    private DraggableNumber frameNumber;
    private float frame;

    public AnimationBar(final NodeBoxDocument document) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 2));
        setPreferredSize(new Dimension(100, ANIMATION_BAR_HEIGHT));
        setMinimumSize(new Dimension(100, ANIMATION_BAR_HEIGHT));
        setMaximumSize(new Dimension(100, ANIMATION_BAR_HEIGHT));

        this.document = document;

        frameNumber = new DraggableNumber();
        frameNumber.addChangeListener(this);
        add(frameNumber);
        NButton startButton = new NButton("Start", "res/animation-start.png");
        startButton.setToolTipText("Start Animation");
        startButton.setActionMethod(this, "startAnimation");
        NButton stopButton = new NButton("Stop", "res/animation-stop.png");
        stopButton.setToolTipText("Stop Animation");
        stopButton.setActionMethod(this, "stopAnimation");
        NButton resetButton = new NButton("Reset", "res/animation-reset.png");
        resetButton.setToolTipText("Reset Animation");
        resetButton.setActionMethod(this, "resetAnimation");
        add(startButton);
        add(stopButton);
        add(resetButton);
    }

    public void updateFrame() {
        if (document.getFrame() != frame) {
            frameNumber.setValue(frame);
            this.frame = document.getFrame();
        }
    }

    public void stateChanged(ChangeEvent changeEvent) {
        frame = (float) frameNumber.getValue();
        document.setFrame(frame);
    }

    public void startAnimation() {
        document.startAnimation();
    }

    public void stopAnimation() {
        document.stopAnimation();
    }

    public void resetAnimation() {
        document.resetAnimation();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(animationBackground, 0, 0, getWidth(), ANIMATION_BAR_HEIGHT, null);
    }

}
