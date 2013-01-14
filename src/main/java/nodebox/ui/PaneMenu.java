package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;


public class PaneMenu extends JComponent implements MouseListener {

    private static Image paneMenuLeft, paneMenuBackground, paneMenuRight;

    static {
        try {
            paneMenuLeft = ImageIO.read(PaneMenu.class.getResourceAsStream("/pane-menu-left.png"));
            paneMenuBackground = ImageIO.read(PaneMenu.class.getResourceAsStream("/pane-menu-background.png"));
            paneMenuRight = ImageIO.read(PaneMenu.class.getResourceAsStream("/pane-menu-right.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The list of event listeners for this component.
     */
    protected EventListenerList listenerList = new EventListenerList();

    public PaneMenu() {
        Dimension d = new Dimension(103, 21);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
        setEnabled(true);
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

    /**
     * Adds a ChangeListener to the slider.
     *
     * @param l the ChangeListener to add
     * @see #fireActionEvent
     * @see #removeActionListener
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes a ChangeListener from the slider.
     *
     * @param l the ChangeListener to remove
     * @see #fireActionEvent
     * @see #addActionListener
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Send a ChangeEvent, whose source is this Slider, to
     * each listener.  This method method is called each time
     * a ChangeEvent is received from the model.
     *
     * @param menuKey The menu item key that was selected.
     * @see #addActionListener
     * @see javax.swing.event.EventListenerList
     */
    protected void fireActionEvent(String menuKey) {
        ActionEvent actionEvent = new ActionEvent(this, 0, menuKey);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(actionEvent);
            }
        }
    }

}
