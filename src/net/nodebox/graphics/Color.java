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

package net.nodebox.graphics;

public class Color implements Cloneable {

    private double r, g, b, a;

    public static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    public Color() {
        r = g = b = 0.0;
        a = 1.0;
    }

    public Color(double r, double g, double b) {
        this(r, g, b, 1.0);
    }

    public Color(double r, double g, double b, double a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public Color(String colorName) {
        // RGB hex color.
        if (colorName.startsWith("#")) {
            if (colorName.length() == 9) { // #rrggbbaa
                String rr = colorName.substring(1, 3);
                String gg = colorName.substring(3, 5);
                String bb = colorName.substring(5, 7);
                String aa = colorName.substring(7);
                int r = Integer.parseInt(rr, 16);
                int g = Integer.parseInt(gg, 16);
                int b = Integer.parseInt(bb, 16);
                int a = Integer.parseInt(aa, 16);
                this.r = r / 255.0;
                this.g = g / 255.0;
                this.b = b / 255.0;
                this.a = a / 255.0;
            } else {
                throw new IllegalArgumentException("Inapropriate length for color value (" + colorName + ")");
            }
        } else {
            throw new IllegalArgumentException("Only hexadecimal values (e.g. #337711ff) are accepted. (" + colorName + ")");
        }
    }

    public Color(java.awt.Color color) {
        this.r = color.getRed() / 255.0;
        this.g = color.getGreen() / 255.0;
        this.b = color.getBlue() / 255.0;
        this.a = color.getAlpha() / 255.0;
    }

    public Color(Color other) {
        this.r = other.r;
        this.g = other.g;
        this.b = other.b;
        this.a = other.a;
    }

    public double getRed() {
        return r;
    }

    public double getGreen() {
        return g;
    }

    public double getBlue() {
        return b;
    }

    public double getAlpha() {
        return a;
    }

    public boolean isVisible() {
        return a > 0.0;
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
        return r == other.r && g == other.g && b == other.b && a == other.a;
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
