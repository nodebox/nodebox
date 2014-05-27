package nodebox.client;

import nodebox.ui.DraggableNumber;
import nodebox.ui.NButton;
import nodebox.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class AnimationBar extends JPanel implements ChangeListener {

    private static final int ANIMATION_BAR_HEIGHT = 27;

    static {
        try {
            animationBackground = ImageIO.read(AnimationBar.class.getResourceAsStream("/animation-background.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Image animationBackground;
    private final NodeBoxDocument document;
    private final DraggableNumber frameNumber;
    private final NButton playButton;
    private double frame;

    public AnimationBar(final NodeBoxDocument document) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 0));
        setPreferredSize(new Dimension(100, ANIMATION_BAR_HEIGHT));
        setMinimumSize(new Dimension(100, ANIMATION_BAR_HEIGHT));
        setMaximumSize(new Dimension(100, ANIMATION_BAR_HEIGHT));

        this.document = document;

        // We use a number of tricks here to make the frame number line up with the buttons.
        // - We use a panel with a flow layout so we can set a border. (Setting it directly on the DraggableNumber does not work)
        // - We use an inset border with a positive top margin (the actual space we want to move) and a negative bottom margin
        //   (to compensate for the effect).
        JPanel frameNumberPanel = new JPanel();
        frameNumberPanel.setOpaque(false);
        frameNumberPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        frameNumberPanel.setBorder(new Theme.InsetsBorder(5, 0, -4, 0));
        frameNumber = new DraggableNumber();
        frameNumber.addChangeListener(this);
        frameNumberPanel.add(frameNumber);
        add(frameNumberPanel);
        playButton = new NButton("Play", getImageStream("/animation-play.png"), getImageStream("/animation-stop.png"));
        playButton.setToolTipText("Play Animation");
        playButton.setActionMethod(this, "playAnimation");
        forcePlayButtonWidth(52);
        NButton rewindButton = new NButton("Rewind", getImageStream("/animation-rewind.png"));
        rewindButton.setToolTipText("Rewind Animation");
        rewindButton.setActionMethod(this, "rewindAnimation");
        add(playButton);
        add(rewindButton);
        setFrame(1);
    }

    private InputStream getImageStream(String name) {
        return getClass().getResourceAsStream(name);
    }

    public void setFrame(double frame) {
        this.frame = frame;
        frameNumber.setValue(frame);
    }

    public void stateChanged(ChangeEvent changeEvent) {
        frame = frameNumber.getValue();
        document.setFrame(frame);
    }

    public void toggleAnimation() {
        if (playButton.isChecked())
            stopAnimation();
        else
            playAnimation();
    }

    public void playAnimation() {
        if (!playButton.isChecked()) playButton.setChecked(true);
        document.playAnimation();
        playButton.setText("Pause");
        playButton.setToolTipText("Pause Animation");
        playButton.setActionMethod(this, "stopAnimation");
        forcePlayButtonWidth(52);
    }

    public void stopAnimation() {
        if (playButton.isChecked()) playButton.setChecked(false);
        document.stopAnimation();
        playButton.setText("Play");
        playButton.setToolTipText("Play Animation");
        playButton.setActionMethod(this, "playAnimation");
        forcePlayButtonWidth(52);
    }

    public void rewindAnimation() {
        stopAnimation();
        document.rewindAnimation();
    }

    private void forcePlayButtonWidth(int width) {
        Dimension d = new Dimension(width, NButton.BUTTON_HEIGHT);
        playButton.setSize(d);
        playButton.setPreferredSize(d);
        playButton.setMinimumSize(d);
        playButton.setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(animationBackground, 0, 0, getWidth(), ANIMATION_BAR_HEIGHT, null);
    }

}


