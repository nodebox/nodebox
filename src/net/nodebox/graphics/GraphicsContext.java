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

public class GraphicsContext {

    private Canvas canvas;
    private Transform transform;

    public GraphicsContext() {
        canvas = new Canvas();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public GraphicsContext(Canvas canvas) {
        this.canvas = canvas;
    }

    public Transform getTransform() {
        return transform;
    }

    public BezierPath rect(double x, double y, double width, double height) {
        BezierPath p = new BezierPath();
        p.addRect(x, y, width, height);
        // TODO: Inherit from context
        canvas.add(p);
        return p;
    }


}
