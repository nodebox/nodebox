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

import static com.google.common.base.Preconditions.checkArgument;

public class PathElement {

    public static final int MOVETO = 0;
    public static final int LINETO = 1;
    public static final int CURVETO = 2;
    public static final int CLOSE = 3;

    private final int command;
    private final Point point, control1, control2;

    public PathElement() {
        this(MOVETO, Point.ZERO, Point.ZERO, Point.ZERO);
    }

    public PathElement(int command) {
        checkArgument(command == CLOSE, "Command needs to be CLOSE.");
        this.command = CLOSE;
        this.point = Point.ZERO;
        this.control1 = Point.ZERO;
        this.control2 = Point.ZERO;
    }

    public PathElement(int command, double x, double y) {
        checkArgument(command == MOVETO || command == LINETO, "Command needs to be MOVETO or LINETO.");
        this.command = command;
        this.point = new Point(x, y);
        this.control1 = Point.ZERO;
        this.control2 = Point.ZERO;
    }

    public PathElement(int command, double x1, double y1, double x2, double y2, double x3, double y3) {
        checkArgument(command == CURVETO, "Command needs to be CURVETO.");
        this.command = command;
        this.control1 = new Point(x1, y1);
        this.control2 = new Point(x2, y2);
        this.point = new Point(x3, y3);
    }

    public PathElement(int command, double[] points) {
        this.command = command;
        switch (command) {
            case MOVETO:
            case LINETO:
                checkArgument(points.length == 2, "MOVETO or LINETO commands requires 2 points.");
                this.point = new Point(points[0], points[1]);
                this.control1 = Point.ZERO;
                this.control2 = Point.ZERO;
                break;
            case CURVETO:
                checkArgument(points.length == 6, "CURVETO command requires 6 points.");
                this.control1 = new Point(points[0], points[1]);
                this.control2 = new Point(points[2], points[3]);
                this.point = new Point(points[4], points[5]);
                break;

            case CLOSE:
                checkArgument(points.length == 0, "CLOSE command requires no points.");
                this.point = Point.ZERO;
                this.control1 = Point.ZERO;
                this.control2 = Point.ZERO;
                break;
            default:
                throw new IllegalArgumentException("Unknown command" + command);
        }
    }

    public PathElement(int command, Point point, Point control1, Point control2) {
        this.command = command;
        this.point = point;
        this.control1 = control1;
        this.control2 = control2;
    }

    public int getCommand() {
        return command;
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

    public Point getControl1() {
        return control1;
    }

    public Point getControl2() {
        return control2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathElement)) return false;
        final PathElement other = (PathElement) o;
        return Objects.equal(command, other.command)
                && Objects.equal(point, other.point)
                && Objects.equal(control1, other.control1)
                && Objects.equal(control2, other.control2);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(command, point, control1, control2);
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

}
