package nodebox.graphics;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.ArrayList;

public class Contour implements IGeometry {

    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1f);
    private static final int SEGMENT_ACCURACY = 20;

    private ArrayList<Point> points;
    private transient ArrayList<Float> segmentLengths;
    private transient float length = -1;

    public Contour() {
        points = new ArrayList<Point>();
    }

    public Contour(Contour other) {
        points = new ArrayList<Point>(other.points.size());
        for (Point p : other.points) {
            points.add(p.clone());
        }
    }

    //// Point operations ////

    public int getPointCount() {
        return points.size();
    }

    public java.util.List<Point> getPoints() {
        return points;
    }

    public void addPoint(Point pt) {
        points.add(pt.clone());
    }

    public void addPoint(float x, float y) {
        points.add(new Point(x, y));
    }

    //// Geometric queries ////

    public Rect getBounds() {
        if (points.size() == 0) {
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

    public float updateSegmentLengths() {
        java.util.List<Point> points = getPoints();
        segmentLengths = new ArrayList<Float>();
        float totalLength = 0;

        // We cannot form a line or curve with the first point.
        // Since the algorithm looks back at previous points, we
        // start looking from the first useful point, which is
        // the second (index of 1).
        for (int pi = 1; pi < points.size(); pi++) {
            Point pt = points.get(pi);
            if (pt.isLineTo()) {
                Point pt0 = points.get(pi - 1);
                float length = Path.lineLength(pt0.x, pt0.y, pt.x, pt.y);
                segmentLengths.add(length);
                totalLength += length;
            } else if (pt.isCurveTo()) {
                Point pt0 = points.get(pi - 3);
                Point c1 = points.get(pi - 2);
                Point c2 = points.get(pi - 1);
                float length = Path.curveLength(pt0.x, pt0.y,
                        c1.x, c1.y,
                        c2.x, c2.y,
                        pt.x, pt.y, SEGMENT_ACCURACY);
                segmentLengths.add(length);
                totalLength += length;
            }
        }

        this.length = totalLength;
        return totalLength;
    }

    /**
     * Calculate the length of the contour. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the contour
     */
    public float getLength() {
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
     * @return coordinates for point at t.
     */
    public Point pointAt(float t) {
        if (segmentLengths == null)
            updateSegmentLengths();

        // Check if there is a path.
        if (length <= 0)
            throw new NodeBoxError("The path is empty.");

        // Since t is relative, convert it to the absolute length.
        float absT = t * length;
        // The resT is what remains of t after we traversed all segments.
        float resT = t;

        // Find the segment that contains t.
        int segnum = -1;
        for (Float seglength : segmentLengths) {
            segnum++;
            if (absT <= seglength || segnum == segmentLengths.size()-1)
                break;
            absT -= seglength;
            resT -= seglength / length;
        }
        resT /= (segmentLengths.get(segnum) / length);

        // Find the point index for the segment.
        int pi = pointIndexForSegment(segnum + 1);
        Point pt1 = points.get(pi);

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
        return pointIndex < points.size() ? pointIndex : points.size() - 1;
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
        if (getPointCount() < 2) return;
        // Since a contour has no fill or stroke information, draw it in black.
        // We save the current color so as not to disrupt the context.
        java.awt.Color savedColor = g.getColor();
        Stroke savedStroke = g.getStroke();
        GeneralPath gp = new GeneralPath(Path2D.WIND_EVEN_ODD, getPointCount());
        _extendPath(gp);
        g.setColor(java.awt.Color.BLACK);
        g.setStroke(DEFAULT_STROKE);
        g.draw(gp);
        g.setColor(savedColor);
        g.setStroke(savedStroke);
    }

    /* package private */ void _extendPath(GeneralPath gp) {
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
