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

import java.util.ArrayList;
import java.util.List;

public class GraphicsContext {

    private Canvas canvas;
    // TODO: Support output mode
    // TODO: Support color mode
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    private Path path;
    private boolean autoClosePath;
    private boolean pathClosed;
    private Transform transform = new Transform();
    private ArrayList<Transform> transformStack;
    private String fontName;
    private float fontSize;
    private float lineHeight;
    private Text.Align align;

    //// Initialization ////

    public GraphicsContext() {
        canvas = new Canvas();
        resetContext(true);
    }

    public GraphicsContext(Canvas canvas) {
        this.canvas = canvas;
        resetContext(false);
    }

    public void resetContext() {
        resetContext(true);
    }

    public void resetContext(boolean resetBackground) {
        fillColor = new Color();
        strokeColor = null;
        strokeWidth = 1f;
        if (resetBackground)
            canvas.setBackground(new Color(1, 1, 1));
        path = null;
        transform = new Transform();
        transformStack = new ArrayList<Transform>();
        fontName = "Helvetica";
        fontSize = 24;
        lineHeight = 1.2f;
        align = Text.Align.CENTER;
    }

    //// Setup methods ////

    public void size(float width, float height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    public float getWidth() {
        return canvas.getWidth();
    }

    public float getHeight() {
        return canvas.getHeight();
    }

    public float getWIDTH() {
        return canvas.getWidth();
    }

    public float getHEIGHT() {
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
    public Color background(float x) {
        return canvas.setBackground(new Color(x, x, x));
    }

    /**
     * Set the current background color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current background color.
     */
    public Color background(float x, float y) {
        return canvas.setBackground(new Color(x, x, x, y));
    }

    /**
     * Set the current background color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current background color.
     */
    public Color background(float x, float y, float z) {
        return canvas.setBackground(new Color(x, y, z));
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
    public Color background(float x, float y, float z, float a) {
        return canvas.setBackground(new Color(x, y, z, a));
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

    public Transform getTransform() {
        return transform;
    }

    //// Primitives ////

    public Path rect(Rect r) {
        Path p = new Path();
        p.rect(r);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path rect(float x, float y, float width, float height) {
        Path p = new Path();
        p.rect(x, y, width, height);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path rect(Rect r, float roundness) {
        Path p = new Path();
        p.rect(r, roundness);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path rect(float x, float y, float width, float height, float roundness) {
        Path p = new Path();
        p.rect(x, y, width, height, roundness);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path rect(float x, float y, float width, float height, float rx, float ry) {
        Path p = new Path();
        p.rect(x, y, width, height, rx, ry);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path oval(float x, float y, float width, float height) {
        // TODO: Deprecation warning
        return ellipse(x, y, width, height);
    }

    public Path ellipse(float x, float y, float width, float height) {
        Path p = new Path();
        p.ellipse(x, y, width, height);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public Path line(float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.line(x1, y1, x2, y2);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    //// Path commands ////

    public void beginpath() {
        path = new Path();
        pathClosed = false;
    }

    public void beginpath(float x, float y) {
        beginpath();
        moveto(x, y);
    }

    public void moveto(float x, float y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        path.moveto(x, y);
    }

    public void lineto(float x, float y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        path.lineto(x, y);
    }

    public void curveto(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        path.curveto(x1, y1, x2, y2, x3, y3);
    }

    public void closepath() {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        if (!pathClosed) {
            path.close();
            pathClosed = true;
        }
    }

    public Path endpath() {
        return endpath(true);
    }

    public Path endpath(boolean draw) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        if (autoClosePath)
            closepath();
        Path p = path;
        inheritFromContext(p);
        if (draw)
            canvas.add(p);
        // Initialize a new path
        path = null;
        pathClosed = false;
        return p;
    }

    public void drawpath(Path path) {
        inheritFromContext(path);
        canvas.add(path);
    }

    public boolean autoclosepath() {
        return autoClosePath;
    }

    public boolean autoclosepath(boolean c) {
        return autoClosePath = c;
    }

    public Path findpath(List<Point> points) {
        return findpath(points, 1);
    }

    public Path findpath(List<Point> points, float curvature) {
        throw new RuntimeException("findpath is not implemented yet.");
    }

    //// Clipping ////

    // TODO: implement clipping

    public void beginclip(Path p) {
        throw new RuntimeException("beginclip is not implemented yet.");
    }

    public void endclip() {
        throw new RuntimeException("endclip is not implemented yet.");
    }

    //// Transformation commands ////

    public void push() {
        transformStack.add(0, transform.clone());
    }

    public void pop() {
        if (transformStack.isEmpty())
            throw new NodeBoxError("Pop: too many pops!");
        transform = transformStack.get(0);
        transformStack.remove(0);
    }

    public void reset() {
        transformStack.clear();
        transform = new Transform();
    }

    public void translate(float tx, float ty) {
        transform.translate(tx, ty);
    }

    public void rotate(float r) {
        transform.rotate(r);
    }

    public void scale(float scale) {
        transform.scale(scale);
    }

    public void scale(float sx, float sy) {
        transform.scale(sx, sy);
    }

    public void skew(float skew) {
        transform.skew(skew);
    }

    public void skew(float kx, float ky) {
        transform.skew(kx, ky);
    }

    //// Color commands ////

    public String outputmode() {
        throw new RuntimeException("outputmode is not implemented yet.");
    }

    public String outputmode(String mode) {
        throw new RuntimeException("outputmode is not implemented yet.");
    }

    public String colormode() {
        throw new RuntimeException("colormode is not implemented yet.");
    }

    public String colormode(String mode) {
        throw new RuntimeException("colormode is not implemented yet.");
    }

    /**
     * Create an empty (black) color object.
     *
     * @return the new color.
     */
    public Color color() {
        return new Color();
    }

    /**
     * Create a new color with the given grayscale value.
     *
     * @param x the gray component.
     * @return the new color.
     */
    public Color color(float x) {
        return new Color(x, x, x);
    }

    /**
     * Create a new color with the given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the new color.
     */
    public Color color(float x, float y) {
        return new Color(x, x, x, y);
    }

    /**
     * Create a new color with the the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the new color.
     */
    public Color color(float x, float y, float z) {
        return new Color(x, y, z);
    }

    /**
     * Create a new color with the the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the new color.
     */
    public Color color(float x, float y, float z, float a) {
        return new Color(x, y, z, a);
    }

    /**
     * Create a new color with the the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the new color is turned off (same as nocolor).
     *
     * @param c the color object.
     * @return the new color.
     */
    public Color color(Color c) {
        return c == null ? new Color(0, 0, 0, 0) : c.clone();
    }

    /**
     * Get the current fill color.
     *
     * @return the current fill color.
     */
    public Color fill() {
        return fillColor;
    }

    /**
     * Set the current fill color to given grayscale value.
     *
     * @param x the gray component.
     * @return the current fill color.
     */
    public Color fill(float x) {
        return fillColor = new Color(x, x, x);
    }

    /**
     * Set the current fill color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current fill color.
     */
    public Color fill(float x, float y) {
        return fillColor = new Color(x, x, x, y);
    }

    /**
     * Set the current fill color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current fill color.
     */
    public Color fill(float x, float y, float z) {
        return fillColor = new Color(x, y, z);
    }

    /**
     * Set the current fill color to the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the current fill color.
     */
    public Color fill(float x, float y, float z, float a) {
        return fillColor = new Color(x, y, z, a);
    }

    /**
     * Set the current fill color to the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the current fill color is turned off (same as nofill).
     *
     * @param c the color object.
     * @return the current fill color.
     */
    public Color fill(Color c) {
        return fillColor = c == null ? null : c.clone();
    }

    public void nofill() {
        fillColor = null;
    }

    /**
     * Get the current stroke color.
     *
     * @return the current stroke color.
     */
    public Color stroke() {
        return strokeColor;
    }

    /**
     * Set the current stroke color to given grayscale value.
     *
     * @param x the gray component.
     * @return the current stroke color.
     */
    public Color stroke(float x) {
        return strokeColor = new Color(x, x, x);
    }

    /**
     * Set the current stroke color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current stroke color.
     */
    public Color stroke(float x, float y) {
        return strokeColor = new Color(x, x, x, y);
    }

    /**
     * Set the current stroke color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current stroke color.
     */
    public Color stroke(float x, float y, float z) {
        return strokeColor = new Color(x, y, z);
    }

    /**
     * Set the current stroke color to the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the current stroke color.
     */
    public Color stroke(float x, float y, float z, float a) {
        return strokeColor = new Color(x, y, z, a);
    }

    /**
     * Set the current stroke color to the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the current stroke color is turned off (same as nostroke).
     *
     * @param c the color object.
     * @return the current stroke color.
     */
    public Color stroke(Color c) {
        return strokeColor = c == null ? null : c.clone();
    }

    public void nostroke() {
        strokeColor = null;
    }

    public float strokewidth() {
        return strokeWidth;
    }

    public float strokewidth(float w) {
        return strokeWidth = w;
    }

    //// Font commands ////

    public String font() {
        return fontName;
    }

    public String font(String fontName) {
        if (!Text.fontExists(fontName))
            throw new NodeBoxError("Font '" + fontName + "' does not exist.");
        return this.fontName = fontName;
    }

    public String font(String fontName, float fontSize) {
        font(fontName);
        fontsize(fontSize);
        return fontName;
    }

    public float fontsize() {
        return fontSize;
    }

    public float fontsize(float s) {
        return fontSize = s;
    }

    public float lineheight() {
        return lineHeight;
    }

    public float lineheight(float lineHeight) {
        return this.lineHeight = lineHeight;
    }

    public Text.Align align() {
        return align;
    }

    public void align(Text.Align align) {
        this.align = align;
    }

    public Text text(String text, float x, float y) {
        return text(text, x, y, 0, 0);
    }

    public Text text(String text, float x, float y, float width) {
        return text(text, x, y, width, 0);
    }

    public Text text(String text, float x, float y, float width, float height) {
        Text t = new Text(text, x, y, width, height);
        inheritFromContext(t);
        canvas.add(t);
        return t;
    }

    public Path textpath(String text, float x, float y) {
        return textpath(text, x, y, 0, 0);
    }

    public Path textpath(String text, float x, float y, float width) {
        return textpath(text, x, y, width, 0);
    }

    public Path textpath(String text, float x, float y, float width, float height) {
        Text t = new Text(text, x, y, width, height);
        inheritFromContext(t);
        Path p = new Path();
        p.text(t);
        inheritFromContext(p);
        return p;
    }

    public Rect textmetrics(String text) {
        return textmetrics(text, 0, 0);
    }

    public Rect textmetrics(String text, float width) {
        return textmetrics(text, width, 0);
    }

    public Rect textmetrics(String text, float width, float height) {
        Text t = new Text(text, 0, 0, width, height);
        inheritFromContext(t);
        return t.getMetrics();
    }

    public float textwidth(String text) {
        return textmetrics(text, 0, 0).getWidth();
    }

    public float textwidth(String text, float width) {
        return textmetrics(text, width).getWidth();
    }

    public float textheight(String text) {
        return textmetrics(text, 0, 0).getHeight();
    }

    public float textheight(String text, float width) {
        return textmetrics(text, width).getHeight();
    }

    //// Image methods ////

    public Image image(String path, float x, float y) {
        Image img = new Image(path);
        img.setX(x);
        img.setY(y);
        inheritFromContext(img);
        canvas.add(img);
        return img;
    }

    public Size imagesize(String path) {
        Image img = new Image(path);
        return img.getSize();
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

    //// Utility methods ////

    public void var(Object... args) {
        throw new RuntimeException("var is no longer supported. Create parameters on the node instead.");
    }

    public double random() {
        return Math.random();
    }

    public long random(int max) {
        return Math.round(Math.random() * max);
    }

    public long random(int min, int max) {
        return Math.round(min + (Math.random() * (max - min)));
    }

    public double random(double max) {
        return Math.random() * max;
    }

    public double random(double min, double max) {
        return min + (Math.random() * (max - min));
    }

    public Object choice(List objects) {
        if (objects == null || objects.isEmpty()) return null;
        return objects.get((int) random(objects.size()));
    }

    //// Context inheritance ////

    private void inheritFromContext(Path p) {
        p.setFillColor(fillColor == null ? null : fillColor.clone());
        p.setStrokeColor(strokeColor == null ? null : strokeColor.clone());
        p.setStrokeWidth(strokeWidth);
    }

    private void inheritFromContext(Text t) {
        t.setFillColor(fillColor == null ? null : fillColor.clone());
        t.setFontName(fontName);
        t.setFontSize(fontSize);
        t.setLineHeight(lineHeight);
        t.setAlign(align);
    }

    private void inheritFromContext(Image i) {
        i.setTransform(transform.clone());
    }

}
