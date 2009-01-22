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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederik
 */
public class BezierPath extends Grob {

    // Magic number used for drawing bezier circles.
    // 4 *(sqrt(2) -1)/3
    private static final float KAPPA = 0.5522847498f;

    // To simulate a quarter of a circle.
    private static final double ONE_MINUS_QUARTER = 1.0 - 0.552;

    private ArrayList<PathElement> elements = new ArrayList<PathElement>();
    private Color fillColor;
    private Color strokeColor;
    private double strokeWidth;
    private boolean dirty = true;
    private boolean needsMoveto = true; // Flag to check if we already moved to a start point.
    private transient java.awt.geom.GeneralPath awtPath;
    private transient Rect bounds;
    private double[] segmentLengths;
    private double pathLength = -1;

    public BezierPath() {
        fillColor = new Color();
        strokeColor = new Color();
        strokeWidth = 1;
    }

    public BezierPath(Shape s) {
        fillColor = new Color();
        strokeColor = new Color();
        strokeWidth = 1;
        extend(s);
    }

    public BezierPath(BezierPath other) {
        super(other);
        fillColor = other.fillColor == null ? null : other.fillColor.clone();
        strokeColor = other.strokeColor == null ? null : other.strokeColor.clone();
        strokeWidth = other.strokeWidth;
        extend(other);
    }

    //// Color methods ////

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    //// Path methods ////

    public void moveto(Point pt) {
        moveto(pt.getX(), pt.getY());
    }

    public void moveto(double x, double y) {
        elements.add(new PathElement(PathElement.MOVETO, x, y));
        needsMoveto = false;
        dirty = true;
    }

    public void lineto(Point pt) {
        lineto(pt.getX(), pt.getY());
    }

    public void lineto(double x, double y) {
        if (needsMoveto)
            throw new NodeBoxError("Lineto without first doing moveto.");
        elements.add(new PathElement(PathElement.LINETO, x, y));
        dirty = true;
    }

    public void quadto(Point pt1, Point pt2) {
        quadto(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
    }

    public void quadto(double x1, double y1, double x2, double y2) {
        if (needsMoveto)
            throw new NodeBoxError("Quadto without first doing moveto.");
        PathElement lastElement = elements.get(elements.size() - 1);
        // We don't support quads natively, but we accept them and convert them to a cubic bezier.
        double lastX = lastElement.getX();
        double lastY = lastElement.getY();
        double c1x = lastX + (x1 - lastX) * 2 / 3;
        double c1y = lastY + (y1 - lastY) * 2 / 3;
        double c2x = x2 - (x2 - x1) * 2 / 3;
        double c2y = y2 - (y2 - y1) * 2 / 3;
        curveto(c1x, c1y, c2x, c2y, x2, y2);
    }

    public void curveto(Point pt1, Point pt2, Point pt3) {
        curveto(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), pt3.getX(), pt3.getY());
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
        if (needsMoveto)
            throw new NodeBoxError("Curveto without first doing moveto.");
        elements.add(new PathElement(PathElement.CURVETO, x1, y1, x2, y2, x3, y3));
        dirty = true;
    }

    public void close() {
        elements.add(new PathElement(PathElement.CLOSE));
        // After the path is closed, we need a new moveto to start a new path segment.
        needsMoveto = true;
        dirty = true;
    }

    //// Basic shapes ////

    public void addElement(PathElement el) {
        elements.add(el.clone());
    }

