package nodebox.client.visualizer;

import com.google.common.collect.Iterables;
import nodebox.graphics.Color;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Visualizes Color objects.
 */
public final class ColorVisualizer implements Visualizer {

    public static final ColorVisualizer INSTANCE = new ColorVisualizer();
    private static final int COLOR_SIZE = 30;
    public static final int COLOR_MARGIN = 10;
    public static final int MAX_WIDTH = 500;
    public static final int COLORS_PER_ROW = (MAX_WIDTH / (COLOR_SIZE + COLOR_MARGIN)) + 1;
    private static final int COLOR_TOTAL_SIZE = COLOR_SIZE + COLOR_MARGIN;

    private ColorVisualizer() {
    }

    public boolean accepts(Iterable<?> objects, Class listClass) {
        return Color.class.isAssignableFrom(listClass);
    }

    public Rectangle2D getBounds(Iterable<?> objects) {
        int size = Iterables.size(objects);
        int h = ((size / COLORS_PER_ROW) + 1) * COLOR_TOTAL_SIZE;
        return new Rectangle2D.Double(0, 0, MAX_WIDTH, h);
    }

    public Point2D getOffset(Iterable<?> objects, Dimension2D size) {
        return new Point2D.Double(10, 10);
    }

    @SuppressWarnings("unchecked")
    public void draw(Graphics2D g, Iterable<?> objects) {
        int x = 0;
        int y = 0;

        for (Object o : objects) {
            Color c = (Color) o;
            drawColor(g, c, x, y);
            x += COLOR_TOTAL_SIZE;
            if (x > MAX_WIDTH) {
                x = 0;
                y += COLOR_TOTAL_SIZE;
            }
        }
    }

    private static void drawColor(Graphics2D g, Color c, int x, int y) {
        g.setColor(java.awt.Color.WHITE);
        g.fillRoundRect(x, y, COLOR_SIZE + 6, COLOR_SIZE + 6, 3, 3);
        g.setColor(java.awt.Color.LIGHT_GRAY);
        g.drawRoundRect(x, y, COLOR_SIZE + 6, COLOR_SIZE + 6, 3, 3);
        g.setColor(c.getAwtColor());
        g.fillRect(x + 3, y + 3, COLOR_SIZE, COLOR_SIZE);
    }

}
