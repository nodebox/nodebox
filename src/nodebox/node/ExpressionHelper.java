package nodebox.node;

import nodebox.graphics.Color;

import java.util.Random;

/**
 * Class containing static method used in Expression.
 *
 * @see nodebox.node.Expression
 */
public class ExpressionHelper {

    // TODO: Expression system is not thread-safe.
    public static ProcessingContext currentContext;
    public static Parameter currentParameter;

    public static Random randomGenerator = new Random();

    public static double random(long seed, double... minmax) {
        switch (minmax.length) {
            case 0:
                return random(seed);
            case 1:
                return random(seed, minmax[0]);
            default: // Anything larger than 2
                return random(seed, minmax[0], minmax[1]);
        }
    }

    public static double random(long seed) {
        randomGenerator.setSeed(seed * 100000000);
        return randomGenerator.nextDouble();
    }

    public static double random(long seed, double max) {
        return random(seed) * max;
    }

    public static double random(long seed, double min, double max) {
        return min + random(seed) * (max - min);
    }

    public static int randint(long seed, int min, int max) {
        randomGenerator.setSeed(seed * 100000000);
        // nextInt's specified value is exclusive, whereas we want to include it, so add 1.
        return min + randomGenerator.nextInt(max - min + 1);
    }

    public static int toInt(double v) {
        return (int) v;
    }

    public static double toFloat(int v) {
        return (double) v;
    }

    public static Color color(double... values) {
        switch (values.length) {
            case 0:
                return new Color();
            case 1:
                return new Color(values[0], values[0], values[0]);
            case 2:
                return new Color(values[0], values[0], values[0], values[1]);
            case 3:
                return new Color(values[0], values[1], values[2]);
            case 4:
                return new Color(values[0], values[1], values[2], values[3]);
            default:
                return new Color();
        }
    }

    public static Color color(double gray) {
        return new Color(gray, gray, gray);
    }

    public static Color color(double gray, double alpha) {
        return new Color(gray, gray, gray, alpha);
    }

    public static Color color(double red, double green, double blue) {
        return new Color(red, green, blue);
    }

    public static Color color(double red, double green, double blue, double alpha) {
        return new Color(red, green, blue, alpha);
    }

    public static Object stamp(String key, Object defaultValue) {
        if (currentContext == null) return defaultValue;
        currentParameter.markStampExpression();
        Object v = currentContext.get(key);
        return v != null ? v : defaultValue;
    }

}
