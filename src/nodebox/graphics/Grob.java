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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public interface Grob extends Cloneable {

    //// Operations on the current context. ////

    public void inheritFromContext(GraphicsContext ctx);

    public void draw(Graphics2D g);

    //// Geometric queries ////

    public Rect getBounds();

    //// Transformations ////

    public void transform(Transform t);

    //// Cloning ////

    public Grob clone();

    void translate(float tx, float ty);

    void rotate(float degrees);

    void rotateRadians(float radians);

    void scale(float scale);

    void scale(float sx, float sy);

    void skew(float skew);

    void skew(float kx, float ky);
}
