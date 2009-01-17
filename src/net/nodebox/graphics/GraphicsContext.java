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

import java.util.ArrayList;
import java.util.List;

public class GraphicsContext {

    private Canvas canvas;
    // TODO: Support output mode
    // TODO: Support color mode
    private Color fillColor;
    private Color strokeColor;
    private double strokeWidth;
    private BezierPath path;
    private boolean autoClosePath;
    private boolean pathClosed;
    private Transform transform = new Transform();
    private ArrayList<Transform> transformStack;
    private String fontName;
    private double fontSize;
    private double lineHeight;
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
        strokeWidth = 1.0;
        if (resetBackground)
            canvas.setBackground(new Color(1, 1, 1));
        path = null;
        transform = new Transform();
        transformStack = new ArrayList<Transform>();
        fontName = "Helvetica";
        fontSize = 24;
        lineHeight = 1.2;
        align = Text.Align.CENTER;
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

    public Color getBackground() {
        return canvas.getBackground();
    }

    public void setBackground(Color background) {
        canvas.setBackground(background);
    }

    //// Attribute access ////

    public Canvas getCanvas() {
        return canvas;
    }

    public Transform getTransform() {
        return transform;
    }

    //// Primitives ////

    public BezierPath rect(Rect r) {
        BezierPath p = new BezierPath();
        p.rect(r);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath rect(double x, double y, double width, double height) {
        BezierPath p = new BezierPath();
        p.rect(x, y, width, height);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath rect(Rect r, double roundness) {
        BezierPath p = new BezierPath();
        p.rect(r, roundness);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath rect(double x, double y, double width, double height, double roundness) {
        BezierPath p = new BezierPath();
        p.rect(x, y, width, height, roundness);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath rect(double x, double y, double width, double height, double rx, double ry) {
        BezierPath p = new BezierPath();
        p.rect(x, y, width, height, rx, ry);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath oval(double x, double y, double width, double height) {
        // TODO: Deprecation warning
        return ellipse(x, y, width, height);
    }

    public BezierPath ellipse(double x, double y, double width, double height) {
        BezierPath p = new BezierPath();
        p.ellipse(x, y, width, height);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    public BezierPath line(double x1, double y1, double x2, double y2) {
        BezierPath p = new BezierPath();
        p.line(x1, y1, x2, y2);
        inheritFromContext(p);
        canvas.add(p);
        return p;
    }

    //// Path commands ////

    public void beginPath() {
        path = new BezierPath();
        pathClosed = false;
    }

    public void beginPath(double x, double y) {
        beginPath();
        moveto(x, y);
    }

    public void moveto(double x, double y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        path.moveto(x, y);
    }

    public void lineto(double x, double y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        path.lineto(x, y);
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        path.curveto(x1, y1, x2, y2, x3, y3);
    }

    public void closePath() {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        if (!pathClosed) {
            path.close();
            pathClosed = true;
        }
    }

    public BezierPath endPath() {
        return endPath(true);
    }

    public BezierPath endPath(boolean draw) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginPath() first.");
        if (autoClosePath)
            closePath();
        BezierPath p = path;
        inheritFromContext(p);
        if (draw)
            canvas.add(p);
        // Initialize a new path
        path = null;
        pathClosed = false;
        return p;
    }

    public void drawPath(BezierPath path) {
        inheritFromContext(path);
        canvas.add(path);
    }

    public boolean isAutoClosePath() {
        return autoClosePath;
    }

    public void setAutoClosePath(boolean autoClosePath) {
        this.autoClosePath = autoClosePath;
    }

    public BezierPath findPath(List<Point> points) {
        return findPath(points, 1);
    }

    public BezierPath findPath(List<Point> points, double curvature) {
        throw new RuntimeException("Not implemented yet");
    }

    //// Clipping ////

    // TODO: implement clipping

    //// Transformation commands ////

    public void push() {
        transformStack.add(0, transform.clone());
    }

    public void pop() {
        if (transformStack.size() == 0)
            throw new NodeBoxError("Pop: too many pops!");
        transform = transformStack.get(0);
        transformStack.remove(0);
    }

    public void reset() {
        transform = new Transform();
    }

    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    public void rotate(double r) {
        transform.rotate(r);
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

    //// Color commands ////

    public Color getFill() {
        return fillColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFill(Color fillColor) {
        setFillColor(fillColor);
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : fillColor.clone();
    }

    public Color getStroke() {
        return strokeColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStroke(Color strokeColor) {
        setStrokeColor(strokeColor);
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor == null ? null : strokeColor.clone();
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    //// Font commands ////

    public void setFont(String fontName, double fontSize) {
        setFontName(fontName);
        setFontSize(fontSize);
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        if (!Text.fontExists(fontName))
            throw new NodeBoxError("Font '" + fontName + "' does not exist.");
        this.fontName = fontName;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(double lineHeight) {
        this.lineHeight = lineHeight;
    }

    public Text.Align getAlign() {
        return align;
    }

    public void setAlign(Text.Align align) {
        this.align = align;
    }

    public Text text(String text, double x, double y) {
        return text(text, x, y, 0, 0);
    }

    public Text text(String text, double x, double y, double width) {
        return text(text, x, y, width, 0);
    }

    public Text text(String text, double x, double y, double width, double height) {
        Text t = new Text(text, x, y, width, height);
        inheritFromContext(t);
        canvas.add(t);
        return t;
    }

    public BezierPath textPath(String text, double x, double y) {
        return textPath(text, x, y, 0, 0);
    }

    public BezierPath textPath(String text, double x, double y, double width) {
        return textPath(text, x, y, width, 0);
    }

    public BezierPath textPath(String text, double x, double y, double width, double height) {
        BezierPath p = new BezierPath();
        p.text(text, fontName, fontSize, lineHeight, align, x, y, width, height);
        inheritFromContext(p);
        return p;
    }

    public Rect textMetrics(String text) {
        return textMetrics(text, 0, 0);
    }

    public Rect textMetrics(String text, double width) {
        return textMetrics(text, width, 0);
    }

    public Rect textMetrics(String text, double width, double height) {
        Text t = new Text(text, 0, 0, width, height);
        inheritFromContext(t);
        return t.getMetrics();
    }

    //// Image methods ////

    public Image image(String path, double x, double y) {
        Image img = new Image(path);
        img.setX(x);
        img.setY(y);
        inheritFromContext(img);
        canvas.add(img);
        return img;
    }

    public Size imageSize(String path) {
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

    //// Context inheritance ////

    private void inheritFromContext(BezierPath p) {
        p.setTransform(transform.clone());
        p.setFillColor(fillColor == null ? null : fillColor.clone());
        p.setStrokeColor(strokeColor == null ? null : strokeColor.clone());
        p.setStrokeWidth(strokeWidth);
    }

    private void inheritFromContext(Text t) {
        t.setTransform(transform.clone());
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
