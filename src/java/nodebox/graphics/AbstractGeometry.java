package nodebox.graphics;

import com.google.common.base.Function;

import java.util.Iterator;

/**
 *
 */
public abstract class AbstractGeometry extends AbstractTransformable implements IGeometry {

    //// Container operations ////

    /**
     * Extend the current geometry with the given list of points.
     *
     * @param points the points to add to the geometry.
     */
    public void extend(Iterator<Point> points) {
        while (points.hasNext()) {
            Point point = points.next();
            addPoint(point);
        }
    }

    /**
     * Extend the current geometry with the given list of points.
     *
     * @param points the points to add to the geometry.
     */
    public void extend(Point[] points) {
        for (Point point : points) {
            addPoint(point);
        }
    }

    //// Geometric operations ////

    /**
     * Make 100 new points along the contours of the existing path.
     * <p/>
     * Points are evenly distributed according to the length of each geometric object.
     *
     * @return a list of Points.
     */
    public Point[] makePoints() {
        return makePoints(DEFAULT_POINT_AMOUNT, false);
    }

    public Point[] makePoints(int amount) {
        return makePoints(amount, false);
    }

    /**
     * Calculate how far the points would be apart, given the specified amount and whether the geometry is closed.
     *
     * @param amount the amount of points
     * @param closed whether the geometry is closed
     * @return the delta value between each point
     */
    protected double pointDelta(int amount, boolean closed) {
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
        return delta;
    }

    public abstract Point pointAt(double t);

    public abstract IGeometry clone();

    /**
     * Change all points in the geometry and return a mutated copy.
     * The original geometry remains unchanged.
     *
     * @param pointFunction The function to apply to each point.
     * @return The new geometry.
     */
    public abstract AbstractGeometry mapPoints(Function<Point, Point> pointFunction);

}
