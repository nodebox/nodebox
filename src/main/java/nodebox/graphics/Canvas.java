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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class Canvas extends AbstractTransformable {

    public static final double DEFAULT_WIDTH = 1000;
    public static final double DEFAULT_HEIGHT = 1000;

    private Color background = new Color(1, 1, 1);
    private double offsetX, offsetY;
    private double width, height;
    private ArrayList<Grob> items = new ArrayList<Grob>();

    public Canvas() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Canvas(double width, double height) {
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

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
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

    public BufferedImage asImage() {
        Rect bounds = getBounds();
        BufferedImage img = new BufferedImage((int) Math.round(bounds.getWidth()), (int) Math.round(bounds.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(-bounds.getX(), -bounds.getY());
        draw(g);
        img.flush();
        return img;
    }

    public void save(File file) {
        if (file.getName().endsWith(".pdf")) {
            PDFRenderer.render(this, getBounds(), file);
        } else {
            try {
                ImageIO.write(asImage(), getFileExtension(file), file);
            } catch (IOException e) {
                throw new RuntimeException("Could not write image file " + file, e);
            }
        }
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        String ext = null;
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1).toLowerCase(Locale.US);
        }
        return ext;
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + width + ", " + height + ">";
    }
}
