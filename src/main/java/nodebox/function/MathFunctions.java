package nodebox.function;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.graphics.Point;
import nodebox.util.Geometry;
import nodebox.util.MathUtils;
import nodebox.util.waves.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static nodebox.util.MathUtils.clamp;

/**
 * Basic math function library.
 */
public class MathFunctions {

    public static final FunctionLibrary LIBRARY;
    public static final String OVERFLOW_WRAP = "wrap";
    public static final String OVERFLOW_MIRROR = "mirror";
    public static final String OVERFLOW_CLAMP = "clamp";
    public static final String OVERFLOW_IGNORE = "ignore";
    public static final String WAVE_SINE = "sine";
    public static final String WAVE_SQUARE = "square";
    public static final String WAVE_TRIANGLE = "triangle";
    public static final String WAVE_SAWTOOTH = "sawtooth";

    static {
        LIBRARY = JavaLibrary.ofClass("math", MathFunctions.class,
                "number", "integer", "makeBoolean",
                "negate", "abs", "add", "subtract", "multiply", "divide", "mod", "sqrt", "pow",
                "log", "sum", "average", "compare", "logicOperator", "min", "max", "ceil", "floor", "runningTotal",
                "even", "odd",
                "makeNumbers", "randomNumbers", "round",
                "sample", "range",
                "radians", "degrees", "angle", "distance", "coordinates", "reflect", "sin", "cos", "pi", "e",
                "convertRange", "wave");
    }

    public static double number(double n) {
        return n;
    }

    public static long integer(long value) {
        return value;
    }

    public static boolean makeBoolean(boolean value) {
        return value;
    }

    public static double add(double n1, double n2) {
        return n1 + n2;
    }

    public static double subtract(double n1, double n2) {
        return n1 - n2;
    }

    public static double multiply(double n1, double n2) {
        return n1 * n2;
    }

    public static double divide(double n1, double n2) {
        checkArgument(n2 != 0, "Divider cannot be zero.");
        return n1 / n2;
    }

    public static double mod(double n1, double n2) {
        checkArgument(n2 != 0, "Divider cannot be zero.");
        return n1 % n2;
    }

    public static double sqrt(double n) {
        return Math.sqrt(n);
    }

    public static double pow(double n1, double n2) {
        return Math.pow(n1, n2);
    }

    public static double log(double n) {
        checkArgument(n != 0, "Value cannot be zero.");
        return Math.log(n);
    }

    /**
     * Return true if the given number is even.
     *
     * @param n The number to check.
     * @return true if even
     */
    public static boolean even(double n) {
        return n % 2 == 0;
    }

    /**
     * Return true if the given number is not even.
     *
     * @param n The number to check.
     * @return true if odd
     */
    public static boolean odd(double n) {
        return n % 2 != 0;
    }

    public static double negate(double n) {
        return -n;
    }

    public static double abs(double n) {
        return Math.abs(n);
    }

    private static boolean noValues(Iterable<?> values) {
        return values == null || Iterables.isEmpty(values);
    }

    public static double sum(Iterable<Double> numbers) {
        if (noValues(numbers)) return 0.0;
        double sum = 0;
        for (Double d : numbers) {
            sum += d;
        }
        return sum;
    }

    public static double average(Iterable<Double> numbers) {
        if (noValues(numbers)) return 0.0;
        double sum = 0;
        double counter = 0;
        for (Double d : numbers) {
            sum += d;
            counter++;
        }
        return sum / counter;
    }

    public static double max(Iterable<Double> numbers) {
        if (noValues(numbers)) return 0.0;
        double max = Iterables.getFirst(numbers, 0.0);
        for (Double d : numbers) {
            max = Math.max(max, d);
        }
        return max;
    }

    public static double min(Iterable<Double> numbers) {
        if (noValues(numbers)) return 0.0;
        double min = Iterables.getFirst(numbers, 0.0);
        for (Double d : numbers) {
            min = Math.min(min, d);
        }
        return min;
    }

    public static double ceil(double n) {
        return Math.ceil(n);
    }

    public static double floor(double n) {
        return Math.floor(n);
    }

    @SuppressWarnings("unchecked")
    public static boolean compare(Comparable o1, Comparable o2, String comparator) {
        int comparison = o1.compareTo(o2);
        if (comparator.equals("<")) {
            return comparison < 0;
        } else if (comparator.equals(">")) {
            return comparison > 0;
        } else if (comparator.equals("<=")) {
            return comparison <= 0;
        } else if (comparator.equals(">=")) {
            return comparison >= 0;
        } else if (comparator.equals("==")) {
            return comparison == 0;
        } else if (comparator.equals("!=")) {
            return comparison != 0;
        } else {
            throw new IllegalArgumentException("unknown comparison operation " + comparator);
        }
    }


    public static boolean logicOperator(Boolean b1, Boolean b2, String comparator){
        if (comparator.equals("or")) {
            return b1 || b2;
        } else if (comparator.equals("and")) {
            return b1 && b2;
        }
        else if (comparator.equals("xor")) {
            return b1 ^ b2;
        }
        else {
            throw new IllegalArgumentException("unknown logical operation " );
        }
    }

    public static List<Double> makeNumbers(String s, String separator) {
        if (s == null || s.length() == 0) {
            return ImmutableList.of();
        }
        Iterable<String> parts;
        if (separator == null || separator.isEmpty())
            parts = Splitter.fixedLength(1).split(s);
        else
            parts = Splitter.on(separator).split(s);

        ArrayList<Double> numbers = new ArrayList<Double>();
        for (String part : parts) {
            numbers.add(Double.parseDouble(part));
        }
        return ImmutableList.copyOf(numbers);
    }

