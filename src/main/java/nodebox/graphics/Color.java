/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */

package nodebox.graphics;

import java.awt.*;

import static com.google.common.base.Preconditions.checkArgument;
import static nodebox.graphics.MathUtils.clamp;

public final class Color implements Cloneable {

    public enum Mode {
        RGB, HSB, CMYK
    }

    public static final Color BLACK = new Color(0);
    public static final Color WHITE = new Color(1);

    private final double r, g, b, a;
    private final double h, s, v;
    private transient java.awt.Color awtColor = null;

    public static Color fromHSB(double hue, double saturation, double brightness) {
        return new Color(hue, saturation, brightness, Mode.HSB);
    }

    public static Color fromHSB(double hue, double saturation, double brightness, double alpha) {
        return new Color(hue, saturation, brightness, alpha, Mode.HSB);
    }

    public static Color valueOf(String hex) {
        return new Color(hex);
    }

    /**
     * Create an empty (black) color object.
     */
    public Color() {
        this(0, 0, 0, 1.0, Mode.RGB);
    }

    /**
     * Create a new color with the given grayscale value.
     *
     * @param v the gray component.
     */
    public Color(double v) {
        this(v, v, v, 1.0, Mode.RGB);
    }

    /**
     * Create a new color with the given grayscale and alpha value.
     *
     * @param v the grayscale value.
     * @param a the alpha value.
     */

    public Color(double v, double a) {
        this(v, v, v, a, Mode.RGB);
    }

    /**
     * Create a new color with the the given R/G/B value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @param m the specified color mode.
     */
    public Color(double x, double y, double z, Mode m) {
        this(x, y, z, 1.0, m);
    }

    /**
     * Create a new color with the the given R/G/B value.
     *
     * @param r the red component.
     * @param g the green component.
     * @param b the blue component.
     */
    public Color(double r, double g, double b) {
        this(r, g, b, 1.0, Mode.RGB);
    }

    /**
     * Create a new color with the the given R/G/B/A or H/S/B/A value.
     *
     * @param r the red component.
     * @param g the green component.
     * @param b the blue component.
     * @param a the alpha component.
     */
    public Color(double r, double g, double b, double a) {
        this(r, g, b, a, Mode.RGB);
    }

    /**
     * Create a new color with the the given R/G/B/A or H/S/B/A value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @param a the alpha component.
     * @param m the specified color mode.
     */
    public Color(double x, double y, double z, double a, Mode m) {
        checkArgument(m != Mode.CMYK, "CMYK not supported");

        switch (m) {
            case CMYK:
                throw new RuntimeException("CMYK color mode is not implemented yet.");
            case HSB:
                this.h = clamp(x);
                this.s = clamp(y);
                this.v = clamp(z);
                this.a = clamp(a);
                double[] rgb = updateRGB();
                this.r = rgb[0];
                this.g = rgb[1];
                this.b = rgb[2];
                //updateCMYK();
                break;
            case RGB:
            default:
                this.r = clamp(x);
                this.g = clamp(y);
                this.b = clamp(z);
                this.a = clamp(a);
                double[] hsb = updateHSB();
                this.h = hsb[0];
                this.s = hsb[1];
                this.v = hsb[2];
                //updateCMYK();
                break;
        }
    }

    public Color(String colorName) {
        if (!colorName.startsWith("#")) {
            throw new IllegalArgumentException("The given value '" + colorName + "' is not of the format #112233.");
        }
        int r255, g255, b255, a255 = 255;
        if (colorName.length() == 4) { // #123
            r255 = Integer.parseInt(colorName.substring(1, 2) + colorName.substring(1, 2), 16);
            g255 = Integer.parseInt(colorName.substring(2, 3) + colorName.substring(2, 3), 16);
            b255 = Integer.parseInt(colorName.substring(3, 4) + colorName.substring(3, 4), 16);
        } else if (colorName.length() == 5) { // #123f
            r255 = Integer.parseInt(colorName.substring(1, 2) + colorName.substring(1, 2), 16);
            g255 = Integer.parseInt(colorName.substring(2, 3) + colorName.substring(2, 3), 16);
            b255 = Integer.parseInt(colorName.substring(3, 4) + colorName.substring(3, 4), 16);
            a255 = Integer.parseInt(colorName.substring(4, 5) + colorName.substring(4, 5), 16);
        } else if (colorName.length() == 7) { // #112233
            r255 = Integer.parseInt(colorName.substring(1, 3), 16);
            g255 = Integer.parseInt(colorName.substring(3, 5), 16);
            b255 = Integer.parseInt(colorName.substring(5, 7), 16);
        } else if (colorName.length() == 9) { // #112233ff
            r255 = Integer.parseInt(colorName.substring(1, 3), 16);
            g255 = Integer.parseInt(colorName.substring(3, 5), 16);
            b255 = Integer.parseInt(colorName.substring(5, 7), 16);
            a255 = Integer.parseInt(colorName.substring(7, 9), 16);
        } else {
            throw new IllegalArgumentException("The given value '" + colorName + "' is not of the format #112233.");
        }
        this.r = r255 / 255.0;
        this.g = g255 / 255.0;
        this.b = b255 / 255.0;
        this.a = a255 / 255.0;
        double[] hsb = updateHSB();
        this.h = hsb[0];
        this.s = hsb[1];
        this.v = hsb[2];
        //updateCMYK();
    }

