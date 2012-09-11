package nodebox.function;

import nodebox.graphics.Point;

import java.io.File;

/**
 * A function library containing special functions for testing.
 */
public class TestFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("test", TestFunctions.class, "allTypes", "makeNull", "fileExists", "calculateMultipleArgs");
    }

    public static String allTypes(int i, float f, String s, Point pt) {
        StringBuilder b = new StringBuilder()
                .append(i).append(", ")
                .append(f).append(", ")
                .append(s).append(", ")
                .append(pt);
        return b.toString();
    }

    /**
     * Whatever the input, returns null.
     * @param ignored The input, which is ignored
     * @return null.
     */
    public static Double makeNull(Double ignored) {
        return null;
    }

    public static boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    public static double calculateMultipleArgs(double a, double b, double c, double d) {
        return a + b * c + d;
    }
}
