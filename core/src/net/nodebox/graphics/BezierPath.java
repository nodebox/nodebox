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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederik
 */
public class BezierPath implements Grob {
    // Magic number used for drawing bezier circles.
    // 4 *(sqrt(2) -1)/3
    private static final float KAPPA = 0.5522847498f;
    private ArrayList<PathElement> elements = new ArrayList<PathElement>();
    private boolean filled = true;
    private Color fillColor = new Color();
    private boolean stroked = false;
    private Color strokeColor = new Color();
    private double strokeWidth = 1;
    private boolean dirty = true;
    private transient java.awt.geom.GeneralPath awtPath;

    public BezierPath() {
    }

    //// Path methods ////
    public void moveto(double x, double y) {
        elements.add(new PathElement(PathElement.COMMAND_MOVETO, x, y));
        dirty = true;
    }

    public void lineto(double x, double y) {
        elements.add(new PathElement(PathElement.COMMAND_LINETO, x, y));
        dirty = true;
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
        elements.add(new PathElement(PathElement.COMMAND_CURVETO, x1, y1, x2, y2, x3, y3));
        dirty = true;
    }

    public void close() {
        elements.add(new PathElement(PathElement.COMMAND_CLOSE));
        dirty = true;
    }

    //// Basic shapes ////
    public void rect(double x, double y, double width, double height) {
        moveto(x, y);
        lineto(x + width, y);
        lineto(x + width, y + height);
        lineto(x, y + height);
        close();
    }

    public void roundedRect(double x, double y, double width, double height, double roundness) {
        double cv = width < height ? width * roundness : height * roundness;
        moveto(x, y + cv);
        curveto(x, y, x, y, x + cv, y);
        lineto(x + width - cv, y);
        curveto(x + width, y + height, x + width, y + height, x + width - cv, y + height);
        lineto(x + cv, y + height);
        curveto(x, y + height, x, y + height, x, y + height - cv);
        close();
    }

    public void oval(double x, double y, double width, double height) {
        double hdiff = width / 2 * KAPPA;
        double vdiff = height / 2 * KAPPA;
        moveto(x + width / 2, y + height);
        curveto(x + width / 2 - hdiff, y + height,
                x, y + height / 2 + vdiff,
                x, y + height / 2);
        curveto(x, y + height / 2 - vdiff,
                x + width / 2 - hdiff, y,
                x + width / 2, y);
        curveto(x + width / 2 + hdiff, y,
                x + width, y + height / 2 - vdiff,
                x + width, y + height / 2);
        curveto(x + width, y + height / 2 + vdiff,
                x + width / 2 + hdiff, y + height,
                x + width / 2, y + height);
    }

    public void line(double x1, double y1, double x2, double y2) {
        moveto(x1, y1);
        lineto(x2, y2);
    }

    //// List operations ////
    public List<PathElement> getElements() {
        return new ArrayList<PathElement>(elements);
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

    //// Geometry ////
    public java.awt.geom.GeneralPath awtPath() {
        if (!dirty) {
            return awtPath;
        }
        java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
        Point c1, c2;
        for (PathElement el : elements) {
            switch (el.getCommand()) {
                case PathElement.COMMAND_MOVETO:
                    gp.moveTo(el.getX(), el.getY());
                    break;
                case PathElement.COMMAND_LINETO:
                    gp.lineTo(el.getX(), el.getY());
                    break;
                case PathElement.COMMAND_CURVETO:
                    c1 = el.getControl1();
                    c2 = el.getControl2();
                    gp.curveTo(el.getX(), el.getY(), c1.getX(), c1.getY(), c2.getX(), c2.getY());
                    break;
                case PathElement.COMMAND_CLOSE:
                    gp.closePath();
                    break;
                default:
                    assert (false);
            }
        }
        awtPath = gp;
        dirty = false;
        return gp;

    }

    public Rect bounds() {
        return new Rect(awtPath().getBounds2D());
    }

    public void draw(Context ctx) {
    }

    @Override
    public Grob clone() {
        BezierPath p = new BezierPath();
        p.elements = (ArrayList<PathElement>) elements.clone();
        p.filled = filled;
        p.fillColor = fillColor.clone();
        p.stroked = stroked;
        p.strokeColor = strokeColor.clone();
        p.strokeWidth = strokeWidth;
        return p;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BezierPath)) return false;
        BezierPath p = (BezierPath) o;
        // TODO: equality should incorporate color as well.
        return elements.equals(p.elements);
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
