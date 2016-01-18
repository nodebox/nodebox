package nodebox.function;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.graphics.*;
import nodebox.handle.*;
import nodebox.util.MathUtils;

import java.awt.geom.Arc2D;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.function.MathFunctions.coordinates;

/**
 * Core vector function library.
 */
public class CoreVectorFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("corevector", CoreVectorFunctions.class,
                "generator", "filter",
                "align", "arc", "centroid", "colorize", "connect", "copy", "doNothing", "ellipse", "fit", "fitTo",
                "freehand", "grid", "group", "line", "lineAngle", "link", "makePoint", "point", "pointOnPath", "rect",
                "snap", "skew", "toPoints", "ungroup", "textpath",
                "fourPointHandle", "freehandHandle", "lineAngleHandle", "lineHandle", "pointHandle", "snapHandle",
                "translateHandle");
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
     * @param shape The input shape.
     * @return a Point at the center of the input shape.
     */
    public static Point centroid(IGeometry shape) {
        if (shape == null) return Point.ZERO;
        Rect bounds = shape.getBounds();
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
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
            newShape.setStrokeWidth(0);
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
        p.setStrokeWidth(1);
        return p;
    }


    public static List<IGeometry> copy(IGeometry shape, long copies, String order, Point translate, double rotate, Point scale) {
        ImmutableList.Builder<IGeometry> builder = ImmutableList.builder();
        Geometry geo = new Geometry();
        double tx = 0;
        double ty = 0;
        double r = 0;
        double sx = 1.0;
        double sy = 1.0;
        char[] cOrder = order.toCharArray();

        for (long i = 0; i < copies; i++) {
            Transform t = new Transform();

            // Each letter of the order describes an operation.
            for (char op : cOrder) {
                if (op == 't') {
                    t.translate(tx, ty);
                } else if (op == 'r') {
                    t.rotate(r);
                } else if (op == 's') {
                    t.scale(sx, sy);
                }
            }

            builder.add(t.map(shape));

            tx += translate.x;
            ty += translate.y;
            r += rotate;
            sx += scale.x / 100 - 1;
            sy += scale.y / 100 - 1;
        }
        return builder.build();
    }

    /**
     * Return the given object back, as-is.
     * <p/>
     * This function is used in nodes for organizational purposes.
     *
     * @param object The input object.
     * @return The unchanged input object.
     */
    public static Object doNothing(Object object) {
        return object;
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
     * Fit a shape within the given bounds.
     *
     * @param shape           The shape to fit.
     * @param position        The center of the target shape.
     * @param width           The width of the target bounds.
     * @param height          The height of the target shape.
     * @param keepProportions If true, the shape will not be stretched or squashed.
     * @return A new shape that fits within the given bounds.
     */
    public static IGeometry fit(IGeometry shape, Point position, double width, double height, boolean keepProportions) {
        if (shape == null) return null;

        Rect bounds = shape.getBounds();

        // Make sure bw and bh aren't infinitely small numbers.
        // This will lead to incorrect transformations with for examples lines.
        double bw = bounds.width > 0.000000000001 ? bounds.width : 0;
        double bh = bounds.height > 0.000000000001 ? bounds.height : 0;

        Transform t = new Transform();
        t.translate(position.x, position.y);
        double sx, sy;
        if (keepProportions) {
            // don't scale widths or heights that are equal to zero.
            sx = bw > 0 ? width / bw : Float.MAX_VALUE;
            sy = bh > 0 ? height / bh : Float.MAX_VALUE;
            sx = sy = Math.min(sx, sy);
        } else {
            sx = bw > 0 ? width / bw : 1;
            sy = bh > 0 ? height / bh : 1;
        }
        t.scale(sx, sy);
        t.translate(-bw / 2 - bounds.x, -bh / 2 - bounds.y);
        return t.map(shape);
    }

    /**
     * Fit a shape to another given shape.
     *
     * @param shape           The shape to fit.
     * @param bounding        The bounding, or target shape.
     * @param keepProportions If true, the shape will not be stretched or squashed.
     * @return A new shape that fits within the given bounding shape.
     */
    public static IGeometry fitTo(IGeometry shape, IGeometry bounding, boolean keepProportions) {
        if (shape == null) return null;
        if (bounding == null) return shape;

        Rect bounds = bounding.getBounds();
        return fit(shape, bounds.getCentroid(), bounds.width, bounds.height, keepProportions);
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
        p.setStrokeWidth(1);
        return p;
    }

    /**
     * Create a grid of (rows * columns) points.
     * <p/>
     * The total width and height of the grid are given, and the
     * spacing between rows and columns is calculated.
     *
     * @param columns  The number of columns.
     * @param rows     The number of rows.
     * @param width    The total width of the grid.
     * @param height   The total height of the grid.
     * @param position The center position of the grid.
     * @return A list of Points.
     */
    public static List<Point> grid(long columns, long rows, double width, double height, Point position) {
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
     * Combine multiple shapes together into one Geometry.
     *
     * @param shapes The list of shapes (Path or Geometry objects) to combine.
     * @return The combined Geometry.
     */
    public static Geometry group(List<IGeometry> shapes) {
        if (shapes == null) return null;
        Geometry geo = new Geometry();
        for (IGeometry shape : shapes) {
            if (shape instanceof Path) {
                geo.add((Path) shape);
            } else if (shape instanceof Geometry) {
                geo.extend((Geometry) shape);
            } else {
                throw new RuntimeException("Unable to group " + shape + ": I can only group paths or geometry objects.");
            }
        }
        return geo;
    }

    /**
     * Create a line from point 1 to point 2.
     *
     * @param p1     The first point.
     * @param p2     The second point.
     * @param points The amount of points to generate along the line.
     * @return A line between two points.
     */
    public static Path line(Point p1, Point p2, long points) {
        Path p = new Path();
        p.line(p1.x, p1.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        p = p.resampleByAmount((int) points, true);
        return p;
    }

    /**
     * Create a line at the given starting point with the end point calculated by the angle and distance.
     *
     * @param point    The starting point of the line.
     * @param angle    The angle of the line.
     * @param distance The distance of the line.
     * @param points   The amount of points to generate along the line.
     * @return A new line.
     */
    public static Path lineAngle(Point point, double angle, double distance, long points) {
        Point p2 = coordinates(point, angle, distance);
        Path p = new Path();
        p.line(point.x, point.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        p = p.resampleByAmount((int) points, true);
        return p;
    }

    /**
     * Create a path that visually links the two shapes together.
     * The shapes are only used for their bounding rectangles.
     *
     * @param shape1      The first shape.
     * @param shape2      The second shape.
     * @param orientation The link orientation, either "horizontal" or "vertical".
     * @return A new path.
     */
    public static Path link(Grob shape1, Grob shape2, String orientation) {
        if (shape1 == null || shape2 == null) return null;
        Path p = new Path();
        Rect a = shape1.getBounds();
        Rect b = shape2.getBounds();
        if (orientation.equals("horizontal")) {
            double hw = (b.x - (a.x + a.width)) / 2;
            p.moveto(a.x + a.width, a.y);
            p.curveto(a.x + a.width + hw, a.y, b.x - hw, b.y, b.x, b.y);
            p.lineto(b.x, b.y + b.height);
            p.curveto(b.x - hw, b.y + b.height, a.x + a.width + hw, a.y + a.height, a.x + a.width, a.y + a.height);
        } else {
            double hh = (b.y - (a.y + a.height)) / 2;
            p.moveto(a.x, a.y + a.height);
            p.curveto(a.x, a.y + a.height + hh, b.x, b.y - hh, b.x, b.y);
            p.lineto(b.x + b.width, b.y);
            p.curveto(b.x + b.width, b.y - hh, a.x + a.width, a.y + a.height + hh, a.x + a.width, a.y + a.height);
        }
        return p;
    }

    /**
     * Calculate a point on the given shape.
     *
     * @param shape The shape.
     * @param t     The position of the point, going from 0.0-100.0
     * @return The point on the given location of the path.
     */
    public static Point pointOnPath(AbstractGeometry shape, double t) {
        if (shape == null) return null;
        t = Math.abs(t % 100);
        return shape.pointAt(t / 100);
    }

    @SuppressWarnings("unchecked")
    public static Object skew(Object shape, Point skew, Point origin) {
        if (shape == null) return null;
        Transform t = new Transform();
        t.translate(origin);
        t.skew(skew.x, skew.y);
        t.translate(-origin.x, -origin.y);

        if (shape instanceof IGeometry) {
            return t.map((IGeometry) shape);
        } else if (shape instanceof List) {
            return t.map((List<Point>) shape);
        } else {
            throw new UnsupportedOperationException("I cannot work with " + shape.getClass().getSimpleName() + " objects.");
        }
    }

    /**
     * Snap the shape to a grid.
     *
     * @param shape    The shape to snap.
     * @param distance The grid size, or distance between grid lines.
     * @param strength The snap strength, between 0.0-100.0. If 0.0, no snapping occurs. If 100.0, all points are on the grid.
     * @param position The grid position.
     * @return The snapped geometry.
     */
    public static AbstractGeometry snap(AbstractGeometry shape, final double distance, final double strength, final Point position) {
        if (shape == null) return null;
        final double dStrength = strength / 100.0;
        return shape.mapPoints(new Function<Point, Point>() {
            public Point apply(Point point) {
                if (point == null) return Point.ZERO;
                double x = MathUtils.snap(point.x + position.x, distance, dStrength) - position.x;
                double y = MathUtils.snap(point.y + position.y, distance, dStrength) - position.y;
                return new Point(x, y, point.type);
            }
        });
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
     * Get the points of a given shape.
     *
     * @param shape The input shape.
     * @return A list of all points of the shape.
     */
    public static List<Point> toPoints(IGeometry shape) {
        if (shape == null) return null;
        return shape.getPoints();
    }

    /**
     * Decompose the given geometry into paths.
     *
     * @param shape The input geometry
     * @return The list of contained paths.
     */
    public static List<Path> ungroup(IGeometry shape) {
        if (shape == null) return null;
        if (shape instanceof Geometry) {
            return ((Geometry) shape).getPaths();
        } else if (shape instanceof Path) {
            return ImmutableList.of((Path) shape);
        } else {
            throw new RuntimeException("Don't know how to decompose " + shape + " into paths.");
        }
    }

    /**
     * Create a text path.
     *
     * @return A new Path.
     */
    public static Path textpath(String text, String fontName, double fontSize, String alignment, Point position, double width) {
        Text.Align align;
        try {
            align = Text.Align.valueOf(alignment);
        } catch (IllegalArgumentException ignore) {
            align = Text.Align.CENTER;
        }
        if (align == Text.Align.LEFT) {
            position = position.moved(0, 0);
        } else if (align == Text.Align.CENTER) {
            position = position.moved(-width / 2, 0);
        } else if (align == Text.Align.RIGHT) {
            position = position.moved(-width, 0);
        }

        Text t = new Text(text, position.x, position.y, width, 0);
        t.setFontName(fontName);
        t.setFontSize(fontSize);
        t.setAlign(align);

        return t.getPath();
    }

    /**
     * Create a new point with the given x,y coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return A new Point.
     */
    public static Point makePoint(double x, double y) {
        return new Point(x, y);
    }

    /**
     * Return the given Point as-is.
     */
    public static Point point(Point value) {
        return value;
    }

    //// Utility functions ////

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
        s = s.replace(",", " ");
        Contour contour = new Contour();
        boolean parseX = true;
        Double x = null;
        String lastString = null;
        for (String pointString : CONTOUR_SPLITTER.split(s)) {
            lastString = pointString;
            Double d = Double.parseDouble(pointString);
            if (parseX) {
                x = d;
                parseX = false;
            } else {
                contour.addPoint(new Point(x, d));
                parseX = true;
            }
        }
        if (! parseX)
            throw new IllegalArgumentException("Could not parse point " + lastString);
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

    //// Handles ////

    public static Handle fourPointHandle() {
        return new FourPointHandle();
    }

    public static Handle freehandHandle() {
        return new FreehandHandle();
    }

    public static Handle lineAngleHandle() {
        CombinedHandle handle = new CombinedHandle();
        handle.addHandle(new PointHandle());
        handle.addHandle(new RotateHandle("angle", "position"));
        return handle;
    }

    public static Handle lineHandle() {
        return new LineHandle();
    }

    public static Handle pointHandle() {
        return new PointHandle();
    }

    public static Handle snapHandle() {
        return new SnapHandle();
    }

    public static Handle translateHandle() {
        return new TranslateHandle();
    }

}
