package nodebox.graphics;

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
            addPoint(point.clone());
        }
    }

    /**
     * Extend the current geometry with the given list of points.
     *
     * @param points the points to add to the geometry.
     */
    public void extend(Point[] points) {
        for (Point point : points) {
            addPoint(point.clone());
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
        return makePoints(100);
    }

    public abstract IGeometry clone();

}
