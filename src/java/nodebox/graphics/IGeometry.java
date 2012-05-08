package nodebox.graphics;

import java.util.Iterator;
import java.util.List;

public interface IGeometry extends Grob {

    public static final int DEFAULT_POINT_AMOUNT = 100;

    //// Container operations ////

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
    public void addPoint(double x, double y);

    /**
     * Extend the current geometry with the given list of points.
     *
     * @param points the points to add to the geometry.
     */
    public void extend(Iterator<Point> points);

    /**
     * Extend the current geometry with the given list of points.
     *
     * @param points the points to add to the geometry.
     */
    public void extend(Point[] points);

    //// Geometric operations ////

    /**
     * Make 100 new points along the contours of the existing path.
     * <p/>
     * Points are evenly distributed according to the length of each geometric object.
     *
     * @return a list of Points.
     */
    public Point[] makePoints();

    /**
     * Make 100 new points along the contours of the existing path.
     * <p/>
     * Points are evenly distributed according to the length of each geometric object.
     *
     * @param amount the amount of points to distribute.
     * @return a list of Points.
     */
    public Point[] makePoints(int amount);

    /**
     * Make new points along the contours of the existing path.
     * <p/>
     * Points are evenly distributed according to the length of each geometric object.
     *
     * @param amount     the amount of points to distribute.
     * @param perContour if true, the points are distributed per contour. The amount of points returned will then be
     *                   number of contours * amount.
     * @return a list of Points.
     */
    public Point[] makePoints(int amount, boolean perContour);

    /**
     * Generate new geometry with the given amount of points along the shape of the original geometry.
     * <p/>
     * The length of each segment is not given and will be determined based on the required number of points.
     *
     * @param amount     the number of points to generate.
     * @param perContour whether the given points are per contour, or for the entire geometry.
     * @return a new geometry object. This method will return whatever comes out of it, so calling resample on a
     *         Contour will return a new Contour object.
     */
    public IGeometry resampleByAmount(int amount, boolean perContour);

    /**
     * Generate new geometry with points along the shape of the original geometry, spaced at the given length.
     * <p/>
     * The number of points is not given and will be determined by the system based on the segment length.
     *
     * @param segmentLength the maximum length of each resampled segment.
     * @return a new geometry object. This object will be of the same type as the callee, so calling resample on a Contour
     *         will return a new Contour object.
     */
    public IGeometry resampleByLength(double segmentLength);

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

    /**
     * Clone the geometry, returning a new copy that is totally independent from the original.
     *
     * @return the new geometry object.
     */
    public IGeometry clone();
}
