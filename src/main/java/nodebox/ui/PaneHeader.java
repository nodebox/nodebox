package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class PaneHeader extends JPanel {

    public static final Image paneHeaderBackground, paneHeaderOptions;

    static {
        try {
            paneHeaderBackground = ImageIO.read(PaneHeader.class.getResourceAsStream("/pane-header-background.png"));
            paneHeaderOptions = ImageIO.read(PaneHeader.class.getResourceAsStream("/pane-header-options.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PaneHeader withTitle(String title) {
        return new PaneHeader(title);
    }

    public static PaneHeader withoutTitle() {
        return new PaneHeader(null);
    }

    private ShadowLabel titleLabel = null;

    public PaneHeader(String title) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 2));
        setPreferredSize(new Dimension(9999, 25));
        setMinimumSize(new Dimension(10, 25));
        setMaximumSize(new Dimension(9999, 25));
        if (title != null) {
            titleLabel = new ShadowLabel(title);
            add(titleLabel);
            add(new Divider());
        }
    }

    public void setTitle(String title) {
        if (titleLabel == null)
            throw new IllegalStateException("This pane has no title.");
        titleLabel.setText(title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(paneHeaderBackground, 0, 0, getWidth(), 25, null);
    }

    private final class ShadowLabel extends JLabel {

        private ShadowLabel(String label) {
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
