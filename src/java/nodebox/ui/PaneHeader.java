package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PaneHeader extends JPanel {

    public static Image paneHeaderBackground, paneHeaderOptions;

    static {
        try {
            paneHeaderBackground = ImageIO.read(new File("res/pane-header-background.png"));
            paneHeaderOptions = ImageIO.read(new File("res/pane-header-options.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PaneHeader withName(Pane pane) {
        return new PaneHeader(pane, true);
    }

    public static PaneHeader withoutName(Pane pane) {
        return new PaneHeader(pane, false);
    }

    public PaneHeader(Pane pane) {
        this(pane, true);
    }

    public PaneHeader(Pane pane, boolean addNameLabel) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 2));
        setPreferredSize(new Dimension(9999, 25));
        setMinimumSize(new Dimension(10, 25));
        setMaximumSize(new Dimension(9999, 25));
        if (addNameLabel) {
            add(new PaneNameLabel(pane.getPaneName()));
            add(new Divider());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(paneHeaderBackground, 0, 0, getWidth(), 25, null);
    }

    private final class PaneNameLabel extends JLabel {

        private PaneNameLabel(String label) {
            super(label);
            Dimension d = new Dimension(103, 21);
            setMinimumSize(d);
            setMaximumSize(d);
            setPreferredSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setFont(Theme.SMALL_BOLD_FONT);
            g.setColor(Theme.TEXT_NORMAL_COLOR);
            SwingUtils.drawShadowText((Graphics2D) g, getText(), 5, 14);
        }
    }
}
