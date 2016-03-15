package nodebox.client.visualizer;

import nodebox.ui.Theme;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The objects visualizer can visualize everything.
 */
public final class LastResortVisualizer implements Visualizer {

    public static final LastResortVisualizer INSTANCE = new LastResortVisualizer();

    private static final Rectangle2D BIG_RECTANGLE = new Rectangle2D.Double(0, 0, Double.MAX_VALUE, Double.MAX_VALUE);
    private static final Point2D ZERO_POINT = new Point2D.Double(0, 0);

    private LastResortVisualizer() {
    }

    public boolean accepts(Iterable<?> objects, Class listClass) {
        return true;
    }

    public Rectangle2D getBounds(Iterable<?> objects) {
        return BIG_RECTANGLE;
    }

    public Point2D getOffset(Iterable<?> objects, Dimension2D size) {
        return ZERO_POINT;
    }

    public void draw(Graphics2D g, Iterable<?> objects) {
        if (objects == null) {
            return;
        }
        g.setColor(Theme.TEXT_NORMAL_COLOR);
        g.setFont(Theme.EDITOR_FONT);
        AffineTransform t = g.getTransform();
        for (Object o : objects) {
            String s = o.toString();
            for (String line : s.split("\n")) {
                g.drawString(line, 5, 20);
                g.translate(0, 14);
            }
            g.drawLine(-100, 10, 1000, 10);
            g.translate(0, 14);
        }
        g.setTransform(t);
    }

}
