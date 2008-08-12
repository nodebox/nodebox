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

    public static final int COMMAND_MOVETO = 0;
    public static final int COMMAND_LINETO = 1;
    public static final int COMMAND_CURVETO = 2;
    public static final int COMMAND_CLOSE = 3;

    private int command;
    private Point point, control1, control2;

    public PathElement() {

    }

    public PathElement(int command) {
        assert (command == COMMAND_CLOSE);
        this.command = command;
        this.point = new Point(0, 0);
        this.control1 = new Point(0, 0);
        this.control2 = new Point(0, 0);
    }

    public PathElement(int command, double x, double y) {
        assert (command == COMMAND_MOVETO || command == COMMAND_LINETO);
        this.command = command;
        this.point = new Point(x, y);
        this.control1 = new Point(0, 0);
        this.control2 = new Point(0, 0);
    }

    public PathElement(int command, double x1, double y1, double x2, double y2, double x3, double y3) {
        assert (command == COMMAND_CLOSE);
        this.command = command;
        this.control1 = new Point(x1, y1);
        this.control2 = new Point(x2, y2);
        this.point = new Point(x3, y3);
    }

    public int getCommand() {
        return command;
    }

    public Point getControl1() {
        return control1;
    }

    public Point getControl2() {
        return control2;
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
            case COMMAND_MOVETO:
                return "PathElement(COMMAND_MOVETO, " + getX() + ", " + getY() + ")";
            case COMMAND_LINETO:
                return "PathElement(COMMAND_LINETO, " + getX() + ", " + getY() + ")";
            case COMMAND_CURVETO:
                return "PathElement(COMMAND_CURVETO, " + getControl1().getX() + ", " + getControl1().getY()
                        + ", " + getControl2().getX() + ", " + getControl2().getY() + ", "
                        + getX() + ", " + getY() + ")";
            case COMMAND_CLOSE:
                return "PathElement(COMMAND_CLOSE)";
        }
        throw new AssertionError("Invalid PathElement command " + command);
    }
}
