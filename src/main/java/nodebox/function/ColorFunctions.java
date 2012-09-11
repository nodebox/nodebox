package nodebox.function;

import nodebox.graphics.Color;

public final class ColorFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("color", ColorFunctions.class, "color", "gray", "rgb", "hsb");
    }

    public static Color color(Color color) {
        return color;
    }

    public static Color gray(double gray, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(gray / range, gray / range, gray / range, alpha / range);
    }

    public static Color rgb(double red, double green, double blue, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(red / range, green / range, blue / range, alpha / range);
    }

    public static Color hsb(double hue, double saturation, double brightness, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(hue / range, saturation / range, brightness / range, alpha / range, Color.Mode.HSB);
    }

}
