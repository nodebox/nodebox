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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Canvas implements Grob {

    public static final double DEFAULT_WIDTH = 1000;
    public static final double DEFAULT_HEIGHT = 1000;
    private double width,  height;
    private ArrayList<Grob> elements;

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(double width, double height) {
        this.width = width;
        this.height = height;
    }
    //// Container operations ////
    public void append(Grob g) {
        elements.add(g);
    }

    public int size() {
        return elements.size();
    }

    public void clear() {
        elements.clear();
    }

    public List<Grob> elements() {
        return new ArrayList<Grob>(elements);
    }
    //// Geometry ////
    public Rect bounds() {
        if (elements.isEmpty()) {
            return new Rect();
        // TODO: We're running bounds() twice on the first element
        }
        Rect r = elements.get(0).bounds();
        for (Grob g : elements) {
            r = r.united(g.bounds());
        }
        return r;
    }

    @Override
    public Grob clone() {
        Canvas c = new Canvas(width, height);
        c.elements = (ArrayList<Grob>) elements.clone();
        return c;
    }
    //// Drawing ////
    public void draw(Context ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void save(File file) {
        throw new UnsupportedOperationException("Not supported yet.");

    }
}
