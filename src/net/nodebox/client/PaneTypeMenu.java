package net.nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

public class PaneTypeMenu extends JComponent implements MouseListener {

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

    public PaneTypeMenu(Pane pane) {
        this.pane = pane;
        Dimension d = new Dimension(95, 21);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Full width minus left side and right side
        int contentWidth = getWidth() - 9 - 21;
        g.drawImage(paneMenuLeft, 0, 0, null);
        g.drawImage(paneMenuBackground, 9, 0, contentWidth, 21, null);
        g.drawImage(paneMenuRight, 9 + contentWidth, 0, null);

        g2.setFont(SwingUtils.boldFont);
        g2.setColor(SwingUtils.normalColor);
        SwingUtils.drawShadowText(g2, pane.getPaneName(), 9, 14);
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
