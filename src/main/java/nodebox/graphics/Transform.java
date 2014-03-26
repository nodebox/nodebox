package nodebox.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Transform implements Cloneable {

    public enum Mode {
        CORNER, CENTER
    }

    private AffineTransform affineTransform;

    public static Transform translated(double tx, double ty) {
        Transform t = new Transform();
        t.translate(tx, ty);
        return t;
    }

    public static Transform translated(Point t) {
        return translated(t.x, t.y);
    }

    public static Transform rotated(double degrees) {
        Transform t = new Transform();
        t.rotate(degrees);
        return t;
    }

    public static Transform rotatedRadians(double radians) {
        Transform t = new Transform();
        t.rotateRadians(radians);
        return t;
    }

    public static Transform scaled(double scale) {
        Transform t = new Transform();
        t.scale(scale);
        return t;
    }

    public static Transform scaled(double sx, double sy) {
        Transform t = new Transform();
        t.scale(sx, sy);
        return t;
    }

    public static Transform scaled(Point s) {
        return scaled(s.x, s.y);
    }

    public static Transform skewed(double skew) {
        Transform t = new Transform();
        t.skew(skew);
        return t;
    }

    public static Transform skewed(double kx, double ky) {
        Transform t = new Transform();
        t.skew(kx, ky);
        return t;
    }

    public Transform() {
        affineTransform = new AffineTransform();
    }

    public Transform(double m00, double m10, double m01, double m11, double m02, double m12) {
        affineTransform = new AffineTransform(m00, m10, m01, m11, m02, m12);
    }

    public Transform(AffineTransform affineTransform) {
        this.affineTransform = affineTransform;
    }

    public Transform(Transform other) {
        this.affineTransform = (AffineTransform) other.affineTransform.clone();
    }

    //// Transform changes ////

    public void translate(Point point) {
        affineTransform.translate(point.x, point.y);
    }

    public void translate(double tx, double ty) {
        affineTransform.translate(tx, ty);
    }

    public void rotate(double degrees) {
        double radians = degrees * Math.PI / 180;
        affineTransform.rotate(radians);
    }

    public void rotateRadians(double radians) {
        affineTransform.rotate(radians);
    }

    public void scale(double scale) {
        affineTransform.scale(scale, scale);
    }

    public void scale(double sx, double sy) {
        affineTransform.scale(sx, sy);
    }

    public void skew(double skew) {
        skew(skew, skew);
    }

    public void skew(double kx, double ky) {
        kx = Math.PI * kx / 180.0;
        ky = Math.PI * ky / 180.0;
        affineTransform.concatenate(new AffineTransform(1, Math.tan(ky), -Math.tan(kx), 1, 0, 0));
    }

    public boolean invert() {
        try {
            affineTransform = affineTransform.createInverse();
            return true;
        } catch (NoninvertibleTransformException e) {
            return false;
        }
    }

    public void append(Transform t) {
        affineTransform.concatenate(t.affineTransform);
    }

    public void prepend(Transform t) {
        affineTransform.preConcatenate(t.affineTransform);
    }

    //// Operations ////

    public Point map(Point p) {
        Point2D.Double p2 = new Point2D.Double();
        affineTransform.transform(p.toPoint2D(), p2);
        return new Point(p2);
    }

    public Rect map(Rect r) {
        // TODO: The size conversion might be incorrect. (using deltaTransform) In that case, make topLeft and bottomRight points.
        Point2D origin = new Point2D.Double(r.getX(), r.getY());
        Point2D size = new Point2D.Double(r.getWidth(), r.getHeight());
        Point2D transformedOrigin = new Point2D.Double();
        Point2D transformedSize = new Point2D.Double();
        affineTransform.transform(origin, transformedOrigin);
        affineTransform.deltaTransform(size, transformedSize);
        return new Rect(transformedOrigin.getX(), transformedOrigin.getY(), transformedSize.getX(), transformedSize.getY());
    }

    public IGeometry map(IGeometry shape) {
        if (shape instanceof Path) {
            return map((Path) shape);
        } else if (shape instanceof Geometry) {
            return map((Geometry) shape);
        } else {
            throw new RuntimeException("Unsupported geometry type " + shape);
        }
    }

    public Path map(Path p) {
        Path newPath = new Path(p, false);
        for (Contour c : p.getContours()) {
            Contour newContour = new Contour(map(c.getPoints()), c.isClosed());
            newPath.add(newContour);
        }
        return newPath;
    }

    public Geometry map(Geometry g) {
        Geometry newGeometry = new Geometry();
        for (Path p : g.getPaths()) {
            Path newPath = map(p);
            newGeometry.add(newPath);
        }
        return newGeometry;
    }

    /**
     * Transform all the given points and return a list of transformed points.
     * Points are immutable, so they can not be transformed in-place.
     *
     * @param points The points to transform.
     * @return The list of transformed points.
     */
    public List<Point> map(List<Point> points) {
        // Prepare the points for the AffineTransform transformation.
        double[] coords = new double[points.size() * 2];
        int i = 0;
        for (Point pt : points) {
            coords[i++] = pt.x;
            coords[i++] = pt.y;
        }
        affineTransform.transform(coords, 0, coords, 0, points.size());

        // Convert the transformed points into a new List.
        List<Point> transformed = new ArrayList<Point>(points.size());
        int pointIndex = 0;
        for (i = 0; i < coords.length; i += 2) {
            transformed.add(new Point(coords[i], coords[i + 1], points.get(pointIndex).type));
            pointIndex++;
        }
        return transformed;
    }

    public Rect convertBoundsToFrame(Rect bounds) {
        AffineTransform t = fullTransform(bounds);
        Point2D transformedOrigin = new Point2D.Double();
        Point2D transformedSize = new Point2D.Double();
        t.transform(new Point2D.Double(bounds.getX(), bounds.getY()), transformedOrigin);
        t.deltaTransform(new Point2D.Double(bounds.getWidth(), bounds.getHeight()), transformedSize);
        return new Rect(transformedOrigin.getX(), transformedOrigin.getY(), transformedSize.getX(), transformedSize.getY());
    }

    private AffineTransform fullTransform(Rect bounds) {
        double cx = bounds.getX() + bounds.getWidth() / 2;
        double cy = bounds.getY() + bounds.getHeight() / 2;
        AffineTransform t = new AffineTransform();
        t.translate(cx, cy);
        t.preConcatenate(affineTransform);
        t.translate(-cx, -cy);
        return t;
    }

    @Override
    protected Transform clone() {
        return new Transform(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transform)) return false;
        return getAffineTransform().equals(((Transform) obj).getAffineTransform());
    }

    public void apply(Graphics2D g, Rect bounds) {
        AffineTransform t = fullTransform(bounds);
        g.transform(t);
    }

    public AffineTransform getAffineTransform() {
        return affineTransform;
    }
}
