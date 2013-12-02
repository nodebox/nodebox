package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * DraggableNumber represents a number that can be edited in a variety of interesting ways:
 * by dragging, selecting the arrow buttons, or double-clicking to do direct input.
 */
public class DraggableNumber extends JComponent implements MouseListener, MouseMotionListener, ComponentListener, FocusListener {

    private static Image draggerLeft, draggerRight, draggerBackground;
    private static int draggerLeftWidth, draggerRightWidth, draggerHeight;
    private static Cursor dragCursor;

    static {
        Image dragCursorImage;
        try {
            draggerLeft = ImageIO.read(DraggableNumber.class.getResourceAsStream("/dragger-left.png"));
            draggerRight = ImageIO.read(DraggableNumber.class.getResourceAsStream("/dragger-right.png"));
            draggerBackground = ImageIO.read(DraggableNumber.class.getResourceAsStream("/dragger-background.png"));
            draggerLeftWidth = draggerLeft.getWidth(null);
            draggerRightWidth = draggerRight.getWidth(null);
            draggerHeight = draggerBackground.getHeight(null);
            dragCursorImage = ImageIO.read(DraggableNumber.class.getResourceAsStream("/dragger-cursor.png"));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            dragCursor = toolkit.createCustomCursor(dragCursorImage, new Point(16, 17), "DragCursor");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // todo: could use something like BoundedRangeModel (but then for floats) for checking bounds.
    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;
    private JTextField numberField;
    private double oldValue, value;
    private int previousX;
    private Double minimumValue;
    private Double maximumValue;
    private NumberFormat numberFormat;
    private boolean isDragging = false;

    public DraggableNumber() {
        setLayout(null);
        setCursor(dragCursor);
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        setFocusable(true);
        addFocusListener(this);
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
                breakFocusCycle();
                commitNumberField();
            }
        });
        numberField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (numberField.isVisible())
                    commitNumberField();
                setFocusable(true);
            }
        });
        add(numberField);

        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        setValue(0);
        // Set the correct size for the numberField.
        componentResized(null);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new DraggableNumber());
        frame.pack();
        frame.setVisible(true);
    }

    //// Value ranges ////

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) cancelNumberField();
    }

    public Double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(Double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public boolean hasMinimumValue() {
        return minimumValue == null;
    }

    public void clearMinimumValue() {
        this.minimumValue = null;
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(Double maximumValue) {
        this.maximumValue = maximumValue;
    }

    public boolean hasMaximumValue() {
        return maximumValue == null;
    }

    //// Value ////

    public void clearMaximumValue() {
        this.maximumValue = null;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = clampValue(value);
        repaint();
    }

    public double clampValue(double value) {
        if (minimumValue != null && value < minimumValue)
            value = minimumValue;
        if (maximumValue != null && value > maximumValue)
            value = maximumValue;
        return value;
    }

    public void setValueFromString(String s) throws NumberFormatException {
        setValue(Double.parseDouble(s));
    }

    //// Number formatting ////

    public String valueAsString() {
        return numberFormat.format(value);
    }

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

    private void cancelNumberField() {
        numberField.setVisible(false);
    }

    private Rectangle getLeftButtonRect() {
        return new Rectangle(0, 0, draggerLeftWidth, draggerHeight);
    }

    private Rectangle getRightButtonRect() {
        Rectangle r = getBounds();
        return new Rectangle(r.width - draggerRightWidth, r.y, draggerRightWidth, draggerHeight);
    }

    private boolean inDraggableArea(Point pt) {
        return !getLeftButtonRect().contains(pt) && !getRightButtonRect().contains(pt);
    }

    public void focusGained(FocusEvent e) {
        showNumberField();
        setFocusable(false);
    }

    public void focusLost(FocusEvent e) {
    }

    // We want to move focus to a sibling focusable control using TAB only, not by hitting
    // Enter or Escape. In these cases we need to break out of the current focus cycle.
    private void breakFocusCycle() {
        Container o = getParent();
        while (o != null) {
            if (o != null && o.isFocusable())
                break;
            o = o.getParent();
        }
        if (o != null)
            o.requestFocus();
    }

    //// Component size ////

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
        SwingUtils.drawCenteredShadowText(g2, valueAsString(), r.width / 2, 14, Theme.DRAGGABLE_NUMBER_HIGHLIGHT_COLOR);
    }

    //// Component listeners

    @Override
    public Dimension getPreferredSize() {
        // The control is actually 20 pixels high, but setting the height to 30 will leave a nice margin.
        return new Dimension(120, 30);
    }

    public void componentResized(ComponentEvent e) {
        numberField.setBounds(draggerLeftWidth, 1, getWidth() - draggerLeftWidth - draggerRightWidth, draggerHeight - 2);
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    //// Mouse listeners ////

    public void componentHidden(ComponentEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (!isEnabled()) return;
        if (!inDraggableArea(e.getPoint())) return;
        if (e.getButton() == MouseEvent.BUTTON1) {
            isDragging = true;
            oldValue = getValue();
            previousX = e.getX();
            SwingUtilities.getRootPane(this).setCursor(dragCursor);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (!isEnabled()) return;
        double dx = 1.0F;
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
            dx = 10F;
        } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
            dx = 0.01F;
        }
        if (getLeftButtonRect().contains(e.getPoint())) {
            setValue(getValue() - dx);
            fireStateChanged();
        } else if (getRightButtonRect().contains(e.getPoint())) {
            setValue(getValue() + dx);
            fireStateChanged();
        } else if (e.getClickCount() >= 2) {
            showNumberField();
        }
    }

    private void showNumberField() {
        numberField.setText(valueAsString());
        numberField.setVisible(true);
        numberField.requestFocus();
        numberField.selectAll();
        componentResized(null);
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        if (!isEnabled()) return;
        isDragging = false;
        SwingUtilities.getRootPane(this).setCursor(Cursor.getDefaultCursor());
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
        if (!isDragging) return;
        double deltaX = e.getX() - previousX;
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

    /**
     * When the escape key is pressed in the numberField, ignore the change and "close" the field.
     */
    private class EscapeListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                breakFocusCycle();
                numberField.setVisible(false);
            }
        }
    }
}
