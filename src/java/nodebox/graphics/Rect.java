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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class Rect implements Iterable {

    public static Rect centeredRect(double cx, double cy, double width, double height) {
        return new Rect(cx - width / 2, cy - height / 2, width, height);
    }

    public static Rect centeredRect(Rect r) {
        return centeredRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public static Rect corneredRect(double cx, double cy, double width, double height) {
        return new Rect(cx + width / 2, cy + height / 2, width, height);
    }

    public static Rect corneredRect(Rect r) {
        return corneredRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public final double x, y, width, height;

    public Rect() {
        this(0, 0, 0, 0);
    }

    public Rect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect(Rect r) {
        this(r.x, r.y, r.width, r.height);
    }

    public Rect(java.awt.geom.Rectangle2D r) {
        this(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Point getCentroid() {
        return new Point(x + width / 2, y + height / 2);
    }

    public boolean isEmpty() {
        Rect n = normalized();
        return n.width <= 0 || n.height <= 0;
    }

    public Rect normalized() {
        double x = this.x;
        double y = this.y;
        double width = this.width;
        double height = this.height;


        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
        return new Rect(x, y, width, height);
    }

    public Rect united(Rect r) {
        Rect r1 = normalized();
        Rect r2 = r.normalized();

        double x, y, width, height;
        x = Math.min(r1.x, r2.x);
        y = Math.min(r1.y, r2.y);
        width = Math.max(r1.x + r1.width, r2.x + r2.width) - x;
        height = Math.max(r1.y + r1.height, r2.y + r2.height) - y;
        return new Rect(x, y, width, height);
    }

    public boolean intersects(Rect r) {
        Rect r1 = normalized();
        Rect r2 = r.normalized();
        return Math.max(r1.x, r1.y) < Math.min(r1.x + r1.width, r2.width) &&
                Math.max(r1.y, r2.y) < Math.min(r1.y + r1.height, r2.y + r2.height);
    }

    public boolean contains(Point p) {
        Rect r = normalized();
        return p.getX() >= r.x && p.getX() <= r.x + r.width &&
                p.getY() >= r.y && p.getY() <= r.y + r.height;
    }

    public boolean contains(Rect r) {
        Rect r1 = normalized();
        Rect r2 = r.normalized();
        return r2.x >= r1.x && r2.x + r2.width <= r1.x + r1.width &&
                r2.y >= r1.y && r2.y + r2.height <= r1.y + r1.height;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rect)) return false;
        final Rect other = (Rect) o;
        return Objects.equal(x, other.x)
                && Objects.equal(y, other.y)
                && Objects.equal(width, other.width)
                && Objects.equal(height, other.height);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, width, height);
    }

    @Override
    public String toString() {
        return "Rect(" + x + ", " + y + ", " + width + ", " + height + ")";
    }

    public Rectangle2D getRectangle2D() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    public Iterator<Double> iterator() {
        return ImmutableList.<Double>of(x, y, width, height).iterator();
    }

}
