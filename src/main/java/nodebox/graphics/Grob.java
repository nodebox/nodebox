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

public interface Grob extends Cloneable, Drawable {

    //// Geometric queries ////

    public boolean isEmpty();

    public Rect getBounds();

    //// Transformations ////

    public void transform(Transform t);

    //// Cloning ////

    public Grob clone();

    void translate(double tx, double ty);

    void rotate(double degrees);

    void rotateRadians(double radians);

    void scale(double scale);

    void scale(double sx, double sy);

    void skew(double skew);

    void skew(double kx, double ky);

    //// Transform Delegate ////

    void setTransformDelegate(TransformDelegate d);

    TransformDelegate getTransformDelegate();

}