    public void rect(Rect r) {
        rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public void rect(double cx, double cy, double width, double height) {
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        moveto(cx - halfWidth, cy - halfHeight);
        lineto(cx + halfWidth, cy - halfHeight);
        lineto(cx + halfWidth, cy + halfHeight);
        lineto(cx - halfWidth, cy + halfHeight);
        close();
    }

    public void rect(Rect r, double roundness) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), roundness);
    }

    public void rect(Rect r, double rx, double ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void rect(double cx, double cy, double width, double height, double r) {
        roundedRect(cx, cy, width, height, r);
    }

    public void rect(double cx, double cy, double width, double height, double rx, double ry) {
        roundedRect(cx, cy, width, height, rx, ry);
    }

    public void roundedRect(Rect r, double roundness) {
        roundedRect(r, roundness, roundness);
    }

    public void roundedRect(Rect r, double rx, double ry) {
        roundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void roundedRect(double cx, double cy, double width, double height, double r) {
        roundedRect(cx, cy, width, height, r, r);
    }

    public void roundedRect(double cx, double cy, double width, double height, double rx, double ry) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double dx = rx;
        double dy = ry;

        double left = cx - halfWidth;
        double right = cx + halfWidth;
        double top = cy - halfHeight;
        double bottom = cy + halfHeight;
        // rx/ry cannot be greater than half of the width of the retoctangle
        // (required by SVG spec)
        dx = Math.min(dx, width * 0.5);
        dy = Math.min(dy, height * 0.5);
        moveto(left + dx, top);
        if (dx < width * 0.5)
            lineto(right - rx, top);
        curveto(right - dx * ONE_MINUS_QUARTER, top, right, top + dy * ONE_MINUS_QUARTER, right, top + dy);
        if (dy < height * 0.5)
            lineto(right, bottom - dy);
        curveto(right, bottom - dy * ONE_MINUS_QUARTER, right - dx * ONE_MINUS_QUARTER, bottom, right - dx, bottom);
        if (dx < width * 0.5)
            lineto(left + dx, bottom);
        curveto(left + dx * ONE_MINUS_QUARTER, bottom, left, bottom - dy * ONE_MINUS_QUARTER, left, bottom - dy);
        if (dy < height * 0.5)
            lineto(left, top + dy);
        curveto(left, top + dy * ONE_MINUS_QUARTER, left + dx * ONE_MINUS_QUARTER, top, left + dx, top);
        close();
    }

    public void ellipse(double cx, double cy, double width, double height) {
        Ellipse2D.Double e = new Ellipse2D.Double(cx - width / 2, cy - height / 2, width, height);
        extend(e);
    }

    public void line(double x1, double y1, double x2, double y2) {
        moveto(x1, y1);
        lineto(x2, y2);
    }

    public void text(String text, String fontName, double fontSize, double lineHeight, Text.Align align, double x, double y) {
        text(text, fontName, fontSize, lineHeight, align, x, y, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public void text(String text, String fontName, double fontSize, double lineHeight, Text.Align align, double x, double y, double width) {
        text(text, fontName, fontSize, lineHeight, align, x, y, width, Double.MAX_VALUE);
    }

    public void text(String text, String fontName, double fontSize, double lineHeight, Text.Align align, double x, double y, double width, double height) {
        Text t = new Text(text, x, y, width, height);
        t.setFontName(fontName);
        t.setFontSize(fontSize);
        t.setLineHeight(lineHeight);
        t.setAlign(align);
        extend(t.getPath());
    }

    //// Contours ////

    /**
     * Returns a list of contours in the path.
     * <p/>
     * A contour is a sequence of lines and curves
     * separated from the next contour by a MOVETO.
     * <p/>
     * For example, the glyph "o" has two contours:
     * the inner circle and the outer circle.
     *
     * @return a list of BezierPaths.
     */
    public java.util.List<BezierPath> getContours() {
        java.util.List<BezierPath> contours = new ArrayList<BezierPath>();
        BezierPath currentContour = null;
        boolean empty = true;
        for (PathElement el : getElements()) {
            int command = el.getCommand();
            if (command == PathElement.MOVETO) {
                if (!empty) {
                    contours.add(currentContour);
                }
                // Clone and clear copies only the properties (fill, stroke) of this path.
                currentContour = clone();
                currentContour.clear();
                currentContour.addElement(el);
                empty = true;
            } else if (command == PathElement.LINETO || command == PathElement.CURVETO) {
                assert (currentContour != null);
                currentContour.addElement(el);
                empty = false;
            } else if (command == PathElement.CLOSE) {
                assert (currentContour != null);
                currentContour.addElement(el);
            } else {
                throw new AssertionError("Unknown path command " + command);
            }
        }
        if (!empty) {
            contours.add(currentContour);
        }
        return contours;
    }

    //// Mathematics ////


    /**
     * Returns the length of the line.
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X end coordinate
     * @param y1 Y end coordinate
     * @return the length of the line
     */
    public static double lineLength(double x0, double y0, double x1, double y1) {
        x0 = Math.abs(x0 - x1);
        x0 *= x0;
        y0 = Math.abs(y0 - y1);
        y0 *= y0;
        return Math.sqrt(x0 + y0);
    }

    /**
     * Returns coordinates for point at t on the line.
     * <p/>
     * Calculates the coordinates of x and y for a point
     * at t on a straight line.
     * <p/>
     * The t parameter is a number between 0.0 and 1.0,
     * x0 and y0 define the starting point of the line,
     * x1 and y1 the ending point of the line,
     *
     * @param t  a number between 0.0 and 1.0 defining the position on the path.
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X end coordinate
     * @param y1 Y end coordinate
     * @return a Point at position t on the line.
     */
    public static Point linePoint(double t, double x0, double y0, double x1, double y1) {
        return new Point(
                x0 + t * (x1 - x0),
                y0 + t * (y1 - y0));
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * lineair lines between points at t.
     * <p/>
     * The number of points is defined by n
     * (n=10 would add the lengths of lines between 0.0 and 0.1,
     * between 0.1 and 0.2, and so on).
     * <p/>
     * This will use a default accuracy of 20, which is fine for most cases, usually
     * resulting in a deviation of less than 0.01.
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @return the length of the spline.
     */
    public static double curveLength(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        return curveLength(x0, y0, x1, y1, x2, y2, x3, y3, 20);
    }

    /**
     * Returns the length of the spline.
     * <p/>
     * Integrates the estimated length of the cubic bezier spline
     * defined by x0, y0, ... x3, y3, by adding the lengths of
     * lineair lines between points at t.
     * <p/>
     * The number of points is defined by n
     * (n=10 would add the lengths of lines between 0.0 and 0.1,
     * between 0.1 and 0.2, and so on).
     *
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @param n  accuracy
     * @return the length of the spline.
     */
    public static double curveLength(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, int n) {
        double length = 0;
        double xi = x0;
        double yi = y0;
        double t;
        double px, py;
        double tmpX, tmpY;
        for (int i = 0; i < n; i++) {
            t = (i + 1) / (double) n;
            Point pt = curvePoint(t, x0, y0, x1, y1, x2, y2, x3, y3);
            px = pt.getX();
            py = pt.getY();
            tmpX = Math.abs(xi - px);
            tmpX *= tmpX;
            tmpY = Math.abs(yi - py);
            tmpY *= tmpY;
            length += Math.sqrt(tmpX + tmpY);
            xi = px;
            yi = py;
        }
        return length;
    }

    /**
     * Returns coordinates for point at t on the spline.
     * <p/>
     * Calculates the coordinates of x and y for a point
     * at t on the cubic bezier spline, and its control points,
     * based on the de Casteljau interpolation algorithm.
     *
     * @param t  a number between 0.0 and 1.0 defining the position on the path.
     * @param x0 X start coordinate
     * @param y0 Y start coordinate
     * @param x1 X control point 1
     * @param y1 Y control point 1
     * @param x2 X control point 2
     * @param y2 Y control point 2
     * @param x3 X end coordinate
     * @param y3 Y end coordinate
     * @return a Point at position t on the spline.
     */
    public static Point curvePoint(double t, double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        double mint = 1 - t;
        double x01 = x0 * mint + x1 * t;
        double y01 = y0 * mint + y1 * t;
        double x12 = x1 * mint + x2 * t;
        double y12 = y1 * mint + y2 * t;
        double x23 = x2 * mint + x3 * t;
        double y23 = y2 * mint + y3 * t;

        double out_c1x = x01 * mint + x12 * t;
        double out_c1y = y01 * mint + y12 * t;
        double out_c2x = x12 * mint + x23 * t;
        double out_c2y = y12 * mint + y23 * t;
        double out_x = out_c1x * mint + out_c2x * t;
        double out_y = out_c1y * mint + out_c2y * t;
        return new Point(out_x, out_y);
    }

    public double[] getSegmentLengths() {
        return getSegmentLengths(20);
    }

    public double[] getSegmentLengths(int n) {
        if (segmentLengths != null) return segmentLengths;
        segmentLengths = new double[elements.size() - 1];
        double length;
        pathLength = 0;
        double closeX = 0;
        double closeY = 0;
        double x0 = 0;
        double y0 = 0;
        boolean first = true;
        int i = 0;
        for (PathElement el : elements) {
            if (first) {
                closeX = el.getX();
                closeY = el.getY();
                first = false;
                // At the end of the loop, i will be incremented, setting it to 0.
                i = -1;
            } else if (el.getCommand() == PathElement.MOVETO) {
                closeX = el.getX();
                closeY = el.getY();
                segmentLengths[i] = 0.0;
            } else if (el.getCommand() == PathElement.CLOSE) {
                length = lineLength(x0, y0, closeX, closeY);
                segmentLengths[i] = length;
                pathLength += length;
            } else if (el.getCommand() == PathElement.LINETO) {
                length = lineLength(x0, y0, el.getX(), el.getY());
                segmentLengths[i] = length;
                pathLength += length;
            } else if (el.getCommand() == PathElement.CURVETO) {
                length = curveLength(x0, y0,
                        el.getControl1().getX(), el.getControl1().getY(),
                        el.getControl2().getX(), el.getControl2().getY(),
                        el.getX(), el.getY(), n);
                segmentLengths[i] = length;
                pathLength += length;
            }
            if (el.getCommand() != PathElement.CLOSE) {
                x0 = el.getX();
                y0 = el.getY();
            }
            i++;
        }
        assert (i == segmentLengths.length);
        return segmentLengths;
    }

    /**
     * Calculate the length of the path. This is not the number of segments, but rather the sum of all segment lengths.
     *
     * @return the length of the path.
     */
    public double getLength() {
        if (segmentLengths == null)
            getSegmentLengths();
        assert (pathLength != -1);
        return pathLength;
    }

    /**
     * Returns coordinates for point at t on the path.
     * <p/>
     * Gets the length of the path, based on the length
     * of each curve and line in the path.
     * Determines in what segment t falls.
     * Gets the point on that segment.
     *
     * @param t relative coordinate of the point (between 0.0 and 1.0)
     * @return coordinates for point at t.
     */
    public Point getPoint(double t) {
        if (segmentLengths == null)
            getSegmentLengths();

        // Check if there is a path.
        if (pathLength <= 0)
            throw new NodeBoxError("The path is empty.");

        // Since t is relative, convert it to the absolute length.
        double absT = t * pathLength;
        // The resT is what remains of t after we traversed all segments.
        double resT = t;

        // Find the segment that contains t.
        int i = 0;
        double closeX = 0;
        double closeY = 0;
        for (PathElement el : elements) {
            if (i == 0 || el.getCommand() == PathElement.MOVETO) {
                closeX = el.getX();
                closeY = el.getY();
            }
            if (absT <= segmentLengths[i] || i == segmentLengths.length - 1)
                break;
            absT -= segmentLengths[i];
            resT -= segmentLengths[i] / pathLength;
            i++;
        }
        if (segmentLengths[i] != 0) {
            absT /= segmentLengths[i];
            resT /= segmentLengths[i] / pathLength;
        }
        if (i == segmentLengths.length - 1 && segmentLengths[i] == 0)
            i--;

        // Get the element for this (and the next) segment.
        // Calculate the point on that segment.
        PathElement el0 = elements.get(i);
        PathElement el1 = elements.get(i + 1);
        // The resT is what remains of t after we traversed all segments.
        //double resT = absT / pathLength;
        switch (el1.getCommand()) {
            case PathElement.CLOSE:
                return linePoint(resT, el0.getX(), el0.getY(), closeX, closeY);
            case PathElement.LINETO:
                return linePoint(resT, el0.getX(), el0.getY(), el1.getX(), el1.getY());
            case PathElement.CURVETO:
                return curvePoint(resT,
                        el0.getX(), el0.getY(),
                        el1.getControl1().getX(), el1.getControl1().getY(),
                        el1.getControl2().getX(), el1.getControl2().getY(),
                        el1.getX(), el1.getY());
            default:
                return new Point(); // throw new AssertionError("Unknown path command for p1: " + el1);
        }
    }

    public Point[] getPoints() {
        return getPoints(100);
    }

    public Point[] getPoints(int amount) {
        Point[] points = new Point[amount];
        // The delta value is divided by amount - 1, because we also want the last point (t=1.0)
        // If I wouldn't use amount - 1, I fall one point short of the end.
        // E.g. if amount = 4, I want point at t 0.0, 0.33, 0.66 and 1.0,
        // if amount = 2, I want point at t 0.0 and t 1.0
        double delta = 1.0;
        if (amount > 2) {
            delta = 1.0 / (amount - 1);
        }
        for (int i = 0; i < amount; i++) {
            points[i] = getPoint(delta * i);
        }
        return points;
    }

    //// Geometric queries ////

    public boolean contains(Point p) {
        return getGeneralPath().contains(p.getPoint2D());
    }

    public boolean contains(double x, double y) {
        return getGeneralPath().contains(x, y);
    }

    public boolean contains(Rect r) {
        return getGeneralPath().contains(r.getRectangle2D());
    }

    //// Boolean operations ////

    public boolean intersects(Rect r) {
        return getGeneralPath().intersects(r.getRectangle2D());
    }

    public boolean intersects(BezierPath p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.intersect(a2);
        return !a1.isEmpty();
    }

    public BezierPath intersected(BezierPath p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.intersect(a2);
        return new BezierPath(a1);
    }

    public BezierPath subtracted(BezierPath p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.subtract(a2);
        return new BezierPath(a1);
    }

    public BezierPath united(BezierPath p) {
        Area a1 = new Area(getGeneralPath());
        Area a2 = new Area(p.getGeneralPath());
        a1.add(a2);
        return new BezierPath(a1);
    }

    //// List operations ////

    public List<PathElement> getElements() {
        return elements;
    }

    public PathElement getElementAt(int index) {
        return elements.get(index);
    }

    public void clear() {
        elements.clear();
    }

    public int size() {
        return elements.size();
    }

    public void append(PathElement el) {
        elements.add(el);
    }

    public void extend(BezierPath p) {
        for (PathElement el : p.getElements()) {
            append(el.clone());
        }
    }

    public void extend(Shape s) {
        PathIterator pi = s.getPathIterator(new AffineTransform());
        while (!pi.isDone()) {
            float[] points = new float[6];
            int cmd = pi.currentSegment(points);
            if (cmd == PathIterator.SEG_MOVETO) {
                moveto(points[0], points[1]);
            } else if (cmd == PathIterator.SEG_LINETO) {
                lineto(points[0], points[1]);
            } else if (cmd == PathIterator.SEG_QUADTO) {
                quadto(points[0], points[1], points[2], points[3]);
            } else if (cmd == PathIterator.SEG_CUBICTO) {
                curveto(points[0], points[1], points[2], points[3], points[4], points[5]);
            } else if (cmd == PathIterator.SEG_CLOSE) {
                close();
            } else {
                throw new AssertionError("Unknown path command " + cmd);
            }
            pi.next();
        }
    }

    public void extend(List<Point> points) {
        boolean first = true;
        for (Point pt : points) {
            if (first) {
                moveto(pt);
                first = false;
            } else {
                lineto(pt);
            }
        }
    }

    public void extend(Point[] points) {
        boolean first = true;
        for (Point pt : points) {
            if (first) {
                moveto(pt);
                first = false;
            } else {
                lineto(pt);
            }
        }
    }


    //// Geometry ////

    public java.awt.geom.GeneralPath getGeneralPath() {
        if (!dirty) {
            return awtPath;
        }
        java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
        Point c1, c2;
        for (PathElement el : elements) {
            switch (el.getCommand()) {
                case PathElement.MOVETO:
                    gp.moveTo((float) el.getX(), (float) el.getY());
                    break;
                case PathElement.LINETO:
                    gp.lineTo((float) el.getX(), (float) el.getY());
                    break;
                case PathElement.CURVETO:
                    c1 = el.getControl1();
                    c2 = el.getControl2();
                    gp.curveTo((float) c1.getX(), (float) c1.getY(),
                            (float) c2.getX(), (float) c2.getY(),
                            (float) el.getX(), (float) el.getY());
                    break;
                case PathElement.CLOSE:
                    gp.closePath();
                    break;
                default:
                    throw new AssertionError("Unknown path command " + el.getCommand());
            }
        }
        awtPath = gp;
        dirty = false;
        return gp;

    }

    public Rect getBounds() {
        if (!dirty) return bounds;
        bounds = new Rect(getGeneralPath().getBounds2D());
        return bounds;
    }

    public void draw(Graphics2D g) {
        setupTransform(g);
        if (fillColor != null && fillColor.isVisible()) {
            g.setColor(fillColor.getAwtColor());
            g.fill(getGeneralPath());
        }
        if (strokeWidth > 0 && strokeColor != null && strokeColor.isVisible()) {
            try {
                g.setColor(strokeColor.getAwtColor());
                g.setStroke(new BasicStroke((float) strokeWidth));
                g.draw(getGeneralPath());
            } catch (Exception e) {
                // Invalid transformations can cause the pen to not display.
                // Catch the exception and throw it away.
                // The path would be too small to be displayed anyway.
            }
        }
        restoreTransform(g);
    }

    @Override
    public BezierPath clone() {
        return new BezierPath(this);
    }

    /**
     * Creates a cloned empty copy of this object, so only the properties remain, but not the contents.
     * It copies:
     * <ul>
     * <li>transform</li>
     * <li>fillColor</li>
     * <li>strokeColor</li>
     * <li>strokeWidth</li>
     * </ul>
     *
     * @return a new empty BezierPath with the same properties as this path.
     */
    public BezierPath cloneAndClear() {
        BezierPath cloned = new BezierPath();
        cloned.setTransform(getTransform().clone());
        cloned.fillColor = fillColor == null ? null : fillColor.clone();
        cloned.strokeColor = strokeColor == null ? null : strokeColor.clone();
        cloned.strokeWidth = strokeWidth;
        return cloned;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BezierPath)) return false;
        BezierPath other = (BezierPath) obj;
        return elements.equals(other.elements)
                && fillColor.equals(other.fillColor)
                && strokeColor.equals(other.strokeColor)
                && strokeWidth == other.strokeWidth
                && super.equals(other);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("BezierPath(");
        for (PathElement el : elements) {
            sb.append(el.toString());
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

}
