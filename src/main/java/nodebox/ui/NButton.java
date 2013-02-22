package nodebox.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * A custom button that uses an icon and shadowed text.
 */
public class NButton extends JComponent implements MouseListener {

    public static final int BUTTON_HEIGHT = 21;
    public static final int IMAGE_TEXT_MARGIN = 3;
    public static final int TEXT_BASELINE = 14;
    public static final Image checkOn, checkOff, checkDisabledOn, checkDisabledOff;
    private String text;
    private Image normalImage, checkedImage;
    private Object actionObject;
    private Method actionMethod;
    private Mode mode;
    private boolean armed = false;
    private boolean pressed = false;
    private boolean checked = false;
    private boolean warn = false;

    static {
        try {
            checkOn = ImageIO.read(NButton.class.getResourceAsStream("/check-on.png"));
            checkOff = ImageIO.read(NButton.class.getResourceAsStream("/check-off.png"));
            checkDisabledOn = ImageIO.read(NButton.class.getResourceAsStream("/check-disabled-on.png"));
            checkDisabledOff = ImageIO.read(NButton.class.getResourceAsStream("/check-disabled-off.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a push button.
     *
     * @param text      the button label
     * @param imageFile the button image
     */
    public NButton(String text, String imageFile) {
        this(Mode.PUSH, text, imageFile, null);
    }

    public NButton(String text, InputStream imageStream) {
        this(Mode.PUSH, text, imageStream, null);
    }

    public NButton(String text, Image image) {
        init(Mode.PUSH, text, image, null);
    }

    /**
     * Create a check button.
     *
     * @param text         the button label
     * @param normalImage  the image to show when unchecked
     * @param checkedImage the image to show when checked
     */
    public NButton(String text, String normalImage, String checkedImage) {
        this(Mode.CHECK, text, normalImage, checkedImage);
    }

    public NButton(String text, InputStream normalImage, InputStream checkedImage) {
        this(Mode.CHECK, text, normalImage, checkedImage);
    }

    public NButton(Mode mode, String text) {
        if (mode != Mode.CHECK)
            throw new AssertionError("Only use Mode.CHECK.");
        init(mode, text, checkOff, checkOn);
    }

    private NButton(Mode mode, String text, InputStream normalImage, InputStream checkedImage) {
        Image normal, checked = null;
        try {
            normal = ImageIO.read(normalImage);
            if (checkedImage != null)
                checked = ImageIO.read(checkedImage);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image.", e);
        }
        init(mode, text, normal, checked);
    }

    private NButton(Mode mode, String text, String normalImage, String checkedImage) {
        Image normal, checked = null;
        try {
            normal = ImageIO.read(new File(normalImage));
            if (checkedImage != null)
                checked = ImageIO.read(new File(checkedImage));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image.", e);
        }
        init(mode, text, normal, checked);
    }

    private void init(Mode mode, String text, Image normalImage, Image checkedImage) {
        this.mode = mode;
        this.text = text;
        this.normalImage = normalImage;
        this.checkedImage = checkedImage;
        int width = measureWidth();
        Dimension d = new Dimension(width, BUTTON_HEIGHT);
        setSize(d);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        addMouseListener(this);
    }

    private int measureWidth() {
        // To measure text we need a graphics context. Create an image and use its' graphics context.
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        g2.setFont(Theme.SMALL_BOLD_FONT);
        int width = normalImage.getWidth(null);
        width += IMAGE_TEXT_MARGIN;
        width += (int) g2.getFontMetrics().stringWidth(text);
        width += 1; // Anti-aliasing can take up an extra pixel.
        return width;
    }

    /**
     * Set the method that will be called when the button is pressed.
     *
     * @param obj        the instance
     * @param methodName the name of the method on the instance. The method should not take any arguments.
     */
    public void setActionMethod(Object obj, String methodName) {
        actionObject = obj;
        try {
            actionMethod = obj.getClass().getMethod(methodName);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Ojbect or method name cannot be null.");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot find method " + methodName + " on object " + actionObject);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean v) {
        if (v == checked) return;
        checked = v;
        repaint();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        int width = measureWidth();
        Dimension d = new Dimension(width, BUTTON_HEIGHT);
        setSize(d);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public boolean isWarning() {
        return warn;
    }

    public void setWarning(boolean warn) {
        if (warn == this.warn) return;
        this.warn = warn;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (isEnabled()) {
            if (checked) {
                g2.drawImage(checkedImage, 0, 0, null);
            } else {
                g2.drawImage(normalImage, 0, 0, null);
            }
        } else {
            if (mode == Mode.CHECK && checked) {
                g2.drawImage(checkDisabledOn, 0, 0, null);
            } else if (mode == Mode.CHECK && !checked) {
                g2.drawImage(checkDisabledOff, 0, 0, null);
            } else {
                Composite oldComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(normalImage, 0, 0, null);
                g2.setComposite(oldComposite);
            }
        }
        int w = normalImage.getWidth(null);
        g2.setFont(Theme.SMALL_BOLD_FONT);
        if (warn) {
            g2.setColor(Theme.TEXT_WARNING_COLOR);
        } else if (armed) {
            g2.setColor(Theme.TEXT_ARMED_COLOR);
        } else if (!isEnabled()) {
            g2.setColor(Theme.TEXT_DISABLED_COLOR);
        } else {
            g2.setColor(Theme.TEXT_NORMAL_COLOR);
        }
        SwingUtils.drawShadowText(g2, text, w + IMAGE_TEXT_MARGIN, TEXT_BASELINE);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (!isEnabled()) return;
        pressed = true;
        armed = true;
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        if (!isEnabled()) return;
        pressed = false;
        if (armed) {
            armed = false;
            if (mode == Mode.CHECK)
                checked = !checked;
            try {
                actionMethod.invoke(actionObject);
            } catch (Exception e1) {
                throw new RuntimeException("Could not invoke method " + actionMethod + " on object " + actionObject, e1);
            }
            repaint();
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (!isEnabled()) return;
        if (pressed) {
            armed = true;
            repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        if (!isEnabled()) return;
        armed = false;
        repaint();
    }

    public static enum Mode {
        PUSH, CHECK
    }
}

