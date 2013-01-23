package nodebox.graphics;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nodebox.util.IOrderedFields;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public final class Point implements Map<String, Number>, IOrderedFields {

    public final static Point ZERO = new Point(0, 0);

    public static final int LINE_TO = 1;
    public static final int CURVE_TO = 2;
    public static final int CURVE_DATA = 3;

    private static final ImmutableList<String> POINT_FIELDS = ImmutableList.of("x", "y", "type");

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

    //// Map interface ////


    @Override
    public Iterable<String> getOrderedFields() {
        return POINT_FIELDS;
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object o) {
        return POINT_FIELDS.contains(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return o != null && (o.equals(x) || o.equals(y) || o.equals(type));
    }

    @Override
    public Number get(Object o) {
        if (o == null) return null;
        if (o.equals("x")) return x;
        if (o.equals("y")) return y;
        if (o.equals("type")) return type;
        return null;
    }

    @Override
    public Number put(String s, Number o) {
        throw new UnsupportedOperationException("Points are immutable.");
    }

    @Override
    public Number remove(Object o) {
        throw new UnsupportedOperationException("Points are immutable.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Number> map) {
        throw new UnsupportedOperationException("Points are immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Points are immutable.");
    }

    @Override
    public Set<String> keySet() {
        return ImmutableSet.copyOf(POINT_FIELDS);
    }

    @Override
    public Collection<Number> values() {
        return ImmutableList.<Number>of(x, y, type);
    }

    @Override
    public Set<Entry<String, Number>> entrySet() {
        return ImmutableMap.<String,Number>of("x", x, "y", y, "type", type).entrySet();
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
