package nodebox.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.prefs.Preferences;

public class ColorDialog extends JDialog implements ChangeListener {
    public static final String COLOR_RANGE = "NBColorRange";

    public enum ColorRange {
        PERCENTAGE, ABSOLUTE
    }

    public enum ColorComponent {
        RED, GREEN, BLUE, ALPHA, HUE, SATURATION, BRIGHTNESS
    }

    private OKAction okAction = new OKAction();
    private CancelAction cancelAction = new CancelAction();
    private Preferences preferences;

    private ColorRangeMenu rangeBox;

    private Color color;
    private Color newColor;
    private ColorField colorField;
    private JTextField hexField;
    private ColorPanel[] panels;
    private ColorRange colorRange;
    private DraggableNumber hueDraggable, saturationDraggable, brightnessDraggable;
    private DraggableNumber redDraggable, blueDraggable, greenDraggable;
    private DraggableNumber alphaDraggable;

    private float hue, saturation, brightness, red, green, blue, alpha;

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
        colorField = new ColorField();

        Dimension d = new Dimension(Integer.MAX_VALUE, 80);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setMinimumSize(d);
        topPanel.setPreferredSize(d);
        topPanel.setMaximumSize(d);
        topPanel.setSize(d);
        topPanel.add(colorField, BorderLayout.WEST);

