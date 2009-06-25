package nodebox.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

public class Transform implements Cloneable {

    private AffineTransform affineTransform;

    public static Transform translated(float tx, float ty) {
        Transform t = new Transform();
        t.translate(tx, ty);
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
        affineTransform.preConcatenate(new AffineTransform(1, Math.tan(ky), -Math.tan(kx), 1, 0, 0));
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
        affineTransform.transform(p.getPoint2D(), p2);
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
        return new Rect((float) transformedOrigin.getX(), (float) transformedOrigin.getY(), (float) transformedSize.getX(), (float) transformedSize.getY());
    }

    public BezierPath map(BezierPath p) {
        // TODO: Implement
        return p;
    }

    /**
     * Transforms all the given points in place.
     *
     * @param points the point array to transform.
     */
    public void map(Point[] points) {
        float[] coords = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            coords[i * 2] = points[i].x;
            coords[i * 2 + 1] = points[i].y;
        }
        affineTransform.transform(coords, 0, coords, 0, points.length);
        for (int i = 0; i < points.length; i ++) {
            points[i].x = coords[i * 2];
            points[i].y = coords[i * 2 + 1];
        }
    }

    public Rect convertBoundsToFrame(Rect bounds) {
        AffineTransform t = fullTransform(bounds);
        Point2D transformedOrigin = new Point2D.Double();
        Point2D transformedSize = new Point2D.Double();
        t.transform(new Point2D.Double(bounds.getX(), bounds.getY()), transformedOrigin);
        t.deltaTransform(new Point2D.Double(bounds.getWidth(), bounds.getHeight()), transformedSize);
        return new Rect((float) transformedOrigin.getX(), (float) transformedOrigin.getY(), (float) transformedSize.getX(), (float) transformedSize.getY());
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
