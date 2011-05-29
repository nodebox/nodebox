package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;


public class PaneMenu extends JComponent implements MouseListener {
    private static Image paneMenuLeft, paneMenuBackground, paneMenuRight;

    static {
        try {
            paneMenuLeft = ImageIO.read(new File("res/pane-menu-left.png"));
            paneMenuBackground = ImageIO.read(new File("res/pane-menu-background.png"));
            paneMenuRight = ImageIO.read(new File("res/pane-menu-right.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane pane;

    public PaneMenu(Pane pane) {
        this.pane = pane;
        Dimension d = new Dimension(103, 21);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
        setEnabled(true);
    }

    public Pane getPane() {
        return pane;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            addMouseListener(this);
        } else {
            removeMouseListener(this);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Full width minus left side and right side
        int contentWidth = getWidth() - 9 - 21;
        if (isEnabled()) {
            g.drawImage(paneMenuLeft, 0, 0, null);
            g.drawImage(paneMenuBackground, 9, 0, contentWidth, 21, null);
            g.drawImage(paneMenuRight, 9 + contentWidth, 0, null);
        }

        g2.setFont(Theme.SMALL_BOLD_FONT);
        g2.setColor(Theme.TEXT_NORMAL_COLOR);
        int textPosition = isEnabled() ? 9 : 5;
        SwingUtils.drawShadowText(g2, getMenuName(), textPosition, 14);
    }

    public String getMenuName() {
        return "";
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

}
