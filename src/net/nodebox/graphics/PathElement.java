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

/**
 * @author Frederik
 */
public class PathElement {

    public static final int MOVETO = 0;
    public static final int LINETO = 1;
    public static final int CURVETO = 2;
    public static final int CLOSE = 3;

    private int command;
    private Point point, control1, control2;

    public PathElement() {

    }

    public PathElement(int command) {
        assert (command == CLOSE);
        this.command = command;
        this.point = new Point(0, 0);
        this.control1 = new Point(0, 0);
        this.control2 = new Point(0, 0);
    }

    public PathElement(int command, double x, double y) {
        assert (command == MOVETO || command == LINETO);
        this.command = command;
        this.point = new Point(x, y);
        this.control1 = new Point(0, 0);
        this.control2 = new Point(0, 0);
    }

    public PathElement(int command, double x1, double y1, double x2, double y2, double x3, double y3) {
        assert (command == CURVETO);
        this.command = command;
        this.control1 = new Point(x1, y1);
        this.control2 = new Point(x2, y2);
        this.point = new Point(x3, y3);
    }

    public PathElement(int command, float[] points) {
        this.command = command;
        switch (command) {
            case MOVETO:
            case LINETO:
                assert (points.length == 2);
                this.point = new Point(points[0], points[1]);
                break;
            case CURVETO:
                assert (points.length == 6);
                this.control1 = new Point(points[0], points[1]);
                this.control2 = new Point(points[2], points[3]);
                this.point = new Point(points[4], points[5]);
                break;

            case CLOSE:
                assert (points.length == 0);
                break;
            default:
                throw new AssertionError("Unknown command" + command);
        }
    }

    public PathElement(PathElement other) {
        this.command = other.command;
        this.control1 = other.control1 == null ? null : other.control1.clone();
        this.control2 = other.control2 == null ? null : other.control2.clone();
        this.point = other.point == null ? null : other.point.clone();
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public Point getPoint() {
        return point;
    }

    public double getX() {
        return point.getX();
    }

    public double getY() {
        return point.getY();
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public Point getControl1() {
        return control1;
    }

    public void setControl1(Point control1) {
        this.control1 = control1;
    }

    public Point getControl2() {
        return control2;
    }

    public void setControl2(Point control2) {
        this.control2 = control2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathElement)) return false;
        PathElement el = (PathElement) o;
        return command == el.command &&
                point.equals(el.point) &&
                control1.equals(el.control1) &&
                control2.equals(el.control2);
    }

    @Override
    public String toString() {
        switch (command) {
            case MOVETO:
                return "PathElement(Command.MOVETO, " + getX() + ", " + getY() + ")";
            case LINETO:
                return "PathElement(Command.LINETO, " + getX() + ", " + getY() + ")";
            case CURVETO:
                return "PathElement(Command.CURVETO, " + getControl1().getX() + ", " + getControl1().getY()
                        + ", " + getControl2().getX() + ", " + getControl2().getY() + ", "
                        + getX() + ", " + getY() + ")";
            case CLOSE:
                return "PathElement(Command.CLOSE)";
        }
        throw new AssertionError("Invalid PathElement command " + command);
    }

    @Override
    public PathElement clone() {
        return new PathElement(this);
    }
}
