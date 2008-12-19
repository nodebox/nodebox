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
        new Color(r, g, b, 1.0);
    }

    public Color(double r, double g, double b, double a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public Color(String colorName) {
        // TODO: Implement
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
}
