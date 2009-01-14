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
import java.io.File;

public class Canvas extends Group {

    public static final double DEFAULT_WIDTH = 1000;
    public static final double DEFAULT_HEIGHT = 1000;

    private Color background = new Color(1, 1, 1);
    private double width, height;

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(double width, double height) {
        setSize(width, height);
    }

    public Canvas(Canvas other) {
        super(other);
        this.width = other.width;
        this.height = other.height;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Canvas clone() {
        return new Canvas(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Canvas)) return false;
        Canvas other = (Canvas) obj;
        return width == other.width
                && height == other.height
                && background.equals(other.background)
                && super.equals(other);
    }

    //// Drawing ////

    @Override
    public void draw(Graphics2D g) {
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double left = -halfWidth;
        double right = halfWidth;
        double top = -halfHeight;
        double bottom = halfHeight;
        g.setColor(background.getAwtColor());
        g.fillRect((int) left, (int) top, (int) width, (int) height);
        Rectangle clip = g.getClipBounds();
        int clipwidth = width > clip.width ? clip.width : (int) height;
        int clipheight = height > clip.height ? clip.height : (int) width;
        g.setClip(clip.x, clip.y, clipwidth, clipheight);
        super.draw(g);
    }

    public void save(File file) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + width + ", " + height + ">";
    }
}
