package nodebox.graphics;

import java.util.List;

public interface IGeometry extends Grob {

    //// Point operations ////

    public int getPointCount();

    /**
     * Get the points for this geometry.
     * <p/>
     * This method returns live references to the points.
     * Changing them will change the original geometry.
     *
     * @return a list of Points.
     */
    public List<Point> getPoints();

    /**
     * Add the given point to the geometry. The point is cloned.
     *
     * @param pt the point to add.
     */
    public void addPoint(Point pt);

    /**
     * Add a new point to the geometry specified by its x and y coordinates.
     *
     * @param x the X coordinate.
     * @param y the Y coordinate.
     */
    public void addPoint(float x, float y);

    //// Geometric operations ////

    /**
     * Flatten the geometry.
     */
    public void flatten();

    /**
     * Make a flattened copy of the geometry.
     *
     * @return a flattened copy.
     */
    public IGeometry flattened();

    public IGeometry clone();
}
