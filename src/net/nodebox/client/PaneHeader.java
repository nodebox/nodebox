package net.nodebox.client;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PaneHeader extends JPanel implements MouseListener {

    private Pane pane;
    private PaneMenu paneMenu;

    private class PaneHeaderBorder implements Border {
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Theme.getInstance().getBorderColor());
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }


    public PaneHeader(Pane pane) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 0));
        setPreferredSize(new Dimension(100, 22));
        setBackground(Theme.getInstance().getBackgroundColor());
        this.pane = pane;
        paneMenu = new PaneMenu(this.pane);
        setBorder(new PaneHeaderBorder());
        addMouseListener(this);
    }

    public PaneMenu getPaneMenu() {
        return paneMenu;
    }

    public Pane getPane() {
        return pane;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getX() < this.getWidth() - 20) return;
        paneMenu.show(this, e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Theme.getInstance().getArrowIcon().paintIcon(this, g, getWidth() - 15, 8);
    }
}
