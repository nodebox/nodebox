package nodebox.function;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import nodebox.graphics.*;
import nodebox.handle.*;

import java.awt.geom.Arc2D;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Core vector function library.
 */
public class CoreVectorFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("corevector", CoreVectorFunctions.class,
                "generator", "filter",
                "align", "arc", "centroid", "colorize", "connect", "ellipse", "freehand", "grid", "line", "rect",
                "toPoints", "valuesToPoint",
                "fourPointHandle", "freehandHandle", "lineHandle", "pointHandle");
    }

    /**
     * Example function that generates a path.
     * This is here so people can view and change the existing code.
     *
     * @return An example Path.
     */
    public static Path generator() {
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        return p;
    }

    /**
     * Example function that rotates the given shape.
     *
     * @param geometry The input geometry.
     * @return The new, rotated geometry.
     */
    public static Geometry filter(Geometry geometry) {
        if (geometry == null) return null;
        Transform t = new Transform();
        t.rotate(45);
        return t.map(geometry);
    }

    /**
     * Align a shape in relation to the origin.
     *
     * @param geometry The input geometry.
     * @param position The point to align to.
     * @param hAlign   The horizontal align mode. Either "left", "right" or "center".
     * @param vAlign   The vertical align mode. Either "top", "bottom" or "middle".
     * @return The aligned Geometry. The original geometry is left intact.
     */
    public static AbstractGeometry align(AbstractGeometry geometry, Point position, String hAlign, String vAlign) {
        if (geometry == null) return null;
        double x = position.x;
        double y = position.y;
        Rect bounds = geometry.getBounds();
        double dx, dy;
        if (hAlign.equals("left")) {
            dx = x - bounds.x;
        } else if (hAlign.equals("right")) {
            dx = x - bounds.x - bounds.width;
        } else if (hAlign.equals("center")) {
            dx = x - bounds.x - bounds.width / 2;
        } else {
            dx = 0;
        }
        if (vAlign.equals("top")) {
            dy = y - bounds.y;
        } else if (vAlign.equals("bottom")) {
            dy = y - bounds.y - bounds.height;
        } else if (vAlign.equals("middle")) {
            dy = y - bounds.y - bounds.height / 2;
        } else {
            dy = 0;
        }

        Transform t = Transform.translated(dx, dy);
        if (geometry instanceof Path) {
            return t.map((Path) geometry);
        } else if (geometry instanceof Geometry) {
            return t.map((Geometry) geometry);
        } else {
            throw new IllegalArgumentException("Unknown geometry type " + geometry.getClass().getName());
        }
    }

    /**
     * Create an arc at the given position.
     * <p/>
     * Arcs rotate in the opposite direction from Java's Arc2D to be compatible with our transform functions.
     *
     * @param position   The position of the arc.
     * @param width      The arc width.
     * @param height     The arc height.
     * @param startAngle The start angle.
     * @param degrees    The amount of degrees.
     * @param arcType    The type of arc. Either "chord", "pie", or "open"
     * @return The new arc.
     */
    public static Path arc(Point position, double width, double height, double startAngle, double degrees, String arcType) {

        int awtType;
        if (arcType.equals("chord")) {
            awtType = Arc2D.CHORD;
        } else if (arcType.equals("pie")) {
            awtType = Arc2D.PIE;
        } else {
            awtType = Arc2D.OPEN;
        }
        return new Path(new Arc2D.Double(position.x - width / 2, position.y - height / 2, width, height,
                -startAngle, -degrees, awtType));
    }

    /**
     * Calculate the geometric center of a shape.
     *
     * @param grob The input shape.
     * @return a Point at the center of the input shape.
     */
    public static Point centroid(Grob grob) {
        if (grob == null) return Point.ZERO;
        return grob.getBounds().getCentroid();
    }

    /**
     * Change the color of a shape.
     *
     * @param shape       The input shape.
     * @param fill        The new fill color.
     * @param stroke      The new stroke color.
     * @param strokeWidth The new stroke width.
     * @return The new colored shape.
     */
    public static Colorizable colorize(Colorizable shape, Color fill, Color stroke, double strokeWidth) {
        if (shape == null) return null;
        Colorizable newShape = shape.clone();
        newShape.setFill(fill);
        if (strokeWidth > 0) {
            newShape.setStrokeColor(stroke);
            newShape.setStrokeWidth(strokeWidth);
        } else {
            newShape.setStrokeColor(null);
        }
        return newShape;
    }

    /**
     * Connects all given points, in order, as a new path.
     *
     * @param points A list of points.
     * @param closed If true, close the path contour.
     * @return A new path with all points connected.
     */
    public static Path connect(List<Point> points, boolean closed) {
        if (points == null) return null;
        Path p = new Path();
        for (Point pt : points) {
            p.addPoint(pt);
        }
        if (closed)
            p.close();
        p.setFill(null);
        p.setStroke(Color.BLACK);
        return p;
    }

    /**
     * Create an ellipse at the given position.
     *
     * @param position The center position of the ellipse.
     * @param width    The ellipse width.
     * @param height   The ellipse height.
     * @return The new ellipse, as a Path.
     */
    public static Path ellipse(Point position, double width, double height) {
        Path p = new Path();
        p.ellipse(position.x, position.y, width, height);
        return p;
    }

    /**
     * Get the points of a given shape.
     *
     * @param shape The input shape.
     * @return A list of all points of the shape.
     */
    public static List<Point> toPoints(IGeometry shape) {
        if (shape == null) return null;
        return shape.getPoints();
    }

    private final static Splitter PATH_SPLITTER = Splitter.on("M").omitEmptyStrings();
    private final static Splitter CONTOUR_SPLITTER = Splitter.on(" ").omitEmptyStrings();
    private final static Splitter POINT_SPLITTER = Splitter.on(",");

    /**
     * Create a new, open path with the given path string.
     * <p/>
     * The path string is composed of contour strings, starting with "M". Points are separated by a a space, e.g.:
     * <p/>
     * "M0,0 100,0 100,100 0,100M10,20 30,40 50,60"
     *
     * @param pathString The string to parse
     * @return a new Path.
     */
    public static Path freehand(String pathString) {
        if (pathString == null) return new Path();
        Path p = parsePath(pathString);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        return p;
    }

    public static Path parsePath(String s) {
        checkNotNull(s);
        Path p = new Path();
        s = s.trim();
        for (String pointString : PATH_SPLITTER.split(s)) {
            pointString = pointString.trim();
            if (!pointString.isEmpty()) {
                p.add(parseContour(pointString));
            }
        }
        return p;
    }

    public static Contour parseContour(String s) {
        Contour contour = new Contour();
        for (String pointString : CONTOUR_SPLITTER.split(s)) {
            contour.addPoint(parsePoint(pointString));
        }
        return contour;
    }

    public static Point parsePoint(String s) {
        Double x = null, y = null;
        for (String numberString : POINT_SPLITTER.split(s)) {
            if (x == null) {
                x = Double.parseDouble(numberString);
            } else if (y == null) {
                y = Double.parseDouble(numberString);
            } else {
                throw new IllegalArgumentException("Too many coordinates in point " + s);
            }
        }
        if (x != null && y != null) {
            return new Point(x, y);
        } else {
            throw new IllegalArgumentException("Could not parse point " + s);
        }
    }

    /**
     * Create a grid of (rows * columns) points.
     * <p/>
     * The total width and height of the grid are given, and the
     * spacing between rows and columns is calculated.
     *
     * @param rows     The number of rows.
     * @param columns  The number of columns.
     * @param width    The total width of the grid.
     * @param height   The total height of the grid.
     * @param position The center position of the grid.
     * @return A list of Points.
     */
    public static List<Point> grid(long rows, long columns, double width, double height, Point position) {
        double columnSize, left, rowSize, top;
        if (columns > 1) {
            columnSize = width / (columns - 1);
            left = position.x - width / 2;
        } else {
            columnSize = left = position.x;
        }
        if (rows > 1) {
            rowSize = height / (rows - 1);
            top = position.y - height / 2;
        } else {
            rowSize = top = position.y;
        }

        ImmutableList.Builder<Point> builder = new ImmutableList.Builder<Point>();
        for (long rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (long colIndex = 0; colIndex < columns; colIndex++) {
                double x = left + colIndex * columnSize;
                double y = top + rowIndex * rowSize;
                builder.add(new Point(x, y));
            }
        }
        return builder.build();
    }

    /**
     * Create a line from point 1 to point 2.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return A line between two points.
     */
    public static Path line(Point p1, Point p2) {
        Path p = new Path();
        p.line(p1.x, p1.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        return p;
    }

    /**
     * Create a rectangle.
     *
     * @param position  The center position of the rectangle.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param roundness The roundness of the rectangle, given as a x,y Point. If the roundness is (0,0), we draw a normal rectangle.
     * @return The new rectangle.
     */
    public static Path rect(Point position, double width, double height, Point roundness) {
        Path p = new Path();
        if (roundness.equals(Point.ZERO)) {
            p.rect(position.x, position.y, width, height);
        } else {
            p.roundedRect(position.x, position.y, width, height, roundness.x, roundness.y);
        }
        return p;
    }

    /**
     * Create a new point with the given x,y coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return A new Point.
     */
    public static Point valuesToPoint(double x, double y) {
        return new Point(x, y);
    }

    //// Handles ////

    public static Handle fourPointHandle() {
        return new FourPointHandle();
    }

    public static Handle freehandHandle() {
        return new FreehandHandle();
    }

    public static Handle lineHandle() {
        return new LineHandle();
    }

    public static Handle pointHandle() {
        return new PointHandle();
    }

}