    /**
     * Create a new color with the the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the new color is turned off (same as nocolor).
     *
     * @param color the color object.
     */
    public Color(java.awt.Color color) {
        this(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0, color.getAlpha() / 255.0);
    }

    /**
     * Create a new color with the the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the new color is turned off (same as nocolor).
     *
     * @param other the color object.
     */
    public Color(Color other) {
        this(other.r, other.g, other.b, other.a);
    }

    public double getRed() {
        return r;
    }

    public double getR() {
        return r;
    }

    public double getGreen() {
        return g;
    }

    public double getG() {
        return g;
    }

    public double getBlue() {
        return b;
    }

    public double getB() {
        return b;
    }

    public double getAlpha() {
        return a;
    }

    public double getA() {
        return a;
    }

    public boolean isVisible() {
        return a > 0.0;
    }

    public double getHue() {
        return h;
    }

    public double getH() {
        return h;
    }

    public double getSaturation() {
        return s;
    }

    public double getS() {
        return s;
    }

    public double getBrightness() {
        return v;
    }

    public double getV() {
        return v;
    }

    private double[] updateRGB() {
        if (s == 0)
            return new double[]{this.v, this.v, this.v};
        else {
            double h = this.h;
            if (this.h == 1.0)
                h = 0.999998;
            double s = this.s;
            double v = this.v;
            double f, p, q, t;
            h = h / (60.0 / 360);
            int i = (int) Math.floor(h);
            f = h - i;
            p = v * (1 - s);
            q = v * (1 - s * f);
            t = v * (1 - s * (1 - f));

            double rgb[];
            if (i == 0)
                rgb = new double[]{v, t, p};
            else if (i == 1)
                rgb = new double[]{q, v, p};
            else if (i == 2)
                rgb = new double[]{p, v, t};
            else if (i == 3)
                rgb = new double[]{p, q, v};
            else if (i == 4)
                rgb = new double[]{t, p, v};
            else
                rgb = new double[]{v, p, q};

            return new double[]{rgb[0], rgb[1], rgb[2]};
        }
    }

    private double[] updateHSB() {
        double h = 0;
        double s = 0;
        double v = Math.max(Math.max(r, g), b);
        double d = v - Math.min(Math.min(r, g), b);

        if (v != 0)
            s = d / v;

        if (s != 0) {
            if (r == v)
                h = 0 + (g - b) / d;
            else if (g == v)
                h = 2 + (b - r) / d;
            else
                h = 4 + (r - g) / d;
        }

        h = h * (60.0 / 360);
        if (h < 0)
            h = h + 1;

        return new double[]{h, s, v};
    }

    private void updateCMYK() {
        // TODO: implement
    }

    public java.awt.Color getAwtColor() {
        // We don't return the cached awtColor here, since java.awt.Color is mutable.
        return new java.awt.Color((float) getRed(), (float) getGreen(), (float) getBlue(), (float) getAlpha());
    }

    public void set(Graphics2D g) {
        if (awtColor == null) {
            awtColor = new java.awt.Color((float) getRed(), (float) getGreen(), (float) getBlue(), (float) getAlpha());
        }
        g.setColor(awtColor);
    }

    @Override
    public Color clone() {
        return new Color(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Color)) return false;
        Color other = (Color) obj;
        // Because of the conversion to/from hex, we can have rounding errors.
        // Therefore, we only compare what we can store, i.e. values in the 0-255 range.
        return Math.round(r * 255) == Math.round(other.r * 255)
                && Math.round(g * 255) == Math.round(other.g * 255)
                && Math.round(b * 255) == Math.round(other.b * 255)
                && Math.round(a * 255) == Math.round(other.a * 255);
    }

    /**
     * Parse a hexadecimal value and return a Color object.
     * <p/>
     * The value needs to have four components. (R,G,B,A)
     *
     * @param value the hexadecimal color value, e.g. #995423ff
     * @return a Color object.
     */
    public static Color parseColor(String value) {
        return new Color(value);
    }

    private String paddedHexString(int v) {
        String s = Integer.toHexString(v);
        if (s.length() == 1) {
            return "0" + s;
        } else if (s.length() == 2) {
            return s;
        } else {
            throw new AssertionError("Value too large (must be between 0-255, was " + v + ").");
        }
    }

    /**
     * Returns the color as a 8-bit hexadecimal value, e.g. #ae45cdff
     *
     * @return the color as a 8-bit hexadecimal value
     */
    @Override
    public String toString() {
        int r256 = (int) Math.round(r * 255);
        int g256 = (int) Math.round(g * 255);
        int b256 = (int) Math.round(b * 255);
        int a256 = (int) Math.round(a * 255);
        return "#"
                + paddedHexString(r256)
                + paddedHexString(g256)
                + paddedHexString(b256)
                + paddedHexString(a256);
    }

    public String toCSS() {
        if (!isVisible()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        int r256 = (int) Math.round(r * 255);
        int g256 = (int) Math.round(g * 255);
        int b256 = (int) Math.round(b * 255);
        if (a == 1.0) {
            return "#"
                    + paddedHexString(r256)
                    + paddedHexString(g256)
                    + paddedHexString(b256);
        } else {
            return String.format("rgba(%d,%d,%d,%.2f)", r256, g256, b256, a);
        }
    }
}
