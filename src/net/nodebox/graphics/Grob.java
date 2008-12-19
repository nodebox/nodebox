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

public abstract class Grob implements Cloneable {

    private Transform transform = new Transform();

    protected Grob() {
    }

    protected Grob(Grob other) {
        transform = other.transform.clone();
    }

    public void inheritFromContext(GraphicsContext ctx) {
        this.transform = ctx.getTransform().clone();
    }

    public abstract void draw(Graphics2D g);

    //// Geometric queries ////

    public abstract Rect getBounds();

    public Rect getFrame() {
        return transform.convertBoundsToFrame(getBounds());
    }

    //// Transformations ////

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public void appendTransform(Transform transform) {
        this.transform.append(transform);
    }

    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    public void rotate(double degrees) {
        transform.rotate(degrees);
    }

    public void scale(double scale) {
        transform.scale(scale);
    }

    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }

    public void skew(double skew) {
        transform.skew(skew);
    }

    public void skew(double kx, double ky) {
        transform.skew(kx, ky);
    }

    //// Cloning ////

    public abstract Grob clone();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Grob)) return false;
        Grob other = (Grob) obj;
        return transform.equals(other.transform);
    }
}