    public static List<Double> randomNumbers(long amount, double start, double end, long seed) {
        Random r = MathUtils.randomFromSeed(seed);
        ImmutableList.Builder<Double> numbers = ImmutableList.builder();
        for (int i = 0; i < amount; i++) {
            double v = start + (r.nextDouble() * (end - start));
            numbers.add(v);
        }
        return numbers.build();
    }

    public static long round(double a) {
        return Math.round(a);
    }

    public static List<Double> sample(final long amount, final double start, final double end) {
        if (amount == 0) return ImmutableList.of();
        if (amount == 1) return ImmutableList.of(start + (end - start) / 2);

        // The step is the range divided by amount - 1, because we also want the end value.
        // If I wouldn't use amount - 1, we fall one value short of the end.
        // E.g. if amount = 3 between 0-100, I want 0.0, 50.0, 100.0.
        final double step = (end - start) / (amount - 1);
        ImmutableList.Builder<Double> b = ImmutableList.builder();
        for (long i = 0; i < amount; i++) {
            b.add(start + step * i);
        }
        return b.build();
    }

    public static List<Double> range(final double start, final double end, final double step) {
        if (step == 0 || start == end || (start < end && step < 0) || (start > end && step > 0))
            return ImmutableList.of();
        else {
            return ImmutableList.copyOf(new Iterable<Double>() {
                public Iterator<Double> iterator() {
                    return new RangeIterator(start, end, step);
                }
            });
        }
    }

    public static List<Double> runningTotal(Iterable<Double> numbers) {
        if (noValues(numbers)) return ImmutableList.of(0.0);
        double currentTotal = 0;
        ImmutableList.Builder<Double> b = ImmutableList.builder();
        for (Double d : numbers) {
            b.add(currentTotal);
            currentTotal += d;
        }
        return b.build();
    }

    private static final class RangeIterator implements Iterator<Double> {
        private final double start;
        private final double end;
        private final double step;
        private double next;

        private RangeIterator(double start, double end, double step) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.next = this.start;
        }

        public boolean hasNext() {
            if (step > 0)
                return next < end;
            else
                return next > end;
        }

        public Double next() {
            if (Thread.currentThread().isInterrupted()) throw new RuntimeException("interrupt");
            //if (Thread.interrupted()) throw new RuntimeException("interrupt");
            if (!hasNext())
                throw new NoSuchElementException();
            double result = next;
            next += step;
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static double radians(double degrees) {
        return Geometry.radians(degrees);
    }

    public static double degrees(double radians) {
        return Geometry.degrees(radians);
    }

    /**
     * Calculate the angle between two points.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return The angle in radians.
     */
    public static double angle(Point p1, Point p2) {
        return Geometry.angle(p1.x, p1.y, p2.x, p2.y);
    }

    /**
     * The distance between two points.
     */
    public static double distance(Point p1, Point p2) {
        return Geometry.distance(p1.x, p1.y, p2.x, p2.y);
    }

    /**
     * The location of a point based on angle and distance.
     */
    public static Point coordinates(Point p, double angle, double distance) {
        double x = p.x + Math.cos(radians(angle)) * distance;
        double y = p.y + Math.sin(radians(angle)) * distance;
        return new Point(x, y);
    }

    /**
     * The reflection of a point through an origin point.
     */
    public static Point reflect(Point p1, Point p2, double angle, double distance) {
        distance *= distance(p1, p2);
        angle += angle(p1, p2);
        return coordinates(p1, angle, distance);
    }

    public static double sin(double n) {
        return Math.sin(n);
    }

    public static double cos(double n) {
        return Math.cos(n);
    }

    public static double pi() {
        return Math.PI;
    }

    public static double e() {
        return Math.E;
    }

    public static double convertRange(double value, double srcMin, double srcMax, double targetMin, double targetMax, String overflowMethod) {
        if (overflowMethod.equals(OVERFLOW_WRAP)) {
            value = srcMin + value % (srcMax - srcMin);
        } else if (overflowMethod.equals(OVERFLOW_MIRROR)) {
            double rest = value % (srcMax - srcMin);
            if ((int) (value / (srcMax - srcMin)) % 2 == 1)
                value = srcMax - rest;
            else
                value = srcMin + rest;
        } else if (overflowMethod.equals(OVERFLOW_CLAMP)) {
            value = clamp(value, srcMin, srcMax);

        }

        // Convert value to 0.0-1.0 range.
        try {
            value = (value - srcMin) / (srcMax - srcMin);
        } catch (ArithmeticException e) {
            value = srcMin;
        }

        // Convert value to target range.
        return targetMin + value * (targetMax - targetMin);
    }

    public static double wave(double min, double max, double period, double offset, String waveType) {
        float fmin = (float) min;
        float fmax = (float) max;
        float fperiod = (float) period;

        AbstractWave wave;
        if (waveType.equals(WAVE_TRIANGLE))
            wave = TriangleWave.from(fmin, fmax, fperiod);
        else if (waveType.equals(WAVE_SQUARE))
            wave = SquareWave.from(fmin, fmax, fperiod);
        else if (waveType.equals(WAVE_SAWTOOTH))
            wave = SawtoothWave.from(fmin, fmax, fperiod);
        else
            wave = SineWave.from(fmin, fmax, fperiod);
        return wave.getValueAt((float) offset);
    }

}
