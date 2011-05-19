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
import java.util.ArrayList;

public class Canvas extends AbstractTransformable {

    public static final float DEFAULT_WIDTH = 1000;
    public static final float DEFAULT_HEIGHT = 1000;

    private Color background = new Color(1, 1, 1);
    private float offsetX, offsetY;
    private float width, height;
    private ArrayList<Grob> items = new ArrayList<Grob>();

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(float width, float height) {
        setSize(width, height);
    }

    public Canvas(Canvas other) {
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.width = other.width;
        this.height = other.height;
        this.background = other.background == null ? null : other.background.clone();
        for (Grob g : other.items) {
            add(g.clone());
        }
    }

    /**
     * Convert the current canvas into a geometry object.
     * Only objects of a geometric nature can be present in the output.
     *
     * @return a Geometry object
     */
    public Geometry asGeometry() {
        return asGeometry(true);
    }

    /**
     * Convert the current canvas into a geometry object.
     * Only objects of a geometric nature can be present in the output.
     *
     * @param clone if the items on the canvas need to be cloned.
     * @return a Geometry object
     */
    public Geometry asGeometry(boolean clone) {
        Geometry g = new Geometry();
        for (Grob item : items) {
            if (item instanceof Path)
                g.add((Path) (clone ? item.clone() : item));
            else if (item instanceof Text)
                g.add(((Text) item).getPath());
            else if (item instanceof Geometry)
                g.extend((Geometry) (clone ? item.clone() : item));
        }
        return g;
    }

    public Color getBackground() {
        return background;
    }

    public Color setBackground(Color background) {
        return this.background = background;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
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


    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the bounding box of the canvas.
     * <p/>
     * This does not compute the bounding boxes of the children, but always returns the requested canvas bounds.
     *
     * @return a bounding box with x/y at the center and width/height of the canvas.
     */
    public Rect getBounds() {
        return new Rect(-width / 2 + offsetX, -height / 2 + offsetY, width, height);
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
        if (background != null) {
            g.setColor(background.getAwtColor());
            g.fill(getBounds().getRectangle2D());
        }
        g.clip(getBounds().getRectangle2D());
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
