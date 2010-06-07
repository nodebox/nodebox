package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

/**
 * DraggableNumber represents a number that can be edited in a variety of interesting ways:
 * by dragging, selecting the arrow buttons, or double-clicking to do direct input.
 */
public class DraggableNumber extends JComponent implements MouseListener, MouseMotionListener, ComponentListener {

    private static Image draggerLeft, draggerRight, draggerBackground;
    private static int draggerLeftWidth, draggerRightWidth, draggerHeight;

    static {
        try {
            draggerLeft = ImageIO.read(new File("res/dragger-left.png"));
            draggerRight = ImageIO.read(new File("res/dragger-right.png"));
            draggerBackground = ImageIO.read(new File("res/dragger-background.png"));
            draggerLeftWidth = draggerLeft.getWidth(null);
            draggerRightWidth = draggerRight.getWidth(null);
            draggerHeight = draggerBackground.getHeight(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // todo: could use something like BoundedRangeModel (but then for floats) for checking bounds.

    private JTextField numberField;
    private double oldValue, value;
    private int previousX;

    private Double minimumValue;
    private Double maximumValue;

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
        Dimension d = new Dimension(87, 20);
        setPreferredSize(d);

        numberField = new JTextField();
        numberField.putClientProperty("JComponent.sizeVariant", "small");
        numberField.setFont(Theme.SMALL_BOLD_FONT);
        numberField.setHorizontalAlignment(JTextField.CENTER);
        numberField.setVisible(false);
        numberField.addKeyListener(new EscapeListener());
        numberField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitNumberField();
            }
        });
        numberField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (numberField.isVisible())
                    commitNumberField();
            }
        });
        add(numberField);

        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        setValue(0);
        // Set the correct size for the numberField.
        componentResized(null);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) cancelNumberField();
    }

    //// Value ranges ////

    public Double getMinimumValue() {
        return minimumValue;
    }

    public boolean hasMinimumValue() {
        return minimumValue == null;
    }

    public void setMinimumValue(double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public void clearMinimumValue() {
        this.minimumValue = null;
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public boolean hasMaximumValue() {
        return maximumValue == null;
    }

    public void setMaximumValue(double maximumValue) {
        this.maximumValue = maximumValue;
    }

    public void clearMaximumValue() {
        this.maximumValue = null;
    }

    //// Value ////

    public double getValue() {
        return value;
    }

    public double clampValue(double value) {
        if (minimumValue != null && value < minimumValue)
            value = minimumValue;
        if (maximumValue != null && value > maximumValue)
            value = maximumValue;
        return value;
    }

    public void setValue(double value) {
        this.value = clampValue(value);
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

    private void cancelNumberField() {
        numberField.setVisible(false);
    }

    //// Component paint ////

    private Rectangle getLeftButtonRect(Rectangle r) {
        if (r == null)
            r = getBounds();
        return new Rectangle(0, 0, draggerLeftWidth, draggerHeight);
    }

    private Rectangle getRightButtonRect(Rectangle r) {
        if (r == null)
            r = getBounds();
        return new Rectangle(r.width - draggerRightWidth, r.y, draggerRightWidth, draggerHeight);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle r = getBounds();
        int centerWidth = r.width - draggerLeftWidth - draggerRightWidth;
        g2.drawImage(draggerLeft, 0, 0, null);
        g2.drawImage(draggerRight, r.width - draggerRightWidth, 0, null);
        g2.drawImage(draggerBackground, draggerLeftWidth, 0, centerWidth, draggerHeight, null);
        g2.setFont(Theme.SMALL_BOLD_FONT);
        if (isEnabled()) {
            g2.setColor(Theme.TEXT_NORMAL_COLOR);
        } else {
            g2.setColor(Theme.TEXT_DISABLED_COLOR);
        }
        SwingUtils.drawCenteredShadowText(g2, valueAsString(), r.width / 2, 14, Theme.DRAGGABLE_NUMBER_HIGLIGHT_COLOR);
    }

    //// Component size ////

    @Override
    public Dimension getPreferredSize() {
        // The control is actually 20 pixels high, but setting the height to 30 will leave a nice margin.
        return new Dimension(120, 30);
    }

    //// Component listeners

    public void componentResized(ComponentEvent e) {
        numberField.setBounds(draggerLeftWidth, 1, getWidth() - draggerLeftWidth - draggerRightWidth, draggerHeight - 2);
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    //// Mouse listeners ////

    public void mousePressed(MouseEvent e) {
        if (!isEnabled()) return;
        if (e.getButton() == MouseEvent.BUTTON1) {
            oldValue = getValue();
            previousX = e.getX();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (!isEnabled()) return;
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
        if (!isEnabled()) return;
        if (oldValue != value)
            fireStateChanged();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (!isEnabled()) return;
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
