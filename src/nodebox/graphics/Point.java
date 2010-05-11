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

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Frederik
 */
public class Point implements Iterable<Float> {

    public static final int LINE_TO = 1;
    public static final int CURVE_TO = 2;
    public static final int CURVE_DATA = 3;

    public float x, y;
    public int type;

    public Point() {
        this(0, 0);
    }

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
        this.type = LINE_TO;
    }

    public Point(float x, float y, int type) {
        this.x = x;
        this.y = y;
        if (type < LINE_TO || type > CURVE_DATA) {
            throw new IllegalArgumentException("Invalid point type.");
        }
        this.type = type;
    }

    public Point(Point pt) {
        this.x = pt.x;
        this.y = pt.y;
        this.type = pt.type;
    }

    public Point(Point2D pt) {
        this.x = (float) pt.getX();
        this.y = (float) pt.getY();
        this.type = LINE_TO;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        if (type < LINE_TO || type > CURVE_DATA) {
            throw new IllegalArgumentException("Invalid point type.");
        }
        this.type = type;
    }

    public boolean isLineTo() {
        return type == LINE_TO;
    }

    public boolean isCurveTo() {
        return type == CURVE_TO;
    }

    public boolean isOnCurve() {
        return type != CURVE_DATA;
    }

    public boolean isOffCurve() {
        return type == CURVE_DATA;
    }

    public void move(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public Point2D getPoint2D() {
        return new Point2D.Float(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y && type == p.type;
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }

    @Override
    public Point clone() {
        return new Point(this);
    }

    public Iterator<Float> iterator() {
        return new Iterator<Float>() {
            int pos = 0;

            public boolean hasNext() {
                return pos < 2;
            }

            public Float next() {
                if (pos >= 2) throw new NoSuchElementException("A point has only two elements.");
                return (pos++) == 0 ? x : y;
            }

            public void remove() {
            }
        };
    }
}
