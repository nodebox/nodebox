package nodebox.graphics;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

/**
 * Base class for all geometric (vector) data.
 */
public class Path extends AbstractGeometry implements Colorizable {

    // Simulate a quarter of a circle.
    private static final float ONE_MINUS_QUARTER = 1.0f - 0.552f;

    private Color fillColor = null;
    private Color strokeColor = null;
    private float strokeWidth = 1f;
    private ArrayList<Contour> contours;
    private transient Contour currentContour = null;
    private transient boolean pathDirty = true;
    private transient boolean lengthDirty = true;
    private transient java.awt.geom.GeneralPath awtPath;
    private transient Rect bounds;
    private transient ArrayList<Float> contourLengths;
    private transient float pathLength = -1;

    public Path() {
        fillColor = new Color();
        strokeColor = null;
        strokeWidth = 1f;
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
            // Set the current contour to the last contour.
            currentContour = contours.get(contours.size() - 1);
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

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setFill(Color c) {
        setFillColor(c);
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStroke(Color c) {
        setStrokeColor(c);
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
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

    public void moveto(float x, float y) {
        // Stop using the current contour. addPoint will automatically create a new contour.
        currentContour = null;
        addPoint(x, y);
    }

    public void lineto(float x, float y) {
        if (currentContour == null)
            throw new RuntimeException("Lineto without moveto first.");
        addPoint(x, y);
    }

    public void curveto(float x1, float y1, float x2, float y2, float x3, float y3) {
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

    public void addPoint(float x, float y) {
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
    public void rect(float cx, float cy, float width, float height) {
        float w2 = width / 2;
        float h2 = height / 2;
        addPoint(cx - w2, cy - h2);
        addPoint(cx + w2, cy - h2);
        addPoint(cx + w2, cy + h2);
        addPoint(cx - w2, cy + h2);
        close();
    }

    public void rect(Rect r, float roundness) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), roundness);
    }

    public void rect(Rect r, float rx, float ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void rect(float cx, float cy, float width, float height, float r) {
        roundedRect(cx, cy, width, height, r);
    }

    public void rect(float cx, float cy, float width, float height, float rx, float ry) {
        roundedRect(cx, cy, width, height, rx, ry);
    }

    public void roundedRect(Rect r, float roundness) {
        roundedRect(r, roundness, roundness);
    }

    public void roundedRect(Rect r, float rx, float ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void roundedRect(float cx, float cy, float width, float height, float r) {
        roundedRect(cx, cy, width, height, r, r);
    }

    public void roundedRect(float cx, float cy, float width, float height, float rx, float ry) {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float dx = rx;
        float dy = ry;

        float left = cx - halfWidth;
        float right = cx + halfWidth;
        float top = cy - halfHeight;
        float bottom = cy + halfHeight;
        // rx/ry cannot be greater than half of the width of the retoctangle
        // (required by SVG spec)
        dx = Math.min(dx, width * 0.5f);
        dy = Math.min(dy, height * 0.5f);
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
    public void ellipse(float cx, float cy, float width, float height) {
        Ellipse2D.Float e = new Ellipse2D.Float(cx - width / 2, cy - height / 2, width, height);
        extend(e);
    }

    public void line(float x1, float y1, float x2, float y2) {
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
        return contours.isEmpty();
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
        float px = 0;
        float py = 0;
        while (!pi.isDone()) {
            float[] points = new float[6];
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
                float c1x = px + (points[0] - px) * 2f / 3f;
                float c1y = py + (points[1] - py) * 2f / 3f;
                float c2x = points[0] + (points[2] - points[0]) / 3f;
                float c2y = points[1] + (points[3] - points[1]) / 3f;
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
    public static float lineLength(float x0, float y0, float x1, float y1) {
        x0 = Math.abs(x0 - x1);
        x0 *= x0;
        y0 = Math.abs(y0 - y1);
        y0 *= y0;
        return (float) Math.sqrt(x0 + y0);
    }

    /**
     * Returns coordinates for point at t on the line.
     * <p/>
     * Calculates the coordinates of x and y for a point
     * at t on a straight line.
     * <p/>
     * The t parameter is a number between 0.0 and 1.0,
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
    public static Point linePoint(float t, float x0, float y0, float x1, float y1) {
        return new Point(
                x0 + t * (x1 - x0),
                y0 + t * (y1 - y0));
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * lineair lines between points at t.
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
    public static float curveLength(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        return curveLength(x0, y0, x1, y1, x2, y2, x3, y3, 20);
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * lineair lines between points at t.
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
    public static float curveLength(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int n) {
        float length = 0;
        float xi = x0;
        float yi = y0;
        float t;
        float px, py;
        float tmpX, tmpY;
        for (int i = 0; i < n; i++) {
            t = (i + 1) / (float) n;
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
    public static Point curvePoint(float t, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        float mint = 1 - t;
        float x01 = x0 * mint + x1 * t;
        float y01 = y0 * mint + y1 * t;
        float x12 = x1 * mint + x2 * t;
        float y12 = y1 * mint + y2 * t;
        float x23 = x2 * mint + x3 * t;
        float y23 = y2 * mint + y3 * t;

        float out_c1x = x01 * mint + x12 * t;
        float out_c1y = y01 * mint + y12 * t;
        float out_c2x = x12 * mint + x23 * t;
        float out_c2y = y12 * mint + y23 * t;
        float out_x = out_c1x * mint + out_c2x * t;
        float out_y = out_c1y * mint + out_c2y * t;
        return new Point(out_x, out_y);
    }

    /**
     * Calculate the length of the path. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the path.
     */
    public float getLength() {
        if (lengthDirty) {
            updateContourLengths();
        }
        return pathLength;
    }

    private void updateContourLengths() {
        contourLengths = new ArrayList<Float>(contours.size());
        pathLength = 0;
        float length;
        for (Contour c : contours) {
            length = c.getLength();
            contourLengths.add(length);
            pathLength += length;
        }
        lengthDirty = false;
    }

    public Contour contourAt(float t) {
        // Since t is relative, convert it to the absolute length.
        float absT = t * getLength();

        // Find the contour that contains t.
        float cLength;
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
    public Point pointAt(float t) {
        float length = getLength();
        // Since t is relative, convert it to the absolute length.
        float absT = t * length;
        // The resT is what remains of t after we traversed all segments.
        float resT = t;
        // Find the contour that contains t.
        float cLength;
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
            float delta = pointDelta(amount, isClosed());
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
            float delta = pointDelta(amount, isClosed());
            for (int i = 0; i < amount; i++) {
                p.addPoint(pointAt(delta * i));
            }
            return p;
        }
    }

    public Path resampleByLength(float segmentLength) {
        Path p = cloneAndClear();
        for (Contour c : contours) {
            p.add(c.resampleByLength(segmentLength));
        }
        return p;
    }

    //// Geometric queries ////

    public boolean contains(Point p) {
        return getGeneralPath().contains(p.getPoint2D());
    }

    public boolean contains(float x, float y) {
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
        if (contours.size() == 0) {
            bounds = new Rect();
        } else {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float px, py;
            for (Contour c : contours) {
                for (Point p : c.getPoints()) {
                    px = p.getX();
                    py = p.getY();
                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;
                }
            }
            bounds = new Rect(minX, minY, maxX - minX, maxY - minY);
        }
        return bounds;
    }

    //// Transformations ////

    public void transform(Transform t) {
        t.map(getPoints());
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
        if (fillColor != null) {
            g.setColor(fillColor.getAwtColor());
            g.fill(gp);
        }
        if (strokeWidth > 0 && strokeColor != null) {
            try {
                g.setColor(strokeColor.getAwtColor());
                g.setStroke(new BasicStroke(strokeWidth));
                g.draw(gp);
            } catch (Exception e) {
                // Invalid transformations can cause the pen to not display.
                // Catch the exception and throw it away.
                // The path would be too small to be displayed anyway.
            }
        }
    }

    public void inheritFromContext(GraphicsContext ctx) {
    }

    public Path clone() {
        return new Path(this);
    }

    public Path cloneAndClear() {
        return new Path(this, false);
    }

}
