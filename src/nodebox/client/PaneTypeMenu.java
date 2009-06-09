package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
    private PaneTypePopup paneTypePopup;

    public PaneTypeMenu(Pane pane) {
        this.pane = pane;
        Dimension d = new Dimension(95, 21);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
        addMouseListener(this);
        paneTypePopup = new PaneTypePopup();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Full width minus left side and right side
        int contentWidth = getWidth() - 9 - 21;
        g.drawImage(paneMenuLeft, 0, 0, null);
        g.drawImage(paneMenuBackground, 9, 0, contentWidth, 21, null);
        g.drawImage(paneMenuRight, 9 + contentWidth, 0, null);

        g2.setFont(SwingUtils.FONT_BOLD);
        g2.setColor(SwingUtils.COLOR_NORMAL);
        SwingUtils.drawShadowText(g2, pane.getPaneName(), 9, 14);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        Rectangle bounds = getBounds();
        paneTypePopup.show(this, bounds.x, bounds.y + bounds.height - 4);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private class PaneTypePopup extends JPopupMenu {
        public PaneTypePopup() {
            add(new ChangePaneTypeAction("Network", NetworkPane.class));
            add(new ChangePaneTypeAction("Parameters", ParameterPane.class));
            add(new ChangePaneTypeAction("Viewer", ViewerPane.class));
            add(new ChangePaneTypeAction("Source", EditorPane.class));
            add(new ChangePaneTypeAction("Console", ConsolePane.class));
            add(new ChangePaneTypeAction("Log", LoggingPane.class));
        }
    }

    private class ChangePaneTypeAction extends AbstractAction {

        private Class paneType;

        private ChangePaneTypeAction(String name, Class paneType) {
            super(name);
            this.paneType = paneType;
        }

        public void actionPerformed(ActionEvent e) {
            pane.changePaneType(paneType);
        }
    }


}
