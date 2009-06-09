package nodebox.client;

import nodebox.graphics.Color;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventListener;

public class ColorWell extends JComponent {

    private Color color = new Color();
    private transient ClickHandler clickHandler = new ClickHandler();
    private transient ColorPicker colorPicker = new ColorPicker();

    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;

    public ColorWell() {
        setPreferredSize(new Dimension(40, 30));
        addMouseListener(clickHandler);
        addMouseMotionListener(clickHandler);
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(color.getAwtColor());
        Rectangle r = g.getClipBounds();
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color.equals(this.color)) return;
        this.color = color;
        fireStateChanged();
        repaint();
    }

    /**
     * Adds a ChangeListener to the slider.
     *
     * @param l the ChangeListener to add
     * @see #fireStateChanged
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Removes a ChangeListener from the slider.
     *
     * @param l the ChangeListener to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this JSlider with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[]) listenerList.getListeners(
                ChangeListener.class);
    }

    protected void fireStateChanged() {
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        for (EventListener l : listenerList.getListeners(ChangeListener.class)) {
            ((ChangeListener) l).stateChanged(changeEvent);
        }
    }

    private class ClickHandler implements MouseListener, MouseMotionListener {

        private boolean isDragging;

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if (isDragging) {
                colorPicker.setVisible(false);
            }
            isDragging = false;
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            isDragging = true;
            JComponent c = ColorWell.this;
            Point pt = e.getPoint();
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(c);
            pt = SwingUtilities.convertPoint(c, pt, frame.getContentPane());
            MouseEvent newEvent = new MouseEvent(colorPicker, e.getID(), e.getWhen(), e.getModifiers(), (int) pt.getX(), (int) pt.getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
            colorPicker.dispatchEvent(newEvent);
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            isDragging = false;
            JComponent c = ColorWell.this;
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(c);
            if (frame == null) return;
            Point pt = c.getLocation();
            pt = SwingUtilities.convertPoint(c, pt, frame.getContentPane());
            Rectangle r = c.getBounds();
            frame.setGlassPane(colorPicker);
            colorPicker.setVisible(true);
            colorPicker.setPoint(new Point(pt.x, pt.y + r.height));
            frame.validate();
        }
    }

    private java.awt.Color colorFromPoint(int x, int y) {
        float h = x / 255.0F;
        float s = 1.0F;
        float b = 1.0F - (y / 100.0F);
        return java.awt.Color.getHSBColor(h, s, b);
    }

    private class ColorPicker extends JComponent implements MouseListener, MouseMotionListener {

        public Point point;
        public Point dragPoint;
        public Rectangle pickerRect;

        private ColorPicker() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public void setPoint(Point point) {
            this.point = point;
            pickerRect = new Rectangle(point.x, point.y, 255, 100);
        }

        public void setDragPoint(Point dragPoint) {
            this.dragPoint = dragPoint;
            repaint();
            int x = dragPoint.x - point.x;
            int y = dragPoint.y - point.y;
            setColor(new Color(colorFromPoint(x, y)));
        }

        @Override
        public void paint(Graphics g) {
            if (point == null) return;
            //Graphics2D g2 = (Graphics2D) g;
            Rectangle r = g.getClipBounds();
            //g2.setColor(new java.awt.Color(30, 30, 30, 100));
            //g2.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(new java.awt.Color(200, 200, 200, 220));
            g.fillRect(pickerRect.x, pickerRect.y, pickerRect.width, pickerRect.height);

            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 255; x++) {
                    g.setColor(colorFromPoint(x, y));
                    g.fillRect(pickerRect.x + x, pickerRect.y + y, 1, 1);
                }
            }
            if (dragPoint == null) return;
            if (!pickerRect.contains(dragPoint)) return;
        }


        public void mouseClicked(MouseEvent e) {
            if (pickerRect.contains(e.getPoint())) {
                setDragPoint(e.getPoint());
            }
            setVisible(false);
        }

        public void mouseDragged(MouseEvent e) {
            setDragPoint(e.getPoint());
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            setVisible(false);
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
        }

    }
}
