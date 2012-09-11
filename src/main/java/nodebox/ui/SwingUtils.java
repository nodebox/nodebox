package nodebox.ui;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

public class SwingUtils {

    public static void centerOnScreen(Window w) {
        centerOnScreen(w, null);
    }

    public static void centerOnScreen(Window w, Window parent) {
        Rectangle r = new Rectangle();
        if (parent == null) {
            r.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        } else {
            r.setLocation(parent.getLocation());
            r.setSize(parent.getSize());
        }
        // Determine the new location of the alert
        int x = r.x + (r.width - w.getWidth()) / 2;
        int y = r.y + (r.height - w.getHeight()) / 2;
        // Move the alert
        w.setLocation(x, y);
    }

    public static void drawShadowText(Graphics2D g2, String s, int x, int y) {
        drawShadowText(g2, s, x, y, Theme.TEXT_SHADOW_COLOR, 1);
    }

    public static void drawShadowText(Graphics2D g2, String s, int x, int y, Color shadowColor, int offset) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color c = g2.getColor();
        g2.setColor(shadowColor);
        g2.drawString(s, x, y + offset);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    public static void drawCenteredShadowText(Graphics2D g2, String s, int x, int y) {
        drawCenteredShadowText(g2, s, x, y, Theme.TEXT_SHADOW_COLOR);
    }

    public static void drawCenteredShadowText(Graphics2D g2, String s, int x, int y, Color shadowColor) {
        FontRenderContext frc = g2.getFontRenderContext();
        Rectangle2D bounds = g2.getFont().getStringBounds(s, frc);
        int leftX = (int) (x - (float) bounds.getWidth() / 2);
        drawShadowText(g2, s, leftX, y, shadowColor, 1);
    }

    /**
     * Make the color brighter by the specified factor.
     * <p/>
     * This code is adapted from java.awt.Color to take in a factor.
     * Therefore, it inherits its quirks. Most importantly,
     * a smaller factor makes the color more bright.
     *
     * @param c      a color
     * @param factor the smaller the factor, the brighter.
     * @return a brighter color
     */
    private static Color brighter(Color c, double factor) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - factor));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / factor), 255),
                Math.min((int) (g / factor), 255),
                Math.min((int) (b / factor), 255));
    }

    /**
     * Find the Pane that the given component is in.
     *
     * @param c an AWT component.
     * @return a Pane, or null if this component is not in a pane.
     */
    public static Pane getPaneForComponent(Component c) {
        while (c != null) {
            if (c instanceof Pane) break;
            c = c.getParent();
        }
        return (Pane) c;
    }
}
