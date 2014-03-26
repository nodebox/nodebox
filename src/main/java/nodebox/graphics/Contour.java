package nodebox.graphics;

import com.google.common.base.Function;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class Contour extends AbstractGeometry {

    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1);
    private static final int SEGMENT_ACCURACY = 20;

    private List<Point> points;
    private boolean closed;
    private transient ArrayList<Double> segmentLengths;
    private transient double length = -1;

    public Contour() {
        points = new ArrayList<Point>();
        closed = false;
    }

    public Contour(Contour other) {
        points = new ArrayList<Point>(other.points.size());
        for (Point p : other.points) {
            points.add(p);
        }
        closed = other.closed;
    }

    public Contour(Iterable<Point> points, boolean closed) {
        this.points = new ArrayList<Point>();
        for (Point p : points) {
            this.points.add(p);
        }
        this.closed = closed;
    }

    //// Point operations ////

    public int getPointCount() {
        return points.size();
    }

    public java.util.List<Point> getPoints() {
        return points;
    }

    void setPoints(List<Point> points) {
        this.points = points;
    }

    public void addPoint(Point pt) {
        points.add(pt);
        invalidate();
    }

    public void addPoint(double x, double y) {
        points.add(new Point(x, y));
        invalidate();
    }

    //// Close ////

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
        invalidate();
    }

    public void close() {
        this.closed = true;
        invalidate();
    }

    //// Geometric queries ////

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public Rect getBounds() {
        if (points.isEmpty()) {
            return new Rect();
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double px, py;
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

    /**
     * Invalidates the cache. Querying the contour length or calling makePoints/resample will an up-to-date result.
     * <p/>
     * Cache invalidation happens automatically when using the Contour methods, such as addPoint/close. You should
     * invalidate the cache only after manually changing the point positions.
     * <p/>
     * Invalidating the cache is a lightweight operation; it doesn't recalculate anything. Only when querying the
     * new length will the values be recalculated.
     */

    public void invalidate() {
        segmentLengths = null;
    }

    public double updateSegmentLengths() {
        java.util.List<Point> points = getPoints();
        segmentLengths = new ArrayList<Double>();
        double totalLength = 0;

        // We cannot form a line or curve with the first point.
        // Since the algorithm looks back at previous points, we
        // start looking from the first useful point, which is
        // the second (index of 1).
        for (int pi = 1; pi < points.size(); pi++) {
            Point pt = points.get(pi);
            if (pt.isLineTo()) {
                Point pt0 = points.get(pi - 1);
                double length = Path.lineLength(pt0.x, pt0.y, pt.x, pt.y);
                segmentLengths.add(length);
                totalLength += length;
            } else if (pt.isCurveTo()) {
                Point pt0 = points.get(pi - 3);
                Point c1 = points.get(pi - 2);
                Point c2 = points.get(pi - 1);
                double length = Path.curveLength(pt0.x, pt0.y,
                        c1.x, c1.y,
                        c2.x, c2.y,
                        pt.x, pt.y, SEGMENT_ACCURACY);
                segmentLengths.add(length);
                totalLength += length;
            }
        }
        // If the path is closed, add the closing segment.
        if (closed && !points.isEmpty()) {
            Point pt0 = points.get(points.size() - 1);
            Point pt1 = points.get(0);
            double length = Path.lineLength(pt0.x, pt0.y, pt1.x, pt1.y);
            segmentLengths.add(length);
            totalLength += length;
        }

        this.length = totalLength;
        return totalLength;
    }

    /**
     * Calculate the length of the contour. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the contour
     */
    public double getLength() {
        if (segmentLengths == null)
            updateSegmentLengths();
        assert (length != -1);
        return length;
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
        if (segmentLengths == null)
            updateSegmentLengths();

        // Check if there is a path.
        if (points.isEmpty())
            throw new NodeBoxError("The path is empty.");

        // If the path has no length, return the position of the first point.
        if (length == 0)
            return points.get(0);

        // Since t is relative, convert it to the absolute length.
        double absT = t * length;
        // The resT is what remains of t after we traversed all segments.
        double resT = t;

        // Find the segment that contains t.
        int segnum = -1;
        for (Double seglength : segmentLengths) {
            segnum++;
            if (absT <= seglength || segnum == segmentLengths.size() - 1)
                break;
            absT -= seglength;
            resT -= seglength / length;
        }
        resT /= (segmentLengths.get(segnum) / length);

        // Find the point index for the segment.
        int pi = pointIndexForSegment(segnum + 1);
        Point pt1 = points.get(pi);
        // If the path is closed, the point index is set to zero.
        // Set the index to the last point to get the one-but-last point for pt0. 
        if (pi == 0) {
            pi = points.size();
        }

        if (pt1.isLineTo()) {
            Point pt0 = points.get(pi - 1);
            return Path.linePoint(resT, pt0.x, pt0.y, pt1.x, pt1.y);
        } else if (pt1.isCurveTo()) {
            Point pt0 = points.get(pi - 3);
            Point c1 = points.get(pi - 2);
            Point c2 = points.get(pi - 1);
            return Path.curvePoint(resT,
                    pt0.x, pt0.y,
                    c1.x, c1.y,
                    c2.x, c2.y,
                    pt1.x, pt1.y);
        } else {
            throw new AssertionError("Incorrect point.");
        }
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

    /**
     * Calculate the point index for the segment number. Segments lie between points. The first point of the segment
     * is returned. The point index will be a valid index, even if the segment number doesn't exist.
     *
     * @param segnum the segment index
     * @return the index of the point.
     */
    private int pointIndexForSegment(int segnum) {
        int pointIndex = 0;
        for (Point pt : points) {
            if (pt.isCurveTo() || pt.isLineTo()) {
                if (segnum == 0) break;
                segnum--;
            }
            pointIndex++;
        }
        int pointCount = points.size();
        if (pointIndex < pointCount) {
            return pointIndex;
        } else if (closed) {
            return 0;
        } else {
            return pointCount - 1;
        }
    }

    //// Geometric operations ////

    /**
     * Make new points along the contours of the existing path.
     *
     * @param amount the number of points to create.
     * @return a list with "amount" points or zero points if the contour is empty.
     */
    public Point[] makePoints(int amount) {
        // If the contour is empty, pointAt will fail. Return an empty array.
        if (points.isEmpty()) return new Point[0];
        Point[] points = new Point[amount];
        double delta = 1;
        if (closed) {
            if (amount > 0) {
                delta = 1.0 / amount;
            }
        } else {
            // The delta value is divided by amount - 1, because we also want the last point (t=1.0)
            // If I wouldn't use amount - 1, I fall one point short of the end.
            // E.g. if amount = 4, I want point at t 0.0, 0.33, 0.66 and 1.0,
            // if amount = 2, I want point at t 0.0 and t 1.0
            if (amount > 2) {
                delta = 1.0 / (amount - 1.0);
            }
        }
        for (int i = 0; i < amount; i++) {
            points[i] = pointAt(delta * i);
        }
        return points;
    }

    /**
     * Make new points along the contours of the existing path.
     *
     * @param amount     the amount of points to distribute.
     * @param perContour this port was added to comply with the IGeometry interface, but is ignored since
     *                   we're at the contour level.
     * @return a list with "amount" points or zero points if the contour is empty.
     */
    public Point[] makePoints(int amount, boolean perContour) {
        return makePoints(amount);
    }

    /**
     * Generate new geometry with the given amount of points along the shape of the original geometry.
     * <p/>
     * The length of each segment is not given and will be determined based on the required number of points.
     *
     * @param amount     the number of points to generate.
     * @param perContour this port is ignored since we're at the contour level.
     * @return a new Contour with the given number of points.
     */
    public Contour resampleByAmount(int amount, boolean perContour) {
        return resampleByAmount(amount);
    }


    /**
     * Generate new geometry with the given amount of points along the shape of the original geometry.
     * <p/>
     * The length of each segment is not given and will be determined based on the required number of points.
     *
     * @param amount the number of points to generate.
     * @return a new Contour with the given number of points.
     */
    public Contour resampleByAmount(int amount) {
        Contour c = new Contour();
        c.extend(makePoints(amount));
        c.setClosed(closed);
        return c;
    }

    /**
     * Generate new geometry with points along the shape of the original geometry, spaced at the given length.
     * <p/>
     * The number of points is not given and will be determined by the system based on the segment length.
     * Note that the last segment may be shorter than the given segment length.
     *
     * @param segmentLength the maximum length of each resampled segment.
     * @return a new Contour with segments of the given length.
     */
    public Contour resampleByLength(double segmentLength) {
        if (segmentLength <= 0.0000001) {
            throw new IllegalArgumentException("Segment length must be greater than zero.");
        }
        double contourLength = getLength();
        int amount = (int) Math.ceil(contourLength / segmentLength);
        if (closed) {
            return resampleByAmount(amount);
        } else {
            return resampleByAmount(amount + 1);
        }
    }

    public void flatten() {
        throw new UnsupportedOperationException();
    }

    public IGeometry flattened() {
        throw new UnsupportedOperationException();
    }

    //// Graphics ////

    public void draw(Graphics2D g) {
        if (getPointCount() < 2) return;
        // Since a contour has no fill or stroke information, draw it in black.
        // We save the current color so as not to disrupt the context.
        java.awt.Color savedColor = g.getColor();
        Stroke savedStroke = g.getStroke();
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, getPointCount());
        _extendPath(gp);
        g.setColor(java.awt.Color.BLACK);
        g.setStroke(DEFAULT_STROKE);
        g.draw(gp);
        g.setColor(savedColor);
        g.setStroke(savedStroke);
    }

    /* package private */

    void _extendPath(GeneralPath gp) {
        if (points.size() == 0) return;
        Point pt = points.get(0);
        Point ctrl1, ctrl2;
        gp.moveTo(pt.x, pt.y);
        int pointCount = getPointCount();
        for (int i = 1; i < pointCount; i++) {
            pt = points.get(i);
            if (pt.isLineTo()) {
                gp.lineTo(pt.x, pt.y);
            } else if (pt.isCurveTo()) {
                ctrl1 = points.get(i - 2);
                ctrl2 = points.get(i - 1);
                gp.curveTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, pt.x, pt.y);
            }
        }
        if (closed)
            gp.closePath();
    }

    public void transform(Transform t) {
        this.points = t.map(getPoints());
        invalidate();
    }

    //// Conversions ////

    public Path toPath() {
        return new Path(this);
    }

    //// Functional operations ////

    public AbstractGeometry mapPoints(Function<Point, Point> pointFunction) {
        Contour c = new Contour();
        c.setClosed(isClosed());
        for (Point point : getPoints()) {
            Point newPoint = pointFunction.apply(point);
            c.addPoint(newPoint);
        }
        return c;
    }

    //// Object operations ////

    public Contour clone() {
        return new Contour(this);
    }

}
