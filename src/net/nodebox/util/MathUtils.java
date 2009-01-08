package net.nodebox.util;

public class MathUtils {

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
}
