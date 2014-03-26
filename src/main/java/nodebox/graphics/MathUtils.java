package nodebox.graphics;

import java.util.Random;

public final class MathUtils {

    private MathUtils() {
    }

    /**
     * Clamps the value so the result is between 0.0 and 1.0.
     * <p/>
     * This means that if the value is smaller than 0.0, this method will return 0.0.
     * If the value is larger than 1.0, this method will return 1.0.
     * Values within the range are returned unchanged.
     *
     * @param v the value to clamp
     * @return a value between 0.0 and 1.0.
     */
    public static float clamp(float v) {
        return 0 > v ? 0 : 1 < v ? 1 : v;
    }

    /**
     * Clamps the value so the result is between 0.0 and 1.0.
     * <p/>
     * This means that if the value is smaller than 0.0, this method will return 0.0.
     * If the value is larger than 1.0, this method will return 1.0.
     * Values within the range are returned unchanged.
     *
     * @param v the value to clamp
     * @return a value between 0.0 and 1.0.
     */
    public static double clamp(double v) {
        return 0 > v ? 0 : 1 < v ? 1 : v;
    }


    /**
     * Clamps the value so the result is between the given minimum and maximum value.
     * <p/>
     * This means that if the value is smaller than min, this method will return min.
     * If the value is larger than max, this method will return max.
     * Values within the range are returned unchanged.
     *
     * @param v   the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return a value between min and max.
     */
    public static float clamp(float v, float min, float max) {
        return min > v ? min : max < v ? max : v;
    }

    /**
     * Clamps the value so the result is between the given minimum and maximum value.
     * <p/>
     * This means that if the value is smaller than min, this method will return min.
     * If the value is larger than max, this method will return max.
     * Values within the range are returned unchanged.
     *
     * @param v   the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return a value between min and max.
     */
    public static double clamp(double v, double min, double max) {
        return min > v ? min : max < v ? max : v;
    }

    /**
     * Round a value to the nearest "step".
     *
     * @param v        The value to snap.
     * @param distance The distance between steps.
     * @param strength The strength of rounding. If 1 the values will always be on a step. If zero, the value is unchanged.
     * @return The snapped value.
     */
    public static double snap(double v, double distance, double strength) {
        return (v * (1.0 - strength)) + (strength * java.lang.Math.round(v / distance) * distance);
    }

    public static Random randomFromSeed(long seed) {
        return new Random(seed * 1000000000);
    }

}
