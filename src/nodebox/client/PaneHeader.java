package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

public class PaneHeader extends JPanel implements MouseListener {

    public static Image paneHeaderBackground, paneHeaderOptions;

    static {
        try {
            paneHeaderBackground = ImageIO.read(new File("res/pane-header-background.png"));
            paneHeaderOptions = ImageIO.read(new File("res/pane-header-options.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane pane;
    private PaneOptionsMenu paneOptionsMenu;

    public PaneHeader(Pane pane) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 2));
        setPreferredSize(new Dimension(100, 25));
        setMinimumSize(new Dimension(100, 25));
        setMaximumSize(new Dimension(100, 25));
        this.pane = pane;
        paneOptionsMenu = new PaneOptionsMenu(this.pane);
        addMouseListener(this);
        add(new PaneTypeMenu(pane));
        add(new Divider());
    }

    public PaneOptionsMenu getPaneMenu() {
        return paneOptionsMenu;
    }

    public Pane getPane() {
        return pane;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getX() < this.getWidth() - 20) return;
        paneOptionsMenu.show(this, e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(paneHeaderBackground, 0, 0, getWidth(), 25, null);
        g.drawImage(paneHeaderOptions, getWidth() - 20, 0, null);

    }
}
