package nodebox.graphics;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.awt.geom.Point2D;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

public final class Point {

    public final static Point ZERO = new Point(0, 0);

    public static final int LINE_TO = 1;
    public static final int CURVE_TO = 2;
    public static final int CURVE_DATA = 3;

    public static Point valueOf(String s) {
        String[] args = s.split(",");
        checkArgument(args.length == 2, "String '" + s + "' needs two components, i.e. 12.3,45.6");
        return new Point(Float.valueOf(args[0]), Float.valueOf(args[1]));
    }

    public static Point parsePoint(String s) {
        return valueOf(s);
    }

    public final double x, y;
    public final int type;
    public transient int hashCode;

    public Point() {
        this(0, 0, LINE_TO);
    }

    public Point(double x, double y) {
        this(x, y, LINE_TO);
    }

    public Point(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public Point(Point2D pt) {
        this(pt.getX(), pt.getY(), LINE_TO);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getType() {
        return type;
    }

    public boolean isLineTo() {
        return type == LINE_TO;
    }

    public boolean isCurveTo() {
        return type == CURVE_TO;
    }

    public boolean isCurveData() {
        return type == CURVE_DATA;
    }

    public boolean isOnCurve() {
        return type != CURVE_DATA;
    }

    public boolean isOffCurve() {
        return type == CURVE_DATA;
    }

    //// "Mutation" methods ////

    public Point moved(double dx, double dy) {
        return new Point(x + dx, y + dy);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        final Point other = (Point) o;
        return Objects.equal(x, other.x) && Objects.equal(y, other.y) && Objects.equal(type, other.type);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hashCode(x, y, type);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%.2f,%.2f", x, y);
    }

    public Point2D toPoint2D() {
        return new Point2D.Double(x, y);
    }

}
