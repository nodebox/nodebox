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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Color implements Cloneable {

    private static final Pattern HEX_STRING_PATTERN = Pattern.compile("^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$");

    public enum Mode {
        RGB, HSB, CMYK
    }

    private double r, g, b, a;
    private double h, s, v;

    public static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    public static Color fromHSB(double hue, double saturation, double brightness) {
        return new Color(hue, saturation, brightness, Mode.HSB);
    }


    public static Color fromHSB(double hue, double saturation, double brightness, double alpha) {
        return new Color(hue, saturation, brightness, alpha, Mode.HSB);
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
        switch (m) {
            case RGB:
                this.r = clamp(x);
                this.g = clamp(y);
                this.b = clamp(z);
                this.a = clamp(a);
                updateHSB();
                updateCMYK();
                break;
            case HSB:
                this.h = clamp(x);
                this.s = clamp(y);
                this.v = clamp(z);
                this.a = clamp(a);
                updateRGB();
                updateCMYK();
                break;
            case CMYK:
                throw new RuntimeException("CMYK color mode is not implemented yet.");
        }
    }

    public Color(String colorName) {
        // We can only parse RGBA hex color values.
        Matcher m = HEX_STRING_PATTERN.matcher(colorName);
        if (!m.matches())
            throw new IllegalArgumentException("The given value '" + colorName + "' is not of the format #112233ff.");
        int r255 = Integer.parseInt(m.group(1), 16);
        int g255 = Integer.parseInt(m.group(2), 16);
        int b255 = Integer.parseInt(m.group(3), 16);
        int a255 = Integer.parseInt(m.group(4), 16);
        this.r = r255 / 255.0;
        this.g = g255 / 255.0;
        this.b = b255 / 255.0;
        this.a = a255 / 255.0;
        updateHSB();
        updateCMYK();
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
        this.r = color.getRed() / 255.0;
        this.g = color.getGreen() / 255.0;
        this.b = color.getBlue() / 255.0;
        this.a = color.getAlpha() / 255.0;
        updateHSB();
        updateCMYK();
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
        this.r = other.r;
        this.g = other.g;
        this.b = other.b;
        this.a = other.a;
        updateHSB();
        updateCMYK();
    }

    public double getRed() {
        return r;
    }

    public double getR() {
        return r;
    }

    public void setRed(double r) {
        this.r = clamp(r);
        updateHSB();
        updateCMYK();
    }

    public void setR(double r) {
        setRed(r);
    }

    public double getGreen() {
        return g;
    }

    public double getG() {
        return g;
    }

    public void setGreen(double g) {
        this.g = clamp(g);
        updateHSB();
        updateCMYK();
    }

    public void setG(double g) {
        setGreen(g);
    }

    public double getBlue() {
        return b;
    }

    public double getB() {
        return b;
    }

    public void setBlue(double b) {
        this.b = clamp(b);
        updateHSB();
        updateCMYK();
    }

    public void setB(double b) {
        setBlue(b);
    }

    public double getAlpha() {
        return a;
    }

    public double getA() {
        return a;
    }

    public void setAlpha(double a) {
        this.a = clamp(a);
        updateHSB();
        updateCMYK();
    }

    public void setA(double a) {
        setAlpha(a);
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

    public void setHue(double h) {
        this.h = clamp(h);
        updateRGB();
        updateCMYK();
    }

    public void setH(double h) {
        setHue(h);
    }

    public double getSaturation() {
        return s;
    }

    public double getS() {
        return s;
    }

    public void setSaturation(double s) {
        this.s = clamp(s);
        updateRGB();
        updateCMYK();
    }

    public void setS(double s) {
        setSaturation(s);
    }

    public double getBrightness() {
        return v;
    }

    public double getV() {
        return v;
    }

    public void setBrightness(double v) {
        this.v = clamp(v);
        updateRGB();
        updateCMYK();
    }

    public void setV(double v) {
        setBrightness(v);
    }

    private void updateRGB() {
        if (s == 0)
            this.r = this.g = this.b = this.v;
        else {
            double h = this.h;
            if (this.h == 1.0)
                h = 0.999998;
            double s = this.s;
            double v = this.v;
            double f, p, q, t;
            h = h / (60.0/360);
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

            this.r = rgb[0];
            this.g = rgb[1];
            this.b = rgb[2];
        }
    }

    private void updateHSB() {
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

        h = h * (float) (60.0 / 360);
        if (h < 0)
            h = h + 1;

        this.h = h;
        this.s = s;
        this.v = v;
    }

    private void updateCMYK() {
        // TODO: implement
    }

    public java.awt.Color getAwtColor() {
        return new java.awt.Color((float) getRed(), (float) getGreen(), (float) getBlue(), (float) getAlpha());
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
}
