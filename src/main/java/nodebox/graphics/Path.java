package nodebox.graphics;

import com.google.common.base.Function;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Base class for all geometric (vector) data.
 */
public class Path extends AbstractGeometry implements Colorizable, Iterable<Point> {

    // Simulate a quarter of a circle.
    private static final double ONE_MINUS_QUARTER = 1.0 - 0.552;

    private Color fillColor = null;
    private Color strokeColor = null;
    private double strokeWidth = 1;
    private ArrayList<Contour> contours;
    private transient Contour currentContour = null;
    private transient boolean pathDirty = true;
    private transient boolean lengthDirty = true;
    private transient java.awt.geom.GeneralPath awtPath;
    private transient Rect bounds;
    private transient ArrayList<Double> contourLengths;
    private transient double pathLength = -1;

    public Path() {
        fillColor = Color.BLACK;
        strokeColor = null;
        strokeWidth = 1;
        contours = new ArrayList<Contour>();
        currentContour = null;
    }

    public Path(Path other) {
        this(other, true);
    }

    public Path(Path other, boolean cloneContours) {
        fillColor = other.fillColor == null ? null : other.fillColor.clone();
        strokeColor = other.strokeColor == null ? null : other.strokeColor.clone();
        strokeWidth = other.strokeWidth;
        if (cloneContours) {
            contours = new ArrayList<Contour>(other.contours.size());
            extend(other);
            if (!contours.isEmpty()) {
                // Set the current contour to the last contour.
                currentContour = contours.get(contours.size() - 1);
            }
        } else {
            contours = new ArrayList<Contour>();
            currentContour = null;
        }
    }

    public Path(Shape s) {
        this();
        extend(s);
    }

    public Path(Contour c) {
        this();
        add(c);
    }

    /**
     * Wrap the current path in a geometry object.
     *
     * @return a Geometry object
     */
    public Geometry asGeometry() {
        Geometry g = new Geometry();
        g.add(this);
        return g;
    }

    //// Color operations ////

    public Color getFillColor() {
        return fillColor;
    }

    public Color getFill() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setFill(Color c) {
        setFillColor(c);
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public Color getStroke() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStroke(Color c) {
        setStrokeColor(c);
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    //// Point operations ////

    public int getPointCount() {
        if (contours == null) return 0;
        int pointCount = 0;
        for (Contour c : contours) {
            pointCount += c.getPointCount();
        }
        return pointCount;
    }

    /**
     * Get the points for this geometry.
     * <p/>
     * This returns a live reference to the points of the geometry. Changing the points will change the geometry.
     *
     * @return a list of Points.
     */
    public java.util.List<Point> getPoints() {
        if (contours.isEmpty()) return new ArrayList<Point>(0);
        ArrayList<Point> points = new ArrayList<Point>();
        for (Contour c : contours) {
            points.addAll(c.getPoints());
        }
        return points;
    }

    //// Primitives ////

    public void moveto(double x, double y) {
        // Stop using the current contour. addPoint will automatically create a new contour.
        currentContour = null;
        addPoint(x, y);
    }

    public void lineto(double x, double y) {
        if (currentContour == null)
            throw new RuntimeException("Lineto without moveto first.");
        addPoint(x, y);
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
        if (currentContour == null)
            throw new RuntimeException("Curveto without moveto first.");
        addPoint(new Point(x1, y1, Point.CURVE_DATA));
        addPoint(new Point(x2, y2, Point.CURVE_DATA));
        addPoint(new Point(x3, y3, Point.CURVE_TO));
    }

    public void close() {
        if (currentContour != null)
            currentContour.close();
        currentContour = null;
        invalidate(false);
    }

    /**
     * Start a new contour without closing the current contour first.
     * <p/>
     * You can call this method even when there is no current contour.
     */
    public void newContour() {
        currentContour = null;
    }

    public void addPoint(Point pt) {
        ensureCurrentContour();
        currentContour.addPoint(pt);
        invalidate(false);
    }

    public void addPoint(double x, double y) {
        ensureCurrentContour();
        currentContour.addPoint(x, y);
        invalidate(false);
    }

    /**
     * Invalidates the cache. Querying the path length or asking for getGeneralPath will return an up-to-date result.
     * <p/>
     * This operation recursively invalidates all underlying geometry.
     * <p/>
     * Cache invalidation happens automatically when using the Path methods, such as rect/ellipse,
     * or container operations such as add/extend/clear. You should invalidate the cache when manually changing the
     * point positions or adding points to the underlying contours.
     * <p/>
     * Invalidating the cache is a lightweight operation; it doesn't recalculate anything. Only when querying the
     * new length will the values be recalculated.
     */
    public void invalidate() {
        invalidate(true);
    }

    private void invalidate(boolean recursive) {
        pathDirty = true;
        lengthDirty = true;
        if (recursive) {
            for (Contour c : contours) {
                c.invalidate();
            }
        }
    }

    /**
     * Ensure that there is a contour available.
     */
    private void ensureCurrentContour() {
        if (currentContour != null) return;
        currentContour = new Contour();
        add(currentContour);
    }

    //// Basic shapes ////

    public void rect(Rect r) {
        rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Add a rectangle shape to the path. The rectangle will be centered around the x,y coordinates.
     *
     * @param cx     the horizontal center of the rectangle
     * @param cy     the vertical center of the rectangle
     * @param width  the width
     * @param height the height
     */
    public void rect(double cx, double cy, double width, double height) {
        double w2 = width / 2;
        double h2 = height / 2;
        addPoint(cx - w2, cy - h2);
        addPoint(cx + w2, cy - h2);
        addPoint(cx + w2, cy + h2);
        addPoint(cx - w2, cy + h2);
        close();
    }

    public void rect(Rect r, double roundness) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), roundness);
    }

