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
                "align", "arc", "centroid", "color", "ellipse", "freehand", "grid", "line", "rect", "valuesToPoint",
                "fourPointHandle", "freehandHandle", "lineHandle", "pointHandle");
    }

    public static Path generator() {
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        return p;
    }

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
     * @param grob The input shape.
     * @return a Point at the center of the input shape.
     */
    public static Point centroid(Grob grob) {
        if (grob == null) return Point.ZERO;
        return grob.getBounds().getCentroid();
    }

    public static Geometry color(Geometry geometry, Color fill, Color stroke, double strokeWidth) {
        Geometry copy = geometry.clone();
        for (Path path : copy.getPaths()) {
            path.setFill(fill);
            path.setStroke(stroke);
            path.setStrokeWidth(strokeWidth);
        }
        return copy;
    }

    public static Path ellipse(Point position, double width, double height) {
        Path p = new Path();
        p.ellipse(position.x, position.y, width, height);
        return p;
    }

    private final static Splitter POINT_SPLITTER = Splitter.on(" ");
    private final static Splitter CONTOUR_SPLITTER = Splitter.on("M");

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
        for (String pointString : CONTOUR_SPLITTER.split(s)) {
            pointString = pointString.trim();
            if (!pointString.isEmpty()) {
                p.add(parseContour(pointString));
            }
        }
        return p;
    }

    public static Contour parseContour(String s) {
        Contour contour = new Contour();
        Double x = null;
        for (String numberString : POINT_SPLITTER.split(s)) {
            if (x == null) {
                x = Double.parseDouble(numberString);
            } else {
                double y = Double.parseDouble(numberString);
                contour.addPoint(x, y);
                x = null;
            }
        }
        return contour;
    }

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

    public static Path line(Point p1, Point p2) {
        Path p = new Path();
        p.line(p1.x, p1.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        return p;
    }

    public static Path rect(Point position, double width, double height, Point roundness) {
        Path p = new Path();
        if (roundness.equals(Point.ZERO)) {
            p.rect(position.x, position.y, width, height);
        } else {
            p.roundedRect(position.x, position.y, width, height, roundness.x, roundness.y);
        }
        return p;
    }

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
