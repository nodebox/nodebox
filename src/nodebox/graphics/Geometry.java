package nodebox.graphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Geometry extends AbstractGeometry implements Colorizable {

    private ArrayList<Path> paths;
    private Path currentPath;
    private boolean lengthDirty = true;
    private ArrayList<Float> pathLengths;
    private float groupLength;

    public Geometry() {
        paths = new ArrayList<Path>();
        currentPath = null;
    }

    public Geometry(Geometry other) {
        paths = new ArrayList<Path>(other.paths.size());
        for (Path path : other.paths) {
            paths.add(path.clone());
        }
        // TODO: We might want to refer to the latest Path object in the items.
        currentPath = null;
    }

    //// Container operations ////

    /**
     * Get the subshapes of a geometry object.
     * <p/>
     * This method returns live references to the geometric objects.
     * Changing them will change the original geometry.
     *
     * @return a list of primitives
     */
    public java.util.List<Path> getPaths() {
        return paths;
    }

    /**
     * Add geometry to the group.
     * <p/>
     * Added geometry is not cloned.
     *
     * @param path the geometry to add.
     */
    public void add(Path path) {
        paths.add(path);
        currentPath = path;
        invalidate(false);
    }

    public int size() {
        return paths.size();
    }

    /**
     * Check if the group contains any paths.
     * This method does not check if the paths themselves are empty.
     *
     * @return true if the group contains no paths.
     */
    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public void clear() {
        paths.clear();
        currentPath = null;
        invalidate(false);
    }

    /**
     * Create copies of all paths in the given group and append them to myself.
     *
     * @param g the group whose paths are appended.
     */
    public void extend(Geometry g) {
        for (Path path : g.paths) {
            paths.add(path.clone());
        }
        invalidate(false);
    }

    /**
     * Check if the last path in this group is closed.
     * <p/>
     * A group (or path) can't technically be called "closed", only specific contours in the path can.
     * This method provides a reasonable heuristic for a "closed" group by checking the closed state
     * of the last contour on the last path. It returns false if this path contains no contours.
     *
     * @return true if the last contour on the last path is closed.
     */
    public boolean isClosed() {
        if (isEmpty()) return false;
        Path lastPath = paths.get(paths.size() - 1);
        return lastPath.isClosed();
    }

    //// Color operations ////

    public void setFillColor(Color fillColor) {
        for (Path path : paths) {
            path.setFillColor(fillColor);
        }
    }

    public void setFill(Color c) {
        setFillColor(c);
    }

    public void setStrokeColor(Color strokeColor) {
        for (Path path : paths) {
            path.setStrokeColor(strokeColor);
        }
    }

    public void setStroke(Color c) {
        setStrokeColor(c);
    }

    public void setStrokeWidth(float strokeWidth) {
        for (Path path : paths) {
            path.setStrokeWidth(strokeWidth);
        }
    }

    //// Point operations ////

    public int getPointCount() {
        int pointCount = 0;
        for (Path path : paths) {
            pointCount += path.getPointCount();
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
        ArrayList<Point> points = new ArrayList<Point>();
        for (Path path : paths) {
            points.addAll(path.getPoints());
        }
        return points;
    }

    public void addPoint(Point pt) {
        ensureCurrentPath();
        currentPath.addPoint(pt);
        invalidate(false);
    }

    public void addPoint(float x, float y) {
        ensureCurrentPath();
        currentPath.addPoint(x, y);
        invalidate(false);
    }

    private void ensureCurrentPath() {
        if (currentPath != null) return;
        currentPath = new Path();
        add(currentPath);
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
        lengthDirty = true;
        if (recursive) {
            for (Path path : paths) {
                path.invalidate();
            }
        }
    }

    //// Geometric queries ////

    /**
     * Returns the bounding box of all elements in the group.
     *
     * @return a bounding box that contains all elements in the group.
     */
    public Rect getBounds() {
        if (isEmpty()) return new Rect();
        Rect r = null;
        for (Grob g : paths) {
            if (r == null) {
                r = g.getBounds();
            }
            if (!g.isEmpty()) {
                r = r.united(g.getBounds());
            }
        }
        return r != null ? r : new Rect();
    }

    //// Geometric math ////

    /**
     * Calculate the length of the path. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the path.
     */
    public float getLength() {
        if (lengthDirty) {
            updatePathLengths();
        }
        return groupLength;
    }

    private void updatePathLengths() {
        pathLengths = new ArrayList<Float>(paths.size());
        groupLength = 0;
        float length;
        for (Path p : paths) {
            length = p.getLength();
            pathLengths.add(length);
            groupLength += length;
        }
        lengthDirty = false;
    }

    /**
     * Returns coordinates for point at t on the group.
     * <p/>
     * Gets the length of the group, based on the length
     * of each path in the group.
     * Determines in what path t falls.
     * Gets the point on that path.
     *
     * @param t relative coordinate of the point (between 0.0 and 1.0).
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
        Path currentPath = null;
        for (Path p : paths) {
            currentPath = p;
            cLength = p.getLength();
            if (absT <= cLength) break;
            absT -= cLength;
            resT -= cLength / length;
        }
        if (currentPath == null) return new Point();
        resT /= (currentPath.getLength() / length);
        return currentPath.pointAt(resT);
    }


    //// Geometric queries ////

    public boolean contains(Point pt) {
        for (Path p : paths) {
            if (p.contains(pt)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(float x, float y) {
        for (Path p : paths) {
            if (p.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Rect r) {
        for (Path p : paths) {
            if (p.contains(r)) {
                return true;
            }
        }
        return false;
    }

    //// Geometric operations ////

    public Point[] makePoints(int amount, boolean perContour) {
        if (perContour) {
            ArrayList<Point> points = new ArrayList<Point>();
            for (Path p : getPaths()) {
                for (Contour c : p.getContours()) {
                    Point[] pointsFromContour = c.makePoints(amount);
                    points.addAll(Arrays.asList(pointsFromContour));
                }
            }
            return (Point[]) points.toArray();
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

    public Geometry resampleByAmount(int amount, boolean perContour) {
        if (perContour) {
            Geometry g = new Geometry();
            for (Path p : paths) {
                g.add(p.resampleByAmount(amount, true));
            }
            return g;
        } else {
            Geometry g = new Geometry();
            float delta = pointDelta(amount, isClosed());
            for (int i = 0; i < amount; i++) {
                g.addPoint(pointAt(delta * i));
            }
            return g;
        }
    }

    public Geometry resampleByLength(float segmentLength) {
        Geometry g = new Geometry();
        for (Path p : paths) {
            g.add(p.resampleByLength(segmentLength));
        }
        return g;
    }

    //// Transformations ////

    public void transform(Transform t) {
        for (Path path : paths) {
            path.transform(t);
        }
        invalidate(true);
    }

    //// Drawing operations ////

    public void inheritFromContext(GraphicsContext ctx) {
        throw new UnsupportedOperationException();
    }

    public void draw(Graphics2D g) {
        for (Grob grob : paths) {
            grob.draw(g);
        }
    }

    public void flatten() {
        throw new UnsupportedOperationException();
    }

    public IGeometry flattened() {
        throw new UnsupportedOperationException();
    }

    //// Object methods ////

    public Geometry clone() {
        return new Geometry(this);
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ">";
    }

}