    public void rect(Rect r, double rx, double ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void rect(double cx, double cy, double width, double height, double r) {
        roundedRect(cx, cy, width, height, r);
    }

    public void rect(double cx, double cy, double width, double height, double rx, double ry) {
        roundedRect(cx, cy, width, height, rx, ry);
    }

    public void cornerRect(double x, double y, double width, double height) {
        addPoint(x, y);
        addPoint(x + width, y);
        addPoint(x + width, y + height);
        addPoint(x, y + height);
        close();
    }

    public void cornerRect(Rect r) {
        cornerRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }


    public void cornerRect(Rect r, double roundness) {
        roundedRect(Rect.corneredRect(r), roundness);
    }

    public void cornerRect(Rect r, double rx, double ry) {
        roundedRect(Rect.corneredRect(r), rx, ry);
    }

    public void cornerRect(double cx, double cy, double width, double height, double r) {
        roundedRect(Rect.corneredRect(cx, cy, width, height), r);
    }

    public void cornerRect(double cx, double cy, double width, double height, double rx, double ry) {
        roundedRect(Rect.corneredRect(cx, cy, width, height), rx, ry);
    }

    public void roundedRect(Rect r, double roundness) {
        roundedRect(r, roundness, roundness);
    }

    public void roundedRect(Rect r, double rx, double ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void roundedRect(double cx, double cy, double width, double height, double r) {
        roundedRect(cx, cy, width, height, r, r);
    }

    public void roundedRect(double cx, double cy, double width, double height, double rx, double ry) {
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double dx = rx;
        double dy = ry;

        double left = cx - halfWidth;
        double right = cx + halfWidth;
        double top = cy - halfHeight;
        double bottom = cy + halfHeight;
        // rx/ry cannot be greater than half of the width of the rectangle
        // (required by SVG spec)
        dx = Math.min(dx, width * 0.5);
        dy = Math.min(dy, height * 0.5);
        moveto(left + dx, top);
        if (dx < width * 0.5)
            lineto(right - rx, top);
        curveto(right - dx * ONE_MINUS_QUARTER, top, right, top + dy * ONE_MINUS_QUARTER, right, top + dy);
        if (dy < height * 0.5)
            lineto(right, bottom - dy);
        curveto(right, bottom - dy * ONE_MINUS_QUARTER, right - dx * ONE_MINUS_QUARTER, bottom, right - dx, bottom);
        if (dx < width * 0.5)
            lineto(left + dx, bottom);
        curveto(left + dx * ONE_MINUS_QUARTER, bottom, left, bottom - dy * ONE_MINUS_QUARTER, left, bottom - dy);
        if (dy < height * 0.5)
            lineto(left, top + dy);
        curveto(left, top + dy * ONE_MINUS_QUARTER, left + dx * ONE_MINUS_QUARTER, top, left + dx, top);
        close();
    }


    /**
     * Add an ellipse shape to the path. The ellipse will be centered around the x,y coordinates.
     *
     * @param cx     the horizontal center of the ellipse
     * @param cy     the vertical center of the ellipse
     * @param width  the width
     * @param height the height
     */
    public void ellipse(double cx, double cy, double width, double height) {
        Ellipse2D.Double e = new Ellipse2D.Double(cx - width / 2, cy - height / 2, width, height);
        extend(e);
    }

    public void cornerEllipse(double x, double y, double width, double height) {
        Ellipse2D.Double e = new Ellipse2D.Double(x, y, width, height);
        extend(e);
    }

    public void line(double x1, double y1, double x2, double y2) {
        moveto(x1, y1);
        lineto(x2, y2);
    }

    public void text(Text t) {
        extend(t.getPath());
    }

    //// Container operations ////

    /**
     * Add the given contour. This will also make it active,
     * so all new drawing operations will operate on the given contour.
     * <p/>
     * The given contour is not cloned.
     *
     * @param c the contour to add.
     */
    public void add(Contour c) {
        contours.add(c);
        currentContour = c;
        invalidate(false);
    }

    public int size() {
        return contours.size();
    }

    public boolean isEmpty() {
        return getPointCount() == 0;
    }

    public void clear() {
        contours.clear();
        currentContour = null;
        invalidate(false);
    }

    public void extend(Path p) {
        for (Contour c : p.contours) {
            contours.add(c.clone());
        }
        invalidate(false);
    }

    public void extend(Shape s) {
        PathIterator pi = s.getPathIterator(new AffineTransform());
        double px = 0;
        double py = 0;
        while (!pi.isDone()) {
            double[] points = new double[6];
            int cmd = pi.currentSegment(points);
            if (cmd == PathIterator.SEG_MOVETO) {
                px = points[0];
                py = points[1];
                moveto(px, py);
            } else if (cmd == PathIterator.SEG_LINETO) {
                px = points[0];
                py = points[1];
                lineto(px, py);
            } else if (cmd == PathIterator.SEG_QUADTO) {
                // Convert the quadratic bezier to a cubic bezier.
                double c1x = px + (points[0] - px) * 2 / 3;
                double c1y = py + (points[1] - py) * 2 / 3;
                double c2x = points[0] + (points[2] - points[0]) / 3;
                double c2y = points[1] + (points[3] - points[1]) / 3;
                curveto(c1x, c1y, c2x, c2y, points[2], points[3]);
                px = points[2];
                py = points[3];
            } else if (cmd == PathIterator.SEG_CUBICTO) {
                px = points[4];
                py = points[5];
                curveto(points[0], points[1], points[2], points[3], px, py);
            } else if (cmd == PathIterator.SEG_CLOSE) {
                px = py = 0;
                close();
            } else {
                throw new AssertionError("Unknown path command " + cmd);
            }
            pi.next();
        }
        invalidate(false);
    }

    /**
     * Get the contours of a geometry object.
     * <p/>
     * This method returns live references to the geometric objects.
     * Changing them will change the original geometry.
     *
     * @return a list of contours
     */
    public java.util.List<Contour> getContours() {
        return contours;
    }

    /**
     * Check if the last contour on this path is closed.
     * <p/>
     * A path can't technically be called "closed", only specific contours in the path can.
     * This method provides a reasonable heuristic for a "closed" path by checking the closed state
     * of the last contour. It returns false if this path contains no contours.
     *
     * @return true if the last contour is closed.
     */
    public boolean isClosed() {
        if (isEmpty()) return false;
        Contour lastContour = contours.get(contours.size() - 1);
        return lastContour.isClosed();
    }

    //// Geometric math ////

    /**
     * Returns the length of the line.
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X end coordinate
     * @param y1 Y end coordinate
     * @return the length of the line
     */
    public static double lineLength(double x0, double y0, double x1, double y1) {
        x0 = Math.abs(x0 - x1);
        x0 *= x0;
        y0 = Math.abs(y0 - y1);
        y0 *= y0;
        return Math.sqrt(x0 + y0);
    }

    /**
     * Returns coordinates for point at t on the line.
     * <p/>
     * Calculates the coordinates of x and y for a point
     * at t on a straight line.
     * <p/>
     * The t port is a number between 0.0 and 1.0,
     * x0 and y0 define the starting point of the line,
     * x1 and y1 the ending point of the line,
     *
     * @param t  a number between 0.0 and 1.0 defining the position on the path.
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X end coordinate
     * @param y1 Y end coordinate
     * @return a Point at position t on the line.
     */
    public static Point linePoint(double t, double x0, double y0, double x1, double y1) {
        return new Point(
                x0 + t * (x1 - x0),
                y0 + t * (y1 - y0));
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * linear lines between points at t.
     * <p/>
     * The number of points is defined by n
     * (n=10 would add the lengths of lines between 0.0 and 0.1,
     * between 0.1 and 0.2, and so on).
     * <p/>
     * This will use a default accuracy of 20, which is fine for most cases, usually
     * resulting in a deviation of less than 0.01.
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @return the length of the spline.
     */
    public static double curveLength(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        return curveLength(x0, y0, x1, y1, x2, y2, x3, y3, 20);
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * linear lines between points at t.
     * <p/>
     * The number of points is defined by n
     * (n=10 would add the lengths of lines between 0.0 and 0.1,
     * between 0.1 and 0.2, and so on).
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @param n  accuracy
     * @return the length of the spline.
     */
    public static double curveLength(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, int n) {
        double length = 0;
        double xi = x0;
        double yi = y0;
        double t;
        double px, py;
        double tmpX, tmpY;
        for (int i = 0; i < n; i++) {
            t = (i + 1) / (double) n;
            Point pt = curvePoint(t, x0, y0, x1, y1, x2, y2, x3, y3);
            px = pt.getX();
            py = pt.getY();
            tmpX = Math.abs(xi - px);
            tmpX *= tmpX;
            tmpY = Math.abs(yi - py);
            tmpY *= tmpY;
            length += Math.sqrt(tmpX + tmpY);
            xi = px;
            yi = py;
        }
        return length;
    }

    /**
     * Returns coordinates for point at t on the spline.
     * <p/>
     * Calculates the coordinates of x and y for a point
     * at t on the cubic bezier spline, and its control points,
     * based on the de Casteljau interpolation algorithm.
     *
     * @param t  a number between 0.0 and 1.0 defining the position on the path.
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @return a Point at position t on the spline.
     */
    public static Point curvePoint(double t, double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        double mint = 1 - t;
        double x01 = x0 * mint + x1 * t;
        double y01 = y0 * mint + y1 * t;
        double x12 = x1 * mint + x2 * t;
        double y12 = y1 * mint + y2 * t;
        double x23 = x2 * mint + x3 * t;
        double y23 = y2 * mint + y3 * t;

        double out_c1x = x01 * mint + x12 * t;
        double out_c1y = y01 * mint + y12 * t;
        double out_c2x = x12 * mint + x23 * t;
        double out_c2y = y12 * mint + y23 * t;
        double out_x = out_c1x * mint + out_c2x * t;
        double out_y = out_c1y * mint + out_c2y * t;
        return new Point(out_x, out_y);
    }

    /**
     * Calculate the length of the path. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the path.
     */
    public double getLength() {
        if (lengthDirty) {
            updateContourLengths();
        }
        return pathLength;
    }

    private void updateContourLengths() {
        contourLengths = new ArrayList<Double>(contours.size());
        pathLength = 0;
        double length;
        for (Contour c : contours) {
            length = c.getLength();
            contourLengths.add(length);
            pathLength += length;
        }
        lengthDirty = false;
    }

    public Contour contourAt(double t) {
        // Since t is relative, convert it to the absolute length.
        double absT = t * getLength();

        // Find the contour that contains t.
        double cLength;
        for (Contour c : contours) {
            cLength = c.getLength();
            if (absT <= cLength) return c;
            absT -= cLength;
        }
        return null;
    }

    /**
     * Returns coordinates for point at t on the path.
     * <p/>
     * Gets the length of the path, based on the length
     * of each curve and line in the path.
     * Determines in what segment t falls.
     * Gets the point on that segment.
     *
     * @param t relative coordinate of the point (between 0.0 and 1.0)
     *          Results outside of this range are undefined.
     * @return coordinates for point at t.
     */
    public Point pointAt(double t) {
        double length = getLength();
        // Since t is relative, convert it to the absolute length.
        double absT = t * length;
        // The resT is what remains of t after we traversed all segments.
        double resT = t;
        // Find the contour that contains t.
        double cLength;
        Contour currentContour = null;
        for (Contour c : contours) {
            currentContour = c;
            cLength = c.getLength();
            if (absT <= cLength) break;
            absT -= cLength;
            resT -= cLength / length;
        }
        if (currentContour == null) return new Point();
        resT /= (currentContour.getLength() / length);
        return currentContour.pointAt(resT);
    }

    /**
     * Same as pointAt(t).
     * <p/>
     * This method is here for compatibility with NodeBox 1.
     *
     * @param t relative coordinate of the point.
     * @return coordinates for point at t.
     * @see #pointAt(double)
     */
    public Point point(double t) {
        return pointAt(t);
    }

    //// Geometric operations ////

    /**
     * Make new points along the contours of the existing path.
     * <p/>
     * Points are evenly distributed according to the length of each contour.
     *
     * @param amount     the number of points to create.
     * @param perContour if true, the amount of points is generated per contour, otherwise the amount
     *                   is for the entire path.
     * @return a list of Points.
     */
    public Point[] makePoints(int amount, boolean perContour) {
        if (perContour) {
            Point[] points = new Point[amount * contours.size()];
            int index = 0;
            for (Contour c : contours) {
                Point[] pointsFromContour = c.makePoints(amount);
                System.arraycopy(pointsFromContour, 0, points, index, amount);
                index += amount;
            }
            return points;
        } else {
            // Distribute all points evenly along the combined length of the contours.
            double delta = pointDelta(amount, isClosed());
            Point[] points = new Point[amount];
            for (int i = 0; i < amount; i++) {
                points[i] = pointAt(delta * i);
            }
            return points;
        }
    }

    public Path resampleByAmount(int amount, boolean perContour) {
        if (perContour) {
            Path p = cloneAndClear();
            for (Contour c : contours) {
                p.add(c.resampleByAmount(amount));
            }
            return p;
        } else {
            Path p = cloneAndClear();
            double delta = pointDelta(amount, isClosed());
            for (int i = 0; i < amount; i++) {
                p.addPoint(pointAt(delta * i));
            }
            if (isClosed()) p.close();
            return p;
        }
    }

    public Path resampleByLength(double segmentLength) {
        Path p = cloneAndClear();
        for (Contour c : contours) {
            p.add(c.resampleByLength(segmentLength));
        }
        return p;
    }

    public static Path findPath(java.util.List<Point> points) {
        Point[] pts = new Point[points.size()];
        points.toArray(pts);
        return findPath(pts, 1);
    }

    public static Path findPath(java.util.List<Point> points, double curvature) {
        Point[] pts = new Point[points.size()];
        points.toArray(pts);
        return findPath(pts, curvature);
    }

    public static Path findPath(Point[] points) {
        return findPath(points, 1);
    }

    /**
     * Constructs a path between the given list of points.
     * </p>
     * Interpolates the list of points and determines
     * a smooth bezier path betweem them.
     * Curvature is only useful if the path has more than  three points.
     * </p>
     *
     * @param points    the points of which to construct the path from.
     * @param curvature the smoothness of the generated path (0: straight, 1: smooth)
     * @return a new Path.
     */
    public static Path findPath(Point[] points, double curvature) {
        if (points.length == 0) return null;
        if (points.length == 1) {
            Path path = new Path();
            path.moveto(points[0].x, points[0].y);
            return path;
        }
        if (points.length == 2) {
            Path path = new Path();
            path.moveto(points[0].x, points[0].y);
            path.lineto(points[1].x, points[1].y);
            return path;
        }

        // Zero curvature means straight lines.

        curvature = Math.max(0, Math.min(1, curvature));
        if (curvature == 0) {
            Path path = new Path();
            path.moveto(points[0].x, points[0].y);
            for (Point point : points) path.lineto(point.x, point.y);
            return path;
        }

        curvature = 4 + (1.0 - curvature) * 40;

        HashMap<Integer, Double> dx, dy, bi, ax, ay;
        dx = new HashMap<Integer, Double>();
        dy = new HashMap<Integer, Double>();
        bi = new HashMap<Integer, Double>();
        ax = new HashMap<Integer, Double>();
        ay = new HashMap<Integer, Double>();
        dx.put(0, 0.0);
        dx.put(points.length - 1, 0.0);
        dy.put(0, 0.0);
        dy.put(points.length - 1, 0.0);
        bi.put(1, -0.25);
        ax.put(1, (points[2].x - points[0].x - dx.get(0)) / 4);
        ay.put(1, (points[2].y - points[0].y - dy.get(0)) / 4);

        for (int i = 2; i < points.length - 1; i++) {
            bi.put(i, -1 / (curvature + bi.get(i - 1)));
            ax.put(i, -(points[i + 1].x - points[i - 1].x - ax.get(i - 1)) * bi.get(i));
            ay.put(i, -(points[i + 1].y - points[i - 1].y - ay.get(i - 1)) * bi.get(i));
        }

        for (int i = points.length - 2; i >= 1; i--) {
            dx.put(i, ax.get(i) + dx.get(i + 1) * bi.get(i));
            dy.put(i, ay.get(i) + dy.get(i + 1) * bi.get(i));
        }

        Path path = new Path();
        path.moveto(points[0].x, points[0].y);
        for (int i = 0; i < points.length - 1; i++) {
            path.curveto(points[i].x + dx.get(i),
                    points[i].y + dy.get(i),
                    points[i + 1].x - dx.get(i + 1),
                    points[i + 1].y - dy.get(i + 1),
                    points[i + 1].x,
                    points[i + 1].y);
        }

        return path;
    }


    //// Geometric queries ////

    public boolean contains(Point p) {
        return getGeneralPath().contains(p.toPoint2D());
    }

    public boolean contains(double x, double y) {
        return getGeneralPath().contains(x, y);
    }

    public boolean contains(Rect r) {
        return getGeneralPath().contains(r.getRectangle2D());
    }

    //// Boolean operations ////

    public boolean intersects(Rect r) {
        return getGeneralPath().intersects(r.getRectangle2D());
    }

    public boolean intersects(Path p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.intersect(a2);
        return !a1.isEmpty();
    }

    public Path intersected(Path p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.intersect(a2);
        return new Path(a1);
    }

    public Path subtracted(Path p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.subtract(a2);
        return new Path(a1);
    }

    public Path united(Path p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.add(a2);
        return new Path(a1);
    }

    //// Path ////

    public java.awt.geom.GeneralPath getGeneralPath() {
        if (!pathDirty) return awtPath;
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO, getPointCount());
        for (Contour c : contours) {
            c._extendPath(gp);
        }
        awtPath = gp;
        pathDirty = false;
        return gp;
    }

    public Rect getBounds() {
        if (!pathDirty && bounds != null) return bounds;
        if (isEmpty()) {
            bounds = new Rect();
        } else {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double px, py;
            ArrayList<Point> points = (ArrayList<Point>) getPoints();
            for (int i = 0; i < getPointCount(); i++) {
                Point p = points.get(i);
                if (p.getType() == Point.LINE_TO) {
                    px = p.getX();
                    py = p.getY();
                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;
                } else if (p.getType() == Point.CURVE_TO) {
                    Bezier b = new Bezier(points.get(i - 3), points.get(i - 2), points.get(i - 1), p);
                    Rect r = b.extrema();
                    double right = r.getX() + r.getWidth();
                    double bottom = r.getY() + r.getHeight();
                    if (r.getX() < minX) minX = r.getX();
                    if (right > maxX) maxX = right;
                    if (r.getY() < minY) minY = r.getY();
                    if (bottom > maxY) maxY = bottom;
                }
            }
            bounds = new Rect(minX, minY, maxX - minX, maxY - minY);
        }
        return bounds;
    }

    //// Transformations ////

    public void transform(Transform t) {
        for (Contour c : contours) {
            c.setPoints(t.map(c.getPoints()));
        }
        invalidate(true);
    }

    //// Path math ////

    /**
     * Flatten the geometry.
     */
    public void flatten() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Make a flattened copy of the geometry.
     *
     * @return a flattened copy.
     */
    public Path flattened() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    //// Operations on the current context. ////

    public void draw(Graphics2D g) {
        // If we can't fill or stroke the path, there's nothing to draw.
        if (fillColor == null && strokeColor == null) return;
        GeneralPath gp = getGeneralPath();
        // If there are no points, there's nothing to draw.
        if (getPointCount() == 0) return;
        if (fillColor != null) {
            fillColor.set(g);
            g.fill(gp);
        }
        if (strokeWidth > 0 && strokeColor != null) {
            try {
                strokeColor.set(g);
                g.setStroke(new BasicStroke((float) strokeWidth));
                g.draw(gp);
            } catch (Exception e) {
                // Invalid transformations can cause the pen to not display.
                // Catch the exception and throw it away.
                // The path would be too small to be displayed anyway.
            }
        }
    }

    public Path clone() {
        return new Path(this);
    }

    public Path cloneAndClear() {
        return new Path(this, false);
    }

    //// Functional operations ////

    public AbstractGeometry mapPoints(Function<Point, Point> pointFunction) {
        Path newPath = this.cloneAndClear();
        for (Contour c : getContours()) {
            Contour newContour = (Contour) c.mapPoints(pointFunction);
            newPath.add(newContour);
        }
        return newPath;
    }

    //// Iterator implementation

    public Iterator<Point> iterator() {
        return getPoints().iterator();
    }


    private class Bezier {
        private double x1, y1, x2, y2, x3, y3, x4, y4;
        private double minx, maxx, miny, maxy;

        public Bezier(Point p1, Point p2, Point p3, Point p4) {
            x1 = p1.getX();
            y1 = p1.getY();
            x2 = p2.getX();
            y2 = p2.getY();
            x3 = p3.getX();
            y3 = p3.getY();
            x4 = p4.getX();
            y4 = p4.getY();
        }

        private boolean fuzzyCompare(double p1, double p2) {
            return Math.abs(p1 - p2) <= (0.000000000001 * Math.min(Math.abs(p1), Math.abs(p2)));
        }

        public Point pointAt(double t) {
            double coeff[], a, b, c, d;
            coeff = coefficients(t);
            a = coeff[0];
            b = coeff[1];
            c = coeff[2];
            d = coeff[3];
            return new Point(a * x1 + b * x2 + c * x3 + d * x4, a * y1 + b * y2 + c * y3 + d * y4);
        }

        private double[] coefficients(double t) {
            double m_t, a, b, c, d;
            m_t = 1 - t;
            b = m_t * m_t;
            c = t * t;
            d = c * t;
            a = b * m_t;
            b *= (3. * t);
            c *= (3. * m_t);
            return new double[]{a, b, c, d};
        }

        private void bezierCheck(double t) {
            if (t >= 0 && t <= 1) {
                Point p = pointAt(t);
                if (p.getX() < minx) minx = p.getX();
                else if (p.getX() > maxx) maxx = p.getX();
                if (p.getY() < miny) miny = p.getY();
                else if (p.getY() > maxy) maxy = p.getY();
            }
        }

        public Rect extrema() {
            double ax, bx, cx, ay, by, cy;

            if (x1 < x4) {
                minx = x1;
                maxx = x4;
            } else {
                minx = x4;
                maxx = x1;
            }
            if (y1 < y4) {
                miny = y1;
                maxy = y4;
            } else {
                miny = y4;
                maxy = y1;
            }

            ax = 3 * (-x1 + 3 * x2 - 3 * x3 + x4);
            bx = 6 * (x1 - 2 * x2 + x3);
            cx = 3 * (-x1 + x2);

            if (fuzzyCompare(ax + 1, 1)) {
                if (!fuzzyCompare(bx + 1, 1)) {
                    double t = -cx / bx;
                    bezierCheck(t);
                }
            } else {
                double tx = bx * bx - 4 * ax * cx;
                if (tx >= 0) {
                    double temp, rcp, t1, t2;
                    temp = (double) Math.sqrt(tx);
                    rcp = 1 / (2 * ax);
                    t1 = (-bx + temp) * rcp;
                    bezierCheck(t1);

                    t2 = (-bx - temp) * rcp;
                    bezierCheck(t2);
                }
            }

            ay = 3 * (-y1 + 3 * y2 - 3 * y3 + y4);
            by = 6 * (y1 - 2 * y2 + y3);
            cy = 3 * (-y1 + y2);

            if (fuzzyCompare(ay + 1, 1)) {
                if (!fuzzyCompare(by + 1, 1)) {
                    double t = -cy / by;
                    bezierCheck(t);
                }
            } else {
                double ty = by * by - 4 * ay * cy;
                if (ty > 0) {
                    double temp, rcp, t1, t2;
                    temp = (double) Math.sqrt(ty);
                    rcp = 1 / (2 * ay);
                    t1 = (-by + temp) * rcp;
                    bezierCheck(t1);

                    t2 = (-by - temp) * rcp;
                    bezierCheck(t2);
                }
            }

            return new Rect(minx, miny, maxx - minx, maxy - miny);
        }
    }

    @Override
    public String toString() {
        return "<Path>";
    }

}
