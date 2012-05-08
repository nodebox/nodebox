package nodebox.function;

import nodebox.graphics.Color;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;

/**
 * Core vector function library.
 */
public class CoreVectorFunctions {

    public static final FunctionLibrary LIBRARY;


    static {
        LIBRARY = JavaLibrary.ofClass("corevector", CoreVectorFunctions.class,
                "rect", "color", "valuesToPoint");
    }

    public static Geometry rect(Point position, double width, double height) {
        Path p = new Path();
        p.rect(position.getX(), position.getY(), width, height);
        return p.asGeometry();
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

    public static Point valuesToPoint(double x, double y) {
        return new Point(x, y);
    }

}
