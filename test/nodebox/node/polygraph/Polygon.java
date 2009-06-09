package nodebox.node.polygraph;

import java.util.ArrayList;
import java.util.List;

/**
 * A polygon is a list of two-dimensional points.
 */
public class Polygon {

    private List<Point> points = new ArrayList<Point>();

    public static Polygon rect(float x, float y, float width, float height) {
        Polygon p = new Polygon();
        p.addPoint(new Point(x, y));
        p.addPoint(new Point(x + width, y));
        p.addPoint(new Point(x + width, y + height));
        p.addPoint(new Point(x, y + height));
        return p;
    }

    public void addPoint(Point p) {
        points.add(p);
    }

    public void extend(Polygon p) {
        for (Point pt : p.points) {
            addPoint(pt);
        }
    }

    public List<Point> getPoints() {
        return points;
    }

    public Rectangle getBounds() {
        if (points.isEmpty()) return new Rectangle();
        float x1, y1, x2, y2;
        int i = points.size() - 1;
        x1 = x2 = points.get(i).x;
        y1 = y2 = points.get(i).y;
        i--;
        while (i > 0) {
            Point pt = points.get(i);
            float x = pt.x;
            float y = pt.y;
            if (x < x1) x1 = x;
            if (y < y1) y1 = y;
            if (x > x2) x2 = x;
            if (y > y2) y2 = y;
            i--;
        }
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Returns a translated copy of this polygon.
     *
     * @param tx delta x value
     * @param ty delta y value
     * @return of copy of this Polygon with all points translated.
     */
    public Polygon translated(float tx, float ty) {
        Polygon p = new Polygon();
        for (Point pt : getPoints()) {
            p.addPoint(new Point(pt.x + tx, pt.y + ty));
        }
        return p;
    }

    @Override
    public Polygon clone() {
        Polygon p = new Polygon();
        for (Point pt : points) {
            p.addPoint(pt);
        }
        return p;
    }
}
