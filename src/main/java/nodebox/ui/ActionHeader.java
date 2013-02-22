package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ActionHeader extends JPanel {

    public static final Image backgroundImage;
    public static final Image dividerImage;

    static {
        try {
            backgroundImage = ImageIO.read(ActionHeader.class.getResourceAsStream("/action-gradient.png"));
            dividerImage = ImageIO.read(ActionHeader.class.getResourceAsStream("/action-divider.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setFont(Theme.SMALL_BOLD_FONT);

        g2.drawImage(backgroundImage, 0, 0, getWidth(), 25, null);
    }

    public void addDivider() {
        add(new Divider());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(1, 25);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1, 25);
    }


    public static class Divider extends JComponent {
        private Divider() {
            Dimension d = new Dimension(1, 24);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(dividerImage, 0, 0, null);
        }
    }

}