        JPanel hexPanel = new JPanel(null);
        hexPanel.setMinimumSize(d);
        hexPanel.setPreferredSize(d);
        hexPanel.setMaximumSize(d);
        hexPanel.setSize(d);
        hexField = new JTextField();
        hexField.setFont(Theme.SMALL_BOLD_FONT);
        hexField.setForeground(Theme.TEXT_NORMAL_COLOR);
        hexField.setHorizontalAlignment(JTextField.CENTER);
        hexField.setBackground(null);
        hexPanel.add(hexField);
        hexField.setBounds(0, 10, 110, 22);
        hexField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String s = hexField.getText().toLowerCase(Locale.US);
                if (! s.startsWith("#"))
                    s = "#" + s;
                if (s.length() == 7)
                    s = s + "ff";
                try {
                    nodebox.graphics.Color c = new nodebox.graphics.Color(s);
                    red = (float) c.getRed();
                    green = (float) c.getGreen();
                    blue = (float) c.getBlue();
                    alpha = (float) c.getAlpha();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(ColorDialog.this, ex.getMessage());
                }
                updateHSB();
                updateColor();
            }
        });
        topPanel.add(hexPanel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 7));

        ColorPanel red = new ColorPanel("red");
        ColorPanel green = new ColorPanel("green");
        ColorPanel blue = new ColorPanel("blue");
        ColorPanel alpha = new ColorPanel("alpha");
        ColorPanel hue = new ColorPanel("hue");
        ColorPanel saturation = new ColorPanel("saturation");
        ColorPanel brightness = new ColorPanel("brightness");
        panels = new ColorPanel[]{red, green, blue, alpha, hue, saturation, brightness};

        d = new Dimension(120, Integer.MAX_VALUE);
        rangeBox = new ColorRangeMenu();
        rangeBox.setMinimumSize(d);
        rangeBox.setPreferredSize(d);
        rangeBox.setSize(d);

        JButton cancelButton = new JButton(cancelAction);
        JButton okButton = new JButton(okAction);

        d = new Dimension(Integer.MAX_VALUE, 30);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setMinimumSize(d);
        bottomPanel.setPreferredSize(d);
        bottomPanel.setMaximumSize(d);
        bottomPanel.setSize(d);
        bottomPanel.add(Box.createHorizontalStrut(77));
        bottomPanel.add(rangeBox);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(cancelButton);
        bottomPanel.add(okButton);
        bottomPanel.add(Box.createHorizontalStrut(5));

        getRootPane().setDefaultButton(okButton);

        Container contents = getContentPane();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        contents.add(topPanel);
        contents.add(hue);
        contents.add(saturation);
        contents.add(brightness);
        contents.add(Box.createVerticalStrut(12));
        contents.add(red);
        contents.add(green);
        contents.add(blue);
        contents.add(Box.createVerticalStrut(12));
        contents.add(alpha);
        contents.add(bottomPanel);
        contents.add(Box.createVerticalStrut(5));

        pack();

        setColor(Color.WHITE);

        this.preferences = Preferences.userNodeForPackage(this.getClass());
        setColorRange(ColorRange.valueOf(getPreferences().get(COLOR_RANGE, "ABSOLUTE")));

        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setColor(color);
                dispose();
            }
        }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setColor(newColor);
            }
        });
    }

    private float clamp(float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    public void setColor(Color color) {
        this.color = color;
        this.newColor = color;
        float[] components = color.getComponents(null);
        assert components.length == 4;
        setRGBA(components[0], components[1], components[2], components[3]);
        updatePanels();
        fireStateChanged();
    }

    public Color getColor() {
        return newColor;
    }

    public void setRGB(float r, float g, float b) {
        setRGBA(r, g, b, 1.0F);
    }

    public void setRGBA(float r, float g, float b, float a) {
        if (red == r && green == g && blue == b && alpha == a) return;
        red = clamp(r);
        green = clamp(g);
        blue = clamp(b);
        alpha = clamp(a);
        updateHSB();
    }

    private void updateRGB() {
        if (saturation == 0)
            red = green = blue = brightness;
        else {
            float h = hue;
            if (hue == 1.0)
                h = 0.999998f;
            float s = saturation;
            float v = brightness;
            float r, g, b, f, p, q, t;
            h = h / (float) (60.0 / 360);
            int i = (int) Math.floor(h);
            f = h - i;
            p = v * (1 - s);
            q = v * (1 - s * f);
            t = v * (1 - s * (1 - f));

            float rgb[];
            if (i == 0)
                rgb = new float[]{v, t, p};
            else if (i == 1)
                rgb = new float[]{q, v, p};
            else if (i == 2)
                rgb = new float[]{p, v, t};
            else if (i == 3)
                rgb = new float[]{p, q, v};
            else if (i == 4)
                rgb = new float[]{t, p, v};
            else
                rgb = new float[]{v, p, q};

            red = rgb[0];
            green = rgb[1];
            blue = rgb[2];
        }
    }

    public void setHSBA(float h, float s, float b, float a) {
        if (hue == h && saturation == s && brightness == b && alpha == a) return;
        hue = clamp(h);
        saturation = clamp(s);
        brightness = clamp(b);
        alpha = clamp(a);
        updateRGB();
    }

    private void updateHSB() {
        float h = 0;
        float s = 0;
        float v = Math.max(Math.max(red, green), blue);
        float d = v - Math.min(Math.min(red, green), blue);

        if (v != 0)
            s = d / v;

        if (s != 0) {
            if (red == v)
                h = 0 + (green - blue) / d;
            else if (green == v)
                h = 2 + (blue - red) / d;
            else
                h = 4 + (red - green) / d;
        }

        h = h * (float) (60.0 / 360);
        if (h < 0)
            h = h + 1;

        hue = h;
        saturation = s;
        brightness = v;
    }

    public float getRed() {
        return red;
    }

    public void setRed(float r) {
        red = clamp(r);
        updateHSB();
        updateColor();
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(float g) {
        green = clamp(g);
        updateHSB();
        updateColor();
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(float b) {
        blue = clamp(b);
        updateHSB();
        updateColor();
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float a) {
        alpha = clamp(a);
        updateColor();
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float h) {
        hue = clamp(h);
        updateRGB();
        updateColor();
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float s) {
        saturation = clamp(s);
        updateRGB();
        updateColor();
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float b) {
        brightness = clamp(b);
        updateRGB();
        updateColor();
    }

    private void updateColor() {
        newColor = new Color(red, green, blue, alpha);
        updatePanels();
        fireStateChanged();
    }

    private void updatePanels() {
        colorField.repaint();
        for (ColorPanel panel : panels) {
            panel.updateDraggableNumber();
            panel.repaint();
        }
        hexField.setText(new nodebox.graphics.Color(newColor).toString());
    }

    public ColorRange getColorRange() {
        return colorRange;
    }

    public void setColorRange(ColorRange colorRange) {
        this.colorRange = colorRange;
        getPreferences().put(COLOR_RANGE, colorRange.toString());
        for (ColorPanel panel : panels)
            panel.setColorRange(colorRange);
    }

    public Preferences getPreferences() {
        return preferences;
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

    public void stateChanged(ChangeEvent e) {
    }

    public static void main(String[] args) {
        ColorDialog cd = new ColorDialog(null);
        cd.setSize(500, 340);
        cd.setLocationByPlatform(true);
        cd.setAlwaysOnTop(true);
        cd.setVisible(true);
    }

    public class ColorField extends JButton {

        public ColorField() {
            Dimension d = new Dimension(75, 75);
            setMinimumSize(d);
            setPreferredSize(d);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getX() >= 2 && e.getX() <= 70 && e.getY() >= 38 && e.getY() <= 70)
                        setColor(color);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(newColor);
            g.fillRect(2, 2, 68, 35);
            g.setColor(color);
            g.fillRect(2, 37, 68, 34);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(2, 2, 67, 67);
            g.setColor(new Color(0.6F, 0.6F, 0.6F));
            g.drawLine(3, 2, 68, 2);
        }
    }

    public class ColorPanel extends JPanel implements ChangeListener {

        private ColorComponent colorComponent;
        private ColorSlider slider;
        private DraggableNumber draggableNumber;

        public ColorPanel(String colorComponent) {
            this(ColorComponent.valueOf(colorComponent.toUpperCase(Locale.US)));
        }

        public ColorPanel(ColorComponent colorComponent) {
            this.colorComponent = colorComponent;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Dimension d = new Dimension(Integer.MAX_VALUE, 30);
            setMinimumSize(d);
            setPreferredSize(d);
            setMaximumSize(d);
            JLabel label = new JLabel(nodebox.util.StringUtils.humanizeName(colorComponent.toString().toLowerCase(Locale.US)), JLabel.RIGHT);
            Dimension size = label.getSize();
            label.setFont(Theme.SMALL_BOLD_FONT);
            label.setForeground(new Color(66, 66, 66));
            label.setPreferredSize(new Dimension(75, size.height));
            label.setAlignmentY(JLabel.CENTER);
            label.setBorder(new EmptyBorder(3, 0, 0, 0));
            add(label);
            add(Box.createHorizontalStrut(2));
            draggableNumber = new DraggableNumber();
            draggableNumber.addChangeListener(this);
            d = new Dimension(20, Integer.MAX_VALUE);
            draggableNumber.setPreferredSize(d);
            draggableNumber.setMaximumSize(d);
            draggableNumber.setSize(d);
            add(draggableNumber);
            slider = new ColorSlider(this);
            add(slider);
            add(Box.createHorizontalStrut(7));
        }

        public void setColorRange(ColorRange colorRange) {
            if (colorRange == ColorRange.ABSOLUTE) {
                NumberFormat intFormat = NumberFormat.getNumberInstance(Locale.US);
                intFormat.setMinimumFractionDigits(0);
                intFormat.setMaximumFractionDigits(0);
                draggableNumber.setNumberFormat(intFormat);
                draggableNumber.setMinimumValue(0.0);
                draggableNumber.setMaximumValue(255.0);
            } else if (colorRange == ColorRange.PERCENTAGE) {
                NumberFormat floatFormat = NumberFormat.getNumberInstance(Locale.US);
                floatFormat.setMinimumFractionDigits(2);
                floatFormat.setMaximumFractionDigits(2);
                draggableNumber.setNumberFormat(floatFormat);
                draggableNumber.setMinimumValue(0.0);
                draggableNumber.setMaximumValue(100.0);
            }
            updateDraggableNumber();
        }

        public void setValue(float value) {
            switch (colorComponent) {
                case RED:
                    setRed(value);
                    break;
                case GREEN:
                    setGreen(value);
                    break;
                case BLUE:
                    setBlue(value);
                    break;
                case ALPHA:
                    setAlpha(value);
                    break;
                case HUE:
                    setHue(value);
                    break;
                case SATURATION:
                    setSaturation(value);
                    break;
                case BRIGHTNESS:
                    setBrightness(value);
                    break;
                default:
                    break;
            }
        }

        public void updateDraggableNumber() {
            float range = 0.0F;
            if (colorRange == ColorRange.ABSOLUTE)
                range = 255.0F;
            else if (colorRange == ColorRange.PERCENTAGE)
                range = 100.0F;
            switch (colorComponent) {
                case RED:
                    draggableNumber.setValue(red * range);
                    break;
                case GREEN:
                    draggableNumber.setValue(green * range);
                    break;
                case BLUE:
                    draggableNumber.setValue(blue * range);
                    break;
                case ALPHA:
                    draggableNumber.setValue(alpha * range);
                    break;
                case HUE:
                    draggableNumber.setValue(hue * range);
                    break;
                case SATURATION:
                    draggableNumber.setValue(saturation * range);
                    break;
                case BRIGHTNESS:
                    draggableNumber.setValue(brightness * range);
                    break;
                default:
                    break;
            }
        }

        public ColorComponent getColorComponent() {
            return colorComponent;
        }

        public void stateChanged(ChangeEvent e) {
            if (e.getSource() == draggableNumber) {
                float range = 0.0F;
                if (colorRange == ColorRange.ABSOLUTE)
                    range = 255.0F;
                else if (colorRange == ColorRange.PERCENTAGE)
                    range = 100.0F;
                float v = (float) (draggableNumber.getValue() / range);
                setValue(v);
            }
        }
    }

    public class ColorSlider extends JComponent {
        private ColorPanel panel;
        private final int WIDTH_OFFSET = 8;
        private final int HALF_WIDTH_OFFSET = WIDTH_OFFSET / 2;
        private final int HEIGHT_OFFSET = 10;
        private final int ARROW_HEIGHT = 6;

        public ColorSlider(ColorPanel panel) {
            this.panel = panel;
            enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r = g.getClipBounds();
            float value = 0F;

            switch (panel.getColorComponent()) {
                case RED:
                    drawRed(g);
                    value = red;
                    break;
                case GREEN:
                    drawGreen(g);
                    value = green;
                    break;
                case BLUE:
                    drawBlue(g);
                    value = blue;
                    break;
                case ALPHA:
                    drawAlpha(g);
                    value = alpha;
                    break;
                case HUE:
                    drawHue(g);
                    value = hue;
                    break;
                case SATURATION:
                    drawSaturation(g);
                    value = saturation;
                    break;
                case BRIGHTNESS:
                    drawBrightness(g);
                    value = brightness;
                    break;
                default:
                    break;
            }
            g.setColor(new Color(0.85F, 0.85F, 0.85F));
            g.drawLine(HALF_WIDTH_OFFSET, r.height - HEIGHT_OFFSET - 1, r.width - HALF_WIDTH_OFFSET - 1, r.height - HEIGHT_OFFSET - 1);
            g.drawLine(r.width - HALF_WIDTH_OFFSET - 1, 0, r.width - HALF_WIDTH_OFFSET - 1, r.height - HEIGHT_OFFSET - 1);
            g.drawLine(HALF_WIDTH_OFFSET, 0, HALF_WIDTH_OFFSET, r.height - HEIGHT_OFFSET - 1);
            g.setColor(new Color(0.65F, 0.65F, 0.65F));
            g.drawLine(HALF_WIDTH_OFFSET + 1, 0, r.width - HALF_WIDTH_OFFSET - 1, 0);

            int i = (int) Math.round(value * (r.width - WIDTH_OFFSET));
            g.setColor(Color.BLACK);
            Polygon p = new Polygon();
            p.addPoint(i + HALF_WIDTH_OFFSET, r.height - HEIGHT_OFFSET);
            p.addPoint(i + WIDTH_OFFSET, r.height - ARROW_HEIGHT);
            p.addPoint(i, r.height - ARROW_HEIGHT);
            g.fillPolygon(p);
        }

        private void drawRed(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color c = new Color((float) i / width, green, blue);
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawGreen(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color c = new Color(red, (float) i / width, blue);
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawBlue(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color c = new Color(red, green, (float) i / width);
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawAlpha(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color c = new Color(red, green, blue, (float) i / width);
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawHue(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color hsb = Color.getHSBColor((float) i / width, saturation, brightness);
                Color c = new Color(hsb.getRed(), hsb.getGreen(), hsb.getBlue());
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawSaturation(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color hsb = Color.getHSBColor(hue, (float) i / width, brightness);
                Color c = new Color(hsb.getRed(), hsb.getGreen(), hsb.getBlue());
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        private void drawBrightness(Graphics g) {
            Rectangle r = g.getClipBounds();
            int width = r.width - WIDTH_OFFSET;
            for (int i = 0; i < width; i++) {
                Color hsb = Color.getHSBColor(hue, saturation, (float) i / width);
                Color c = new Color(hsb.getRed(), hsb.getGreen(), hsb.getBlue());
                g.setColor(c);
                g.fillRect(i + HALF_WIDTH_OFFSET, 0, 1, r.height - HEIGHT_OFFSET);
            }
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                int width = getWidth() - WIDTH_OFFSET;
                int x = e.getX();
                if (x < HALF_WIDTH_OFFSET)
                    panel.setValue(0);
                else if (x > width + WIDTH_OFFSET)
                    panel.setValue(1);
                else
                    panel.setValue((float) (x - HALF_WIDTH_OFFSET) / width);
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                int width = getWidth() - WIDTH_OFFSET;
                int x = e.getX();
                if (x < HALF_WIDTH_OFFSET)
                    panel.setValue(0);
                else if (x > width + WIDTH_OFFSET)
                    panel.setValue(1);
                else
                    panel.setValue((float) (x - HALF_WIDTH_OFFSET) / width);
            }
        }
    }

    private class ColorRangeMenu extends PaneMenu {

        private ColorRangePopup colorRangePopup;

        public ColorRangeMenu() {
            colorRangePopup = new ColorRangePopup();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            colorRangePopup.show(this, 0, 20);
        }

        @Override
        public String getMenuName() {
            switch (colorRange) {
                case PERCENTAGE:
                    return "0-100";
                case ABSOLUTE:
                    return "0-255";
                default:
                    return "0-255";
            }
        }

        private class ColorRangePopup extends JPopupMenu {
            public ColorRangePopup() {
                add(new ChangeColorRangeAction("0-100", ColorRange.PERCENTAGE));
                add(new ChangeColorRangeAction("0-255", ColorRange.ABSOLUTE));
            }
        }

        private class ChangeColorRangeAction extends AbstractAction {
            private ColorRange colorRange;

            private ChangeColorRangeAction(String name, ColorRange colorRange) {
                super(name);
                this.colorRange = colorRange;
            }

            public void actionPerformed(ActionEvent e) {
                setColorRange(colorRange);
                ColorRangeMenu.this.repaint();
            }
        }
    }

    public class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, "Ok");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_ENTER));
        }

        public void actionPerformed(ActionEvent e) {
            setColor(newColor);
            ColorDialog.this.setVisible(false);
        }
    }

    public class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, "Cancel");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            setColor(color);
            ColorDialog.this.setVisible(false);
        }
    }
}
