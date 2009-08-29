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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

public class Canvas extends AbstractTransformable {

    public static final float DEFAULT_WIDTH = 1000;
    public static final float DEFAULT_HEIGHT = 1000;

    private Color background = new Color(1, 1, 1);
    private float width, height;
    private ArrayList<Grob> items = new ArrayList<Grob>();

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(float width, float height) {
        setSize(width, height);
    }

    public Canvas(Canvas other) {
        this.width = other.width;
        this.height = other.height;
        this.background = other.background == null ? null : other.background.clone();
        for (Grob g : other.items) {
            add(g.clone());
        }
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    //// Container operations ////

    public void add(Grob g) {
        items.add(g);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public java.util.List<Grob> getItems() {
        return items;
    }

    public Grob get(int index) {
        try {
            return items.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Create copies of all grobs of the given group and append them to myself.
     *
     * @param c the canvas whose elements are appended.
     */
    public void extend(Canvas c) {
        for (Grob grob : c.getItems()) {
            add(grob.clone());
        }
    }

    public void transform(Transform t) {
        for (Grob g : items) {
            g.transform(t);
        }
    }

    //// Geometry ////

    /**
     * Returns the bounding box of all elements in the group.
     *
     * @return a bounding box that contains all elements in the group.
     */
    public Rect getBounds() {
        if (items.isEmpty()) return new Rect();
        Rect r = null;
        for (Grob g : items) {
            if (r == null) {
                r = g.getBounds();
            } else {
                r = r.united(g.getBounds());
            }
        }
        return r;
    }

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

    public void inheritFromContext(GraphicsContext ctx) {
        // TODO: Implement
    }

    public void draw(Graphics2D g) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float left = -halfWidth;
        float top = -halfHeight;
        g.setColor(background.getAwtColor());
        g.fillRect((int) left, (int) top, (int) width, (int) height);
        //Rectangle clip = g.getClipBounds();
        //int clipwidth = clip != null && width > clip.width ? clip.width : (int) height;
        //int clipheight = clip != null && height > clip.height ? clip.height : (int) width;
        //g.setClip(clip != null ? clip.x : 0, clip != null ? clip.y : 0, clipwidth, clipheight);
        for (Grob grob : items) {
            grob.draw(g);
        }
    }

    public void save(File file) {
        if (file.getName().endsWith(".pdf")) {
            PDFRenderer.render(this, file);
        } else {
            throw new UnsupportedOperationException("Unsupported file extension " + file);
        }
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + width + ", " + height + ">";
    }
}
