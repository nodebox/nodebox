package nodebox.client.visualizer;

import com.google.common.collect.Iterables;
import nodebox.graphics.Path;
import nodebox.graphics.Point;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class PointVisualizer implements Visualizer {

    public static final PointVisualizer INSTANCE = new PointVisualizer();
    public static final double POINT_SIZE = 4;

    private PointVisualizer() {
    }

    public boolean accepts(Iterable<?> objects, Class listClass) {
        return listClass == Point.class;
    }

    public Rectangle2D getBounds(Iterable<?> objects) {
        Rectangle2D.Double bounds = new Rectangle2D.Double();
        for (Object o : objects) {
            if (o instanceof Point) {
                Point pt = (Point) o;
                bounds.add(pt.toPoint2D());
            } else if (o instanceof Iterable) {
                bounds.add(getBounds((Iterable<?>) o));
            }
        }
        bounds.x -= 5;
        bounds.y -= 5;
        bounds.width += 10;
        bounds.height += 10;
        return bounds;
    }

    public Point2D getOffset(Iterable<?> objects, Dimension2D size) {
        return new Point2D.Double(size.getWidth() / 2, size.getHeight() / 2);
    }

    @SuppressWarnings("unchecked")
    public void draw(Graphics2D g, Iterable<?> objects) {
        Object firstObject = Iterables.getFirst(objects, null);
        if (firstObject instanceof Point)
            drawPoints(g, (Iterable<Point>) objects);
        else if (firstObject instanceof Iterable) {
            for (Object o : objects)
                draw(g, (Iterable<?>) o);
        }
    }

    public static void drawPoints(Graphics2D g, Iterable<Point> points) {
        Path onCurves = new Path();
        Path offCurves = new Path();
        onCurves.setFill(new nodebox.graphics.Color(0, 0, 1));
        offCurves.setFill(new nodebox.graphics.Color(1, 0, 0));
        for (Point point : points) {
            if (point.isOnCurve()) {
                onCurves.ellipse(point.x, point.y, POINT_SIZE, POINT_SIZE);
            } else {
                offCurves.ellipse(point.x, point.y, POINT_SIZE, POINT_SIZE);
            }
        }
        onCurves.draw(g);
        offCurves.draw(g);
    }

}
