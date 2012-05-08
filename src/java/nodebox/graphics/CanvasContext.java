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

import java.awt.image.BufferedImage;

public class CanvasContext extends AbstractGraphicsContext {

    public static final double DEFAULT_WIDTH = 1000;
    public static final double DEFAULT_HEIGHT = 1000;

    public enum ImageMode {
        CORNERS, CORNER, RADIUS, CENTER
    }

    protected ImageMode imageMode = ImageMode.CORNER;

    private Canvas canvas;

    //// Initialization ////

    public CanvasContext() {
        canvas = new Canvas(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        resetContext(true);
    }

    public CanvasContext(Canvas canvas) {
        this.canvas = canvas;
        resetContext(false);
    }

    @Override
    public void resetContext() {
        resetContext(true);
    }

    public void resetContext(boolean resetBackground) {
        super.resetContext();
        if (resetBackground)
            canvas.setBackground(new Color(1, 1, 1));
    }

    public ImageMode imagemode() {
        return imageMode;
    }

    public ImageMode imagemode(ImageMode m) {
        return imageMode = m;
    }

    //// Setup methods ////

    public void size(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    public double getWidth() {
        return canvas.getWidth();
    }

    public double getHeight() {
        return canvas.getHeight();
    }

    public double getWIDTH() {
        return canvas.getWidth();
    }

    public double getHEIGHT() {
        return canvas.getHeight();
    }

    /**
     * Get the current background color.
     *
     * @return the current background color.
     */
    public Color background() {
        return canvas.getBackground();
    }

    /**
     * Set the current background color to given grayscale value.
     *
     * @param x the gray component.
     * @return the current background color.
     */
    public Color background(double x) {
        double nx = normalize(x);
        return canvas.setBackground(new Color(nx, nx, nx));
    }

    /**
     * Set the current background color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current background color.
     */
    public Color background(double x, double y) {
        double nx = normalize(x);
        return canvas.setBackground(new Color(nx, nx, nx, normalize(y)));
    }

    /**
     * Set the current background color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current background color.
     */
    public Color background(double x, double y, double z) {
        return canvas.setBackground(new Color(normalize(x), normalize(y), normalize(z), colormode()));
    }

    /**
     * Set the current background color to the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the current background color.
     */
    public Color background(double x, double y, double z, double a) {
        return canvas.setBackground(new Color(normalize(x), normalize(y), normalize(z), normalize(a), colormode()));
    }

    /**
     * Set the current background color to the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the current background color is turned off (same as nobackground).
     *
     * @param c the color object.
     * @return the current background color.
     */
    public Color background(Color c) {
        return canvas.setBackground(c == null ? null : c.clone());
    }

    public void nobackground() {
        canvas.setBackground(null);
    }

    //// Attribute access ////

    public Canvas getCanvas() {
        return canvas;
    }

    //// Image methods ////

    @Override
    public Image image(String path, double x, double y) {
        return image(path, x, y, 0, 0, 1, true);
    }

    @Override
    public Image image(String path, double x, double y, double width) {
        return image(path, x, y, width, 0, 1, true);
    }

    @Override
    public Image image(String path, double x, double y, double width, double height) {
        return image(path, x, y, width, height, 1, true);
    }

    @Override
    public Image image(String path, double x, double y, double width, double height, double alpha) {
        return image(path, x, y, width, height, alpha, true);
    }

    @Override
    public Image image(String path, double x, double y, double width, double height, boolean draw) {
        return image(path, x, y, width, height, 1.0, draw);
    }

    @Override
    public Image image(String path, double x, double y, double width, double height, double alpha, boolean draw) {
        return loadImage(new Image(path), x, y, width, height, alpha, draw);
    }

    @Override
    public Image image(Image img, double x, double y, double width, double height, double alpha, boolean draw) {
        return loadImage(img.clone(), x, y, width, height, alpha, draw);
    }

    @Override
    public Image image(BufferedImage img, double x, double y, double width, double height, double alpha, boolean draw) {
        return loadImage(new Image(img), x, y, width, height, alpha, draw);
    }

    private Image loadImage(Image img, double x, double y, double width, double height, double alpha, boolean draw) {
        if (width != 0) img.setWidth(width);
        if (height != 0) img.setHeight(height);
        switch (imageMode) {
            case CORNER:
                double w = img.getWidth();
                double h = img.getHeight();
                img.setX(x + w / 2);
                img.setY(y + h / 2);
                break;
            case CENTER:
                img.setX(x);
                img.setY(y);
        }
        // todo: differentiate between newly constructed objects and copies.
        img.setTransformDelegate(new ContextTransformDelegate(this));
        inheritFromContext(img);
        if (alpha != 1.0)
            img.setAlpha(alpha);
        if (draw)
            canvas.add(img);
        return img;
    }

    @Override
    public Size imagesize(String path) {
        Image img = new Image(path);
        return img.getSize();
    }

    @Override
    public Size imagesize(Image img) {
        return img.getSize();
    }

    @Override
    public Size imagesize(BufferedImage img) {
        return new Size(img.getWidth(), img.getHeight());
    }

    /// Drawing methods ////

    /**
     * The draw method doesn't actually draw anything, but rather appends grobs to the canvas.
     * When the canvas gets drawn, this grob will be drawn also.
     *
     * @param grob the grob to append to the canvas
     */
    public void draw(Grob grob) {
        canvas.add(grob);
    }

    @Override
    protected void addPath(Path p) {
        canvas.add(p);
    }

    @Override
    protected void addText(Text t) {
        canvas.add(t);
    }

    protected void addImage(Image i) {
        canvas.add(i);
    }

    //// Context inheritance ////

    protected void inheritFromContext(Image i) {
        TransformDelegate d = i.getTransformDelegate();
        d.transform(i, transform, true);
    }
}
