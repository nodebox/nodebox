package nodebox.client.visualizer;

import com.google.common.collect.Iterables;
import nodebox.graphics.Point;

import java.awt.*;
import java.awt.geom.*;

public class PointVisualizer implements Visualizer {

    public static final PointVisualizer INSTANCE = new PointVisualizer();
    public static final double HALF_POINT_SIZE = 2;
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
        GeneralPath onCurves = new GeneralPath();
        GeneralPath offCurves = new GeneralPath();
        for (Point point : points) {
            Shape s = new Ellipse2D.Double(point.x - HALF_POINT_SIZE, point.y - HALF_POINT_SIZE, POINT_SIZE, POINT_SIZE);
            if (point.isOnCurve()) {
                onCurves.append(s, false);
            } else {
                offCurves.append(s, false);
            }
        }
        g.setColor(Color.BLUE);
        g.fill(onCurves);
        g.setColor(Color.RED);
        g.fill(offCurves);
    }

}
