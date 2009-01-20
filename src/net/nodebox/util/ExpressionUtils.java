package net.nodebox.util;

import net.nodebox.graphics.Color;

import java.util.Random;

public class ExpressionUtils {

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

}
