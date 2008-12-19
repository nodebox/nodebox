package net.nodebox.graphics;

import java.awt.*;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

public class Text extends Grob {

    public enum Align {
        LEFT, RIGHT, CENTER, JUSTIFY
    }

    private String text;
    private double baseLineX, baseLineY;
    private double width = -1;
    private double height = -1;
    private String fontName = "Helvetica";
    private double fontSize = 24;
    private double lineHeight;
    private Align align = Align.LEFT;
    private Color fillColor = new Color();

    public Text(String text, Point pt) {
        this.text = text;
        this.baseLineX = pt.getX();
        this.baseLineY = pt.getY();
    }

    public Text(String text, double baseLineX, double baseLineY) {
        this.text = text;
        this.baseLineX = baseLineX;
        this.baseLineY = baseLineY;
    }

    public Text(String text, Rect r) {
        this.text = text;
        this.baseLineX = r.getX();
        this.baseLineY = r.getY();
        this.width = r.getWidth();
        this.height = r.getHeight();
    }

    public Text(String text, double x, double y, double width, double height) {
        this.text = text;
        this.baseLineX = x;
        this.baseLineY = y;
        this.width = width;
        this.height = height;
    }

    public Text(Text other) {
        super(other);
        this.text = other.text;
        this.baseLineX = other.baseLineX;
        this.baseLineY = other.baseLineY;
        this.width = other.width;
        this.height = other.height;
        this.fontName = other.fontName;
        this.fontSize = other.fontSize;
        this.lineHeight = other.lineHeight;
        this.align = other.align;
        fillColor = other.fillColor == null ? null : other.fillColor.clone();
    }

    //// Getters/setters /////

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getBaseLineX() {
        return baseLineX;
    }

    public void setBaseLineX(double baseLineX) {
        this.baseLineX = baseLineX;
    }

    public double getBaseLineY() {
        return baseLineY;
    }

    public void setBaseLineY(double baseLineY) {
        this.baseLineY = baseLineY;
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

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public Font getFont() {
        return new Font(fontName, Font.PLAIN, (int) fontSize);
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(double lineHeight) {
        this.lineHeight = lineHeight;
    }

    public Align getAlign() {
        return align;
    }

    public void setAlign(Align align) {
        this.align = align;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void draw(Graphics2D g) {
        if (text == null || text.length() == 0) return;
        g.setColor(fillColor.getAwtColor());
        double x = baseLineX;
        double y = baseLineY;
        AttributedString attrString = new AttributedString(text);
        attrString.addAttribute(TextAttribute.FONT, getFont());
        AttributedCharacterIterator strIterator = attrString.getIterator();
        LineBreakMeasurer measurer = new LineBreakMeasurer(strIterator, g.getFontRenderContext());
        TextLayout layout;
        while (measurer.getPosition() < text.length() && (height == -1 || y < height)) {
            layout = measurer.nextLayout((float) (width == -1 ? 10000000 : width));
            double dx = 0;
            if (align == Align.RIGHT) {
                dx = width - layout.getAdvance();
            } else if (align == Align.CENTER) {
                dx = (width - layout.getAdvance()) / 2.0F;
            } else if (align == Align.JUSTIFY) {
                // Don't justify the last line.
                if (measurer.getPosition() < text.length()) {
                    layout = layout.getJustifiedLayout((float) width);
                }
            }
            layout.draw(g, (float) (x + dx), (float) y);
            y += layout.getDescent() + layout.getLeading() + layout.getAscent();
        }
        g.setTransform(new AffineTransform());
    }

    public Rect getBounds() {
        // TODO: Implement
        return new Rect(0, 0, 1000, 1000);
    }

    public Text clone() {
        return new Text(this);
    }
}
