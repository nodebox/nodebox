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
    private Color fillColor = new Color();
    private Color strokeColor = new Color();
    private double strokeWidth = 1;
    private boolean dirty = true;
    private transient java.awt.geom.GeneralPath awtPath;

    public BezierPath() {
    }

    public BezierPath(Shape s) {
        extend(s);
    }

    public BezierPath(BezierPath other) {
        super(other);
        elements = (ArrayList<PathElement>) other.elements.clone();
        fillColor = other.fillColor == null ? null : other.fillColor.clone();
        strokeColor = other.strokeColor == null ? null : other.strokeColor.clone();
        strokeWidth = other.strokeWidth;
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

    public void moveto(double x, double y) {
        elements.add(new PathElement(PathElement.MOVETO, x, y));
        dirty = true;
    }

    public void lineto(double x, double y) {
        elements.add(new PathElement(PathElement.LINETO, x, y));
        dirty = true;
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
        elements.add(new PathElement(PathElement.CURVETO, x1, y1, x2, y2, x3, y3));
        dirty = true;
    }

    public void close() {
        elements.add(new PathElement(PathElement.CLOSE));
        dirty = true;
    }

    //// Basic shapes ////

    public void addElement(PathElement el) {
        elements.add(el.clone());
    }

    public void addRect(Rect r) {
        addRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public void addRect(double x, double y, double width, double height) {
        moveto(x, y);
        lineto(x + width, y);
        lineto(x + width, y + height);
        lineto(x, y + height);
        close();
    }

    public void addRoundedRect(Rect r, double rx, double ry) {
        addRoundedRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx, ry);
    }

    public void addRoundedRect(double x, double y, double width, double height, double rx, double ry) {
        double dx = rx;
        double dy = ry;
        // rx/ry cannot be greater than half of the width of the rectangle
        // (required by SVG spec)
        dx = Math.min(dx, width * 0.5);
        dy = Math.min(dy, height * 0.5);
        moveto(x + dx, y);
        if (dx < width * 0.5)
            lineto(x + width - rx, y);
        curveto(x + width - dx * ONE_MINUS_QUARTER, y, x + width, y + dy * ONE_MINUS_QUARTER, x + width, y + dy);
        if (dy < height * 0.5)
            lineto(x + width, y + height - dy);
        curveto(x + width, y + height - dy * ONE_MINUS_QUARTER, x + width - dx * ONE_MINUS_QUARTER, y + height, x + width - dx, y + height);
        if (dx < width * 0.5)
            lineto(x + dx, y + height);
        curveto(x + dx * ONE_MINUS_QUARTER, y + height, x, y + height - dy * ONE_MINUS_QUARTER, x, y + height - dy);
        if (dy < height * 0.5)
            lineto(x, y + dy);
        curveto(x, y + dy * ONE_MINUS_QUARTER, x + dx * ONE_MINUS_QUARTER, y, x + dx, y);
        close();
    }

    public void addEllipse(double x, double y, double width, double height) {
        Ellipse2D.Double e = new Ellipse2D.Double(x, y, width, height);
        extend(e);
    }

    public void addLine(double x1, double y1, double x2, double y2) {
        moveto(x1, y1);
        lineto(x2, y2);
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
            } else if (cmd == PathIterator.SEG_CUBICTO) {
                curveto(points[2], points[3], points[4], points[5], points[0], points[1]);
            } else if (cmd == PathIterator.SEG_CLOSE) {
                close();
            } else {
                throw new AssertionError("Unknown path command " + cmd);
            }
            pi.next();
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
                    gp.curveTo((float) el.getX(), (float) el.getY(),
                            (float) c1.getX(), (float) c1.getY(),
                            (float) c2.getX(), (float) c2.getY());
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
        return new Rect(getGeneralPath().getBounds2D());
    }

    public void draw(Graphics2D g) {
        if (fillColor != null && fillColor.isVisible()) {
            g.setColor(fillColor.getAwtColor());
            g.fill(getGeneralPath());
        }
        if (strokeWidth > 0 && strokeColor != null && strokeColor.isVisible()) {
            g.setColor(strokeColor.getAwtColor());
            g.setStroke(new BasicStroke((float) strokeWidth));
            g.draw(getGeneralPath());
        }
    }

    @Override
    public BezierPath clone() {
        return new BezierPath(this);
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
