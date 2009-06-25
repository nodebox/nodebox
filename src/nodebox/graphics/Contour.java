package nodebox.graphics;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

public class Contour implements IGeometry {

    private static final int DEFAULT_CAPACITY = 10;
    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1f);

    private Point[] points;
    private int pointCount = 0;

    public Contour() {
        points = new Point[0];
    }

    public Contour(Contour other) {
        points = new Point[other.points.length];
        // The copy constructor creates clones of the original points.
        for (int i=0;i<other.points.length;i++) {
            points[i] = other.points[i].clone();
        }
    }

    //// Point operations ////

    public int getPointCount() {
        return points.length;
    }

    public Point[] getPoints() {
        return points;
    }

    public void addPoint(Point pt) {
        ensureLength(pointCount + 1);
        points[pointCount++] = pt.clone();
    }

    public void addPoint(float x, float y) {
        ensureLength(pointCount + 1);
        points[pointCount++] = new Point(x, y);
    }

    private void ensureLength(int length) {
        if (points.length >= length) return;
        // Copy everything over into new array.
        Point[] newPoints = new Point[length];
        System.arraycopy(points, 0, newPoints, 0, points.length);
        points = newPoints;
    }

    //// Geometric queries ////

    public Rect getBounds() {
        if (points.length == 0) {
            return new Rect();
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float px, py;
        for (Point p : points) {
            px = p.getX();
            py = p.getY();
            if (px < minX) minX = px;
            if (py < minY) minY = py;
            if (px > maxX) maxX = px;
            if (py > maxY) maxY = py;
        }
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    //// Geometric operations ////

    public void flatten() {
        throw new UnsupportedOperationException();
    }

    public IGeometry flattened() {
        throw new UnsupportedOperationException();
    }

    //// Graphics ////

    public void inheritFromContext(GraphicsContext ctx) {
    }

    public void draw(Graphics2D g) {
        if (pointCount < 2) return;
        // Since a contour has no fill or stroke information, draw it in black.
        // We save the current color so as not to disrupt the context.
        java.awt.Color savedColor = g.getColor();
        Stroke savedStroke = g.getStroke();
        GeneralPath gp = new GeneralPath(Path2D.WIND_EVEN_ODD, pointCount);
        _extendPath(gp);
        g.setColor(java.awt.Color.BLACK);
        g.setStroke(DEFAULT_STROKE);
        g.draw(gp);
        g.setColor(savedColor);
        g.setStroke(savedStroke);
    }

    void _extendPath(GeneralPath gp) {
        Point pt = points[0];
        Point ctrl1, ctrl2;
        gp.moveTo(pt.x, pt.y);
        for (int i = 1; i < pointCount; i++) {
            pt = points[i];
            if (pt.isLineTo()) {
                gp.lineTo(pt.x, pt.y);
            } else if (pt.isCurveTo()) {
                ctrl1 = points[i - 2];
                ctrl2 = points[i - 1];
                gp.curveTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, pt.x, pt.y);
                // We used up two extra points.
                i += 2;
            }
        }
        gp.closePath();
    }

    public void transform(Transform t) {
        throw new UnsupportedOperationException();
    }

    //// Conversions ////

    public Path toPath() {
        return new Path(this);
    }

    //// Object operations ////

    public Contour clone() {
        return new Contour(this);
    }
}
