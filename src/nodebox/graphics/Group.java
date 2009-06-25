package nodebox.graphics;

import java.util.ArrayList;
import java.awt.*;

public class Group implements IGeometry, Colorizable {

    private ArrayList<IGeometry> items;
    private Path currentPath;

    public Group() {
        items = new ArrayList<IGeometry>();
        currentPath = null;
    }

    public Group(Group other) {
        items = new ArrayList<IGeometry>(other.items.size());
        for (IGeometry item : other.items) {
            items.add(item.clone());
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
    public Path[] getPaths() {
        if (items == null) return new Path[0];
        Path[] paths = new Path[0];
        int pathCount = 0;
        for (IGeometry item : items) {
            if (item instanceof Group) {
                Path[] groupPaths = ((Group) item).getPaths();
                Path[] newPaths = new Path[paths.length + groupPaths.length];
                System.arraycopy(paths, 0, newPaths, 0, paths.length);
                System.arraycopy(groupPaths, 0, newPaths, paths.length, groupPaths.length);
                paths = newPaths;
            } else if (item instanceof Path) {
                if (pathCount + 1 > paths.length) {
                    Path[] newPaths = new Path[paths.length + 1];
                    System.arraycopy(paths, 0, newPaths, 0, paths.length);
                    paths = newPaths;
                }
                paths[pathCount++] = (Path) item;
            }
        }
        return paths;
    }

    /**
     * Add geometry to the group.
     * <p/>
     * Added geometry is not cloned.
     *
     * @param g the geometry to add.
     */
    public void add(IGeometry g) {
        items.add(g);
        if (g instanceof Path) {
            currentPath = (Path) g;
        }
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
        currentPath = null;
    }

    public java.util.List<IGeometry> getItems() {
        return items;
    }

    /**
     * Create copies of all grobs of the given group and append them to myself.
     *
     * @param g the group whose elements are appended.
     */
    public void extend(Group g) {
        for (IGeometry item : g.items) {
            items.add(item.clone());
        }
    }

    //// Color operations ////

    public void setFillColor(Color fillColor) {
        for (IGeometry item : items) {
            if (item instanceof Colorizable) {
                ((Colorizable) item).setFillColor(fillColor);
            }
        }
    }

    public void setFill(Color c) {
        setFillColor(c);
    }

    public void setStrokeColor(Color strokeColor) {
        for (IGeometry item : items) {
            if (item instanceof Colorizable) {
                ((Colorizable) item).setStrokeColor(strokeColor);
            }
        }
    }

    public void setStroke(Color c) {
        setStrokeColor(c);
    }

    public void setStrokeWidth(float strokeWidth) {
        for (IGeometry item : items) {
            if (item instanceof Colorizable) {
                ((Colorizable) item).setStrokeWidth(strokeWidth);
            }
        }
    }

    //// Point operations ////

    public int getPointCount() {
        int pointCount = 0;
        for (IGeometry item : items) {
            pointCount += item.getPointCount();
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
        for (IGeometry item : items) {
            points.addAll(item.getPoints());
        }
        return points;
    }

    public void addPoint(Point pt) {
        ensureCurrentPath();
        currentPath.addPoint(pt);
    }

    public void addPoint(float x, float y) {
        ensureCurrentPath();
        currentPath.addPoint(x, y);
    }

    private void ensureCurrentPath() {
        if (currentPath != null) return;
        currentPath = new Path();
        add(currentPath);
    }

    //// Geometric queries ////

    /**
     * Returns the bounding box of all elements in the group.
     *
     * @return a bounding box that contains all elements in the group.
     */
    public Rect getBounds() {
        if (items == null) return new Rect();
        Rect r = null;
        for (Grob g : items) {
            if (r == null) {
                r = g.getBounds();
            } else {
                r = r.united(g.getBounds());
            }
        }
        return r;
    }

    //// Transformations ////

    public void transform(Transform t) {
        for (Grob grob : items) {
            grob.transform(t);
        }
    }

    //// Drawing operations ////

    public void inheritFromContext(GraphicsContext ctx) {
        throw new UnsupportedOperationException();
    }

    public void draw(Graphics2D g) {
        for (Grob grob : items) {
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

    public Group clone() {
        return new Group(this);
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ">";
    }

}
