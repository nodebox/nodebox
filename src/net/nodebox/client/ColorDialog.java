package net.nodebox.client;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ColorDialog extends JDialog implements ChangeListener {

    private Color color;
    private ColorField colorField;
    private DraggableNumber hueDraggable, saturationDraggable, brightnessDraggable;
    private DraggableNumber redDraggable, blueDraggable, greenDraggable;
    private DraggableNumber alphaDraggable;
    private ColorSlider hueSlider, saturationSlider, brightnessSlider;
    private ColorSlider redSlider, blueSlider, greenSlider;
    private ColorSlider alphaSlider;
    private boolean changeDisabled = false;

    private float hue, saturation, brightness, red, green, blue, alpha;

    private static final int MAX_RANGE = 5000;

    /**
     * Only one <code>ChangeEvent</code> is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this". The event is lazily
     * created the first time that an event notification is fired.
     *
     * @see #fireStateChanged
     */
    protected transient ChangeEvent changeEvent = null;

    /**
     * A list of event listeners for this component.
     */
    protected EventListenerList listenerList = new EventListenerList();

    public ColorDialog(Frame owner) {
        super(owner, "Choose Color");
        getRootPane().putClientProperty("Window.style", "small");
        setMinimumSize(new Dimension(300, 340));
        colorField = new ColorField();
        colorField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        Container contents = getContentPane();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));

        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        fieldPanel.add(colorField);
        contents.add(fieldPanel, BorderLayout.NORTH);

        JPanel sliderPanel = new JPanel(new GridBagLayout());
        hueDraggable = new DraggableNumber();
        setRangeForDraggable(hueDraggable);
        hueDraggable.addChangeListener(this);
        saturationDraggable = new DraggableNumber();
        setRangeForDraggable(saturationDraggable);
        saturationDraggable.addChangeListener(this);
        brightnessDraggable = new DraggableNumber();
        setRangeForDraggable(brightnessDraggable);
        brightnessDraggable.addChangeListener(this);
        redDraggable = new DraggableNumber();
        setRangeForDraggable(redDraggable);
        redDraggable.addChangeListener(this);
        blueDraggable = new DraggableNumber();
        setRangeForDraggable(blueDraggable);
        blueDraggable.addChangeListener(this);
        greenDraggable = new DraggableNumber();
        setRangeForDraggable(greenDraggable);
        greenDraggable.addChangeListener(this);
        alphaDraggable = new DraggableNumber();
        setRangeForDraggable(alphaDraggable);
        alphaDraggable.addChangeListener(this);
        hueSlider = new ColorSlider();
        saturationSlider = new ColorSlider();
        brightnessSlider = new ColorSlider();
        redSlider = new ColorSlider();
        blueSlider = new ColorSlider();
        greenSlider = new ColorSlider();
        alphaSlider = new ColorSlider();

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.ipadx = 10;

        GridBagConstraints draggableConstraints = new GridBagConstraints();
        draggableConstraints.gridx = 1;
        draggableConstraints.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints sliderConstraints = new GridBagConstraints();
        sliderConstraints.gridx = 2;
        sliderConstraints.fill = GridBagConstraints.HORIZONTAL;
        sliderConstraints.weightx = 1.0;

        GridBagConstraints spacerConstraints = new GridBagConstraints();
        spacerConstraints.gridx = 0;
        spacerConstraints.gridwidth = GridBagConstraints.REMAINDER;

        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 0;
        sliderPanel.add(new JLabel("Hue"), labelConstraints);
        sliderPanel.add(hueDraggable, draggableConstraints);
        sliderPanel.add(hueSlider, sliderConstraints);
        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 1;
        sliderPanel.add(new JLabel("Saturation"), labelConstraints);
        sliderPanel.add(saturationDraggable, draggableConstraints);
        sliderPanel.add(saturationSlider, sliderConstraints);
        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 2;
        sliderPanel.add(new JLabel("Brightness"), labelConstraints);
        sliderPanel.add(brightnessDraggable, draggableConstraints);
        sliderPanel.add(brightnessSlider, sliderConstraints);

        spacerConstraints.gridy = 3;
        sliderPanel.add(new JLabel(" "), spacerConstraints);

        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 4;
        sliderPanel.add(new JLabel("Red"), labelConstraints);
        sliderPanel.add(redDraggable, draggableConstraints);
        sliderPanel.add(redSlider, sliderConstraints);
        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 5;
        sliderPanel.add(new JLabel("Green"), labelConstraints);
        sliderPanel.add(greenDraggable, draggableConstraints);
        sliderPanel.add(greenSlider, sliderConstraints);
        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 6;
        sliderPanel.add(new JLabel("Blue"), labelConstraints);
        sliderPanel.add(blueDraggable, draggableConstraints);
        sliderPanel.add(blueSlider, sliderConstraints);

        spacerConstraints.gridy = 7;
        sliderPanel.add(new JLabel(" "), spacerConstraints);

        labelConstraints.gridy = draggableConstraints.gridy = sliderConstraints.gridy = 8;
        sliderPanel.add(new JLabel("Alpha"), labelConstraints);
        sliderPanel.add(alphaDraggable, draggableConstraints);
        sliderPanel.add(alphaSlider, sliderConstraints);

        contents.add(sliderPanel);

        Dimension fillDimension = new Dimension(0, Integer.MAX_VALUE);
        contents.add(new Box.Filler(fillDimension, fillDimension, fillDimension));

        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, escapeStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setColor(Color.WHITE);
    }

    public static void main(String[] args) {
        ColorDialog cd = new ColorDialog(null);
        cd.setSize(500, 340);
        cd.setLocationByPlatform(true);
        cd.setAlwaysOnTop(true);
        cd.setVisible(true);
    }

    private void setRangeForDraggable(DraggableNumber draggable) {
        draggable.setMinimumValue(0);
        draggable.setMaximumValue(1);
    }

    private float clamp(float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    public void stateChanged(ChangeEvent e) {
        if (changeDisabled) return;
        if (e.getSource() == hueSlider) {
            float v = clamp(hueSlider.getValue() / (float) MAX_RANGE);
            setHSB(v, saturation, brightness);
            hueDraggable.setValue(v);
        } else if (e.getSource() == saturationSlider) {
            float v = clamp(saturationSlider.getValue() / (float) MAX_RANGE);
            setHSB(hue, v, brightness);
            saturationDraggable.setValue(v);
        } else if (e.getSource() == brightnessSlider) {
            float v = clamp(brightnessSlider.getValue() / (float) MAX_RANGE);
            setHSB(hue, saturation, v);
            brightnessDraggable.setValue(v);
        } else if (e.getSource() == redSlider) {
            float v = clamp(redSlider.getValue() / (float) MAX_RANGE);
            setRGB(v, green, blue);
            redDraggable.setValue(v);
        } else if (e.getSource() == greenSlider) {
            float v = clamp(greenSlider.getValue() / (float) MAX_RANGE);
            setRGB(red, v, blue);
            greenDraggable.setValue(v);
        } else if (e.getSource() == blueSlider) {
            float v = clamp(blueSlider.getValue() / (float) MAX_RANGE);
            setRGB(red, green, v);
            blueDraggable.setValue(v);
        } else if (e.getSource() == alphaSlider) {
            float v = clamp(alphaSlider.getValue() / (float) MAX_RANGE);
            setAlpha(v);
            alphaDraggable.setValue(v);
        } else if (e.getSource() == hueDraggable) {
            float v = clamp((float) hueDraggable.getValue());
            setHSB(v, saturation, brightness);
            hueSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == saturationDraggable) {
            float v = clamp((float) saturationDraggable.getValue());
            setHSB(hue, v, brightness);
            saturationSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == brightnessDraggable) {
            float v = clamp((float) brightnessDraggable.getValue());
            setHSB(hue, saturation, v);
            brightnessSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == redDraggable) {
            float v = clamp((float) redDraggable.getValue());
            setRGB(v, green, blue);
            redSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == greenDraggable) {
            float v = clamp((float) greenDraggable.getValue());
            setRGB(red, v, blue);
            redSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == blueDraggable) {
            float v = clamp((float) blueDraggable.getValue());
            setRGB(red, green, v);
            redSlider.setValue((int) (v * MAX_RANGE));
        } else if (e.getSource() == alphaDraggable) {
            float v = clamp((float) alphaDraggable.getValue());
            setAlpha(v);
            alphaSlider.setValue((int) (v * MAX_RANGE));
        }
    }

    public void setColor(Color color) {
        if (this.color != null && color != null && this.color.equals(color)) return;
        this.color = color;
        updateRGB();
        updateHSB();
        updateAlpha();
        fireStateChanged();
    }

    public Color getColor() {
        return color;
    }

    private void setHSB(float h, float s, float b) {
        if (hue == h && saturation == s && brightness == b) return;
        hue = h;
        saturation = s;
        brightness = b;
        Color c = Color.getHSBColor(h, s, b);
        this.color = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255));
        updateRGB();
        colorField.repaint();
        fireStateChanged();
    }

    private void setRGB(float r, float g, float b) {
        if (red == r && green == g && blue == b) return;
        red = r;
        green = g;
        blue = b;
        this.color = new Color(r, g, b, alpha);
        updateHSB();
        colorField.repaint();
        fireStateChanged();
    }

    private void setAlpha(float a) {
        if (alpha == a) return;
        alpha = a;
        this.color = new Color(red, green, blue, a);
        colorField.repaint();
        fireStateChanged();
    }

    private void updateRGB() {
        changeDisabled = true;
        red = color.getRed() / 255F;
        green = color.getGreen() / 255F;
        blue = color.getBlue() / 255F;
        redSlider.setValue((int) (red * MAX_RANGE));
        greenSlider.setValue((int) (green * MAX_RANGE));
        blueSlider.setValue((int) (blue * MAX_RANGE));
        redDraggable.setValue(red);
        greenDraggable.setValue(green);
        blueDraggable.setValue(blue);
        changeDisabled = false;
    }

    private void updateHSB() {
        changeDisabled = true;
        float[] hsb = new float[3];
        Color.RGBtoHSB((int) (red * 255), (int) (green * 255), (int) (blue * 255), hsb);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        hueSlider.setValue((int) (hue * MAX_RANGE));
        saturationSlider.setValue((int) (saturation * MAX_RANGE));
        brightnessSlider.setValue((int) (brightness * MAX_RANGE));
        hueDraggable.setValue(hue);
        saturationDraggable.setValue(saturation);
        brightnessDraggable.setValue(brightness);
        changeDisabled = false;
    }

    private void updateAlpha() {
        changeDisabled = true;
        alpha = color.getAlpha() / 255F;
        alphaSlider.setValue((int) (alpha * MAX_RANGE));
        alphaDraggable.setValue(alpha);
        changeDisabled = false;
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

    public class ColorField extends JButton {

        public ColorField() {
            setMinimumSize(new Dimension(60, 60));
            setPreferredSize(new Dimension(60, 60));
            setSize(60, 60);

        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.DARK_GRAY);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    public class ColorSlider extends JSlider {
        public ColorSlider() {
            super(0, MAX_RANGE, 0);
            addChangeListener(ColorDialog.this);
        }
    }


}
