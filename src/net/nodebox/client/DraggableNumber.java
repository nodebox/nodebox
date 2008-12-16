package net.nodebox.client;

import net.nodebox.Icons;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

/**
 * DraggableNumber represents a number that can be edited in a variety of interesting ways:
 * by dragging, selecting the arrow buttons, or double-clicking to do direct input.
 */
public class DraggableNumber extends JComponent {

    // todo: could use something like BoundedRangeModel (but then for floats) for checking bounds.

    private JPanel content;
    private JLabel decNumber;
    private JLabel incNumber;
    private JLabel numberLabel;
    private JTextField numberField;
    private Dragger dragger;
    private double value;

    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;

    private static NumberFormat numberFormat;

    static {
        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
    }


    public DraggableNumber() {
        setLayout(new BorderLayout());
        //setBorder(BorderFactory.createLineBorder(new Color(109,108,119), 2));
        //setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setBackground(new Color(20, 20, 20));
        content = new ContentPanel();

        decNumber = new JLabel(new Icons.ArrowIcon(Icons.ArrowIcon.WEST, Color.LIGHT_GRAY));
        decNumber.setPreferredSize(new Dimension(10, 10));
        decNumber.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
                    setValue(getValue() - 10);
                } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
                    setValue(getValue() - 0.01F);
                } else {
                    setValue(getValue() - 1);
                }
                fireActionPerformed();
            }
        });
        decNumber.setBorder(null);
        //decNumber.setFocusPainted(false);
        decNumber.setOpaque(false);
        decNumber.setBackground(Color.WHITE);

        incNumber = new JLabel(new Icons.ArrowIcon(Icons.ArrowIcon.EAST, Color.LIGHT_GRAY));
        incNumber.setPreferredSize(new Dimension(10, 10));
        incNumber.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
                    setValue(getValue() + 10);
                } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
                    setValue(getValue() + 0.01F);
                } else {
                    setValue(getValue() + 1);
                }
                fireActionPerformed();
            }
        });
        incNumber.setBorder(null);
        //incNumber.setFocusPainted(false);
        incNumber.setOpaque(false);
        incNumber.setBackground(Color.WHITE);

        numberField = new JTextField();
        numberField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        //numberField.setOpaque(false);
        numberField.setForeground(Color.WHITE);
        numberField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setValue(Double.parseDouble(numberField.getText()));
                    content.remove(numberField);
                    content.add(numberLabel, BorderLayout.CENTER);
                    validate();
                } catch (NumberFormatException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
                fireActionPerformed();
            }
        });
        numberField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                content.remove(numberField);
                content.add(numberLabel, BorderLayout.CENTER);
                repaint();
            }
        });

        numberLabel = new JLabel();
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //numberLabel.setBorder(null);
        //numberLabel.setOpaque(false);
        numberLabel.setForeground(new Color(228, 228, 228));
        //numberLabel.setBackground(Color.DARK_GRAY);
        dragger = new Dragger();
        numberLabel.addMouseListener(dragger);
        numberLabel.addMouseMotionListener(dragger);

        content.add(decNumber, BorderLayout.WEST);
        content.add(incNumber, BorderLayout.EAST);
        content.add(numberLabel, BorderLayout.CENTER);
        add(content);

        setValue(0);

    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        String formattedNumber = numberFormat.format(getValue());
        numberLabel.setText(formattedNumber);
        //numberField.setText(formattedNumber);
    }

    public class Dragger implements MouseListener, MouseMotionListener {
        int sourceX, sourceY;
        int prevX, prevY;
        double oldValue;

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() >= 2) {
                content.remove(numberLabel);
                content.add(numberField, BorderLayout.CENTER);
                getParent().doLayout();
                // invalidate();
                numberField.setText(numberLabel.getText());
                numberField.requestFocus();
                numberField.selectAll();
                repaint();
            }
        }

        public void mousePressed(MouseEvent e) {
            sourceX = prevX = e.getX();
            sourceX = prevY = e.getY();
            oldValue = getValue();
        }

        public void mouseReleased(MouseEvent e) {
            if (oldValue != value)
                fireActionPerformed();
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            float dX = e.getX() - prevX;

            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
                dX *= 10;
            } else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
                dX *= 0.01;
            }

            setValue(getValue() + dX);
            prevX = e.getX();
            fireStateChanged();
        }

        public void mouseMoved(MouseEvent e) {
        }
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

    private class ContentPanel extends JPanel {
        public ContentPanel() {
            super(new BorderLayout());
            setBackground(new Color(88, 87, 96));
            // setBorder(new Borders.RoundedBorder());
            //setBorder(new Borders.RoundedBorder());
            setBackground(new Color(88, 87, 96));
        }

//        public void paintComponent(Graphics g) {
//            Graphics2D g2 = (Graphics2D) g;
//            Rectangle r = g2.getClipBounds();
//            Color top = new Color(220, 225, 200);
//            Color mid1 = new Color(255, 255, 255);
//            Color mid2 = new Color(200, 200, 200);
//            Color bot = new Color(220, 220, 200);
//            top = new Color(88,87,96);
//            mid1 = new Color(109,108,119);
//            mid2 = new Color(81,80,88);
//            bot = new Color(73,74,80);
//
//            //g2.fill(r);
//
//            g2.setPaint(new GradientPaint(r.x, r.y, top, r.x, r.y + r.height / 2, mid1));
//            g2.fill(new Rectangle(r.x, r.y, r.width, r.height / 2));
//            g2.setPaint(new GradientPaint(r.x, r.y, mid2, r.x, r.y + r.height / 2, bot));
//            g2.fill(new Rectangle(r.x, r.y + r.height / 2, r.width, r.height / 2));
//        }
    }
}
