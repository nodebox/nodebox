package net.nodebox.client;

import net.nodebox.Icons;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

/**
 * DraggableNumber represents a number that can be edited in a variety of interesting ways:
 * by dragging, selecting the arrow buttons, or double-clicking to do direct input.
 */
public class DraggableNumber extends JComponent implements MouseListener, MouseMotionListener, ComponentListener {

    // todo: could use something like BoundedRangeModel (but then for floats) for checking bounds.

    private JTextField numberField;
    private double oldValue, value;
    private Icon leftIcon;
    private Icon rightIcon;
    private int previousX;

    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;

    private NumberFormat numberFormat;

    public DraggableNumber() {
        setLayout(null);
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        leftIcon = new Icons.ArrowIcon(Icons.ArrowIcon.WEST, Color.GRAY);
        rightIcon = new Icons.ArrowIcon(Icons.ArrowIcon.EAST, Color.GRAY);

        numberField = new JTextField();
        numberField.putClientProperty("JComponent.sizeVariant", "small");
        numberField.setFont(PlatformUtils.getSmallBoldFont());
        numberField.setBounds(12, 1, 100, 30);
        numberField.setHorizontalAlignment(JTextField.CENTER);
        numberField.setVisible(false);
        numberField.addKeyListener(new EscapeListener());
        numberField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitNumberField();
            }
        });
        add(numberField);

        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        setValue(0);
    }

    //// Value ////

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        String formattedNumber = numberFormat.format(getValue());
        numberField.setText(formattedNumber);
        repaint();
    }

    public void setValueFromString(String s) throws NumberFormatException {
        setValue(Double.parseDouble(s));
    }

    public String valueAsString() {
        return numberFormat.format(value);
    }

    //// Number formatting ////

    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    public void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
        // Refresh the label
        setValue(getValue());
    }

    private void commitNumberField() {
        numberField.setVisible(false);
        String s = numberField.getText();
        try {
            setValueFromString(s);
            fireStateChanged();
        } catch (NumberFormatException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    //// Component paint ////

    private Rectangle getLeftButtonRect(Rectangle r) {
        if (r == null)
            r = getBounds();
        return new Rectangle(r.x + 2, r.y + 1, 15, r.height - 2);
    }

    private Rectangle getRightButtonRect(Rectangle r) {
        r = getLeftButtonRect(r);
        r.setRect(getWidth() - 15, r.y, 15, r.height);
        return r;
    }

    @Override
    public void paintComponent(Graphics g) {
        paintDraggableNumber(g);
    }

    protected void paintDraggableNumber(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle r = getBounds();
        int radius = r.height / 2;
        int halfradius = (radius / 2) - 2;
        r.setRect(2, 2, r.width - 8, r.height - 8);
        g2.setColor(new Color(215, 215, 215));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, radius, radius);
        g2.setColor(new Color(150, 150, 150));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, radius, radius);
        g2.setColor(new Color(187, 187, 187));
        g2.drawLine(r.x + halfradius, r.y + 1, r.width - halfradius, r.y + 1);
        g2.setColor(new Color(223, 223, 223));
        g2.drawLine(r.x + halfradius, r.y + r.height - 1, r.width - halfradius, r.y + r.height - 1);
        g2.setColor(new Color(0, 0, 0));
        g2.setFont(PlatformUtils.getSmallBoldFont());
        paintCenteredString(g2, valueAsString(), r.x + r.width / 2F, r.y + r.height / 2F);
        // TODO: The "-2" at the end is a hack. 
        leftIcon.paintIcon(this, g, r.x + 2, r.y + r.height / 2 - 2);
        rightIcon.paintIcon(this, g, r.x + r.width - 8, r.y + r.height / 2 - 2);
    }

    private void paintCenteredString(Graphics2D g2, String s, float centerX, float centerY) {
        FontRenderContext frc = g2.getFontRenderContext();
        Rectangle2D bounds = g2.getFont().getStringBounds(s, frc);
        float leftX = centerX - (float) bounds.getWidth() / 2;
        LineMetrics lm = g2.getFont().getLineMetrics(s, frc);
        float baselineY = centerY - lm.getHeight() / 2 + lm.getAscent();
        g2.drawString(s, leftX, baselineY);
    }

    //// Component size ////

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 25);
    }

    //// Component listeners

    public void componentResized(ComponentEvent e) {
        numberField.setBounds(12, 1, getWidth() - 24, getHeight() - 2);
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    //// Mouse listeners ////

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            oldValue = getValue();
            previousX = e.getX();
        }
    }

    public void mouseClicked(MouseEvent e) {
        float dx = 1.0F;
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
            dx = 10F;
        } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
            dx = 0.01F;
        }
        if (getLeftButtonRect(null).contains(e.getPoint())) {
            setValue(getValue() - dx);
            fireStateChanged();
        } else if (getRightButtonRect(null).contains(e.getPoint())) {
            setValue(getValue() + dx);
            fireStateChanged();
        } else if (e.getClickCount() >= 2) {
            numberField.setText(valueAsString());
            numberField.setVisible(true);
            numberField.requestFocus();
            numberField.selectAll();
            componentResized(null);
            repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (oldValue != value)
            fireActionPerformed();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        float deltaX = e.getX() - previousX;
        if (deltaX == 0F) return;
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
            deltaX *= 10;
        } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
            deltaX *= 0.01;
        }
        setValue(getValue() + deltaX);
        previousX = e.getX();
        fireStateChanged();
    }


    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener to be added
     */
    public synchronized void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener to be removed
     */
    public synchronized void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this JTextField with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public synchronized ActionListener[] getActionListeners() {
        return listenerList.getListeners(
                ActionListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created.
     * The listener list is processed in last to
     * first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent) currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent) currentEvent).getModifiers();
        }

        // todo: could use lightweight event here?
        ActionEvent e =
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        Double.toString(value),
                        EventQueue.getMostRecentEventTime(), modifiers);

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
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
     * Send a ChangeEvent, whose source is this Slider, to
     * each listener.  This method method is called each time
     * a ChangeEvent is received from the model.
     *
     * @see #addChangeListener
     * @see javax.swing.event.EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new DraggableNumber());
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * When the escape key is pressed in the numberField, ignore the change and "close" the field.
     */
    private class EscapeListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                numberField.setVisible(false);
        }
    }
}
