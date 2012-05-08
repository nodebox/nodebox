package nodebox.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.Iterator;

public class Text extends AbstractGrob {

    public enum Align {
        LEFT, RIGHT, CENTER, JUSTIFY
    }

    private String text;
    private double baseLineX, baseLineY;
    private double width = 0;
    private double height = 0;
    private String fontName = "Helvetica";
    private double fontSize = 24;
    private double lineHeight = 1.2;
    private Align align = Align.CENTER;
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

    //// Font management ////

    public static boolean fontExists(String fontName) {
        // TODO: Move getAllFonts() in static attribute.
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = env.getAllFonts();
        for (Font font : allFonts) {
            if (font.getName().equals(fontName)) {
                return true;
            }
        }
        return false;
    }

    //// Metrics ////

    private AttributedString getStyledText(String text) {
        // TODO: Find a better way to handle empty Strings (like for example paragraph line breaks)
        if (text.length() == 0)
            text = " ";
        AttributedString attrString = new AttributedString(text);
        attrString.addAttribute(TextAttribute.FONT, getFont());
        if (fillColor != null)
            attrString.addAttribute(TextAttribute.FOREGROUND, fillColor.getAwtColor());
        if (align == Align.RIGHT) {
            //attrString.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        } else if (align == Align.CENTER) {
            // TODO: Center alignment?
        } else if (align == Align.JUSTIFY) {
            attrString.addAttribute(TextAttribute.JUSTIFICATION, TextAttribute.JUSTIFICATION_FULL);
        }
        return attrString;
    }

    public Rect getMetrics() {
        if (text == null || text.length() == 0) return new Rect();
        TextLayoutIterator iterator = new TextLayoutIterator();
        Rectangle2D bounds = new Rectangle2D.Double();
        while (iterator.hasNext()) {
            TextLayout layout = iterator.next();
            // TODO: Compensate X, Y
            bounds = bounds.createUnion(layout.getBounds());
        }
        return new Rect(bounds);
    }

    //// Transformations ////

    protected void setupTransform(Graphics2D g) {
        saveTransform(g);
        AffineTransform trans = g.getTransform();
        trans.concatenate(getTransform().getAffineTransform());
        g.setTransform(trans);
    }

    public void draw(Graphics2D g) {
        if (fillColor == null) return;
        setupTransform(g);
        if (text == null || text.length() == 0) return;
        TextLayoutIterator iterator = new TextLayoutIterator();
        while (iterator.hasNext()) {
            TextLayout layout = iterator.next();
            layout.draw(g, (float) (baseLineX + iterator.getX()), (float) (baseLineY + iterator.getY()));
        }
        restoreTransform(g);
    }

    public Path getPath() {
        Path p = new Path();
        p.setFillColor(fillColor == null ? null : fillColor.clone());
        TextLayoutIterator iterator = new TextLayoutIterator();
        while (iterator.hasNext()) {
            TextLayout layout = iterator.next();
            AffineTransform trans = new AffineTransform();
            trans.translate(baseLineX + iterator.getX(), baseLineY + iterator.getY());
            Shape shape = layout.getOutline(trans);
            p.extend(shape);
        }
        p.transform(getTransform());
        return p;
    }

    public boolean isEmpty() {
        return text.trim().length() == 0;
    }

    public Rect getBounds() {
        // TODO: This is correct, but creating a full path just for measuring bounds is slow.
        return getPath().getBounds();
    }

    public Text clone() {
        return new Text(this);
    }

    private class TextLayoutIterator implements Iterator<TextLayout> {

        private double x, y;
        private double ascent;
        private int currentIndex = 0;
        private String[] textParts;
        private LineBreakMeasurer[] measurers;
        private LineBreakMeasurer currentMeasurer;
        private String currentText;
        private boolean first;

        private TextLayoutIterator() {
            x = 0;
            y = 0;
            textParts = text.split("\n");
            measurers = new LineBreakMeasurer[textParts.length];
            FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            for (int i = 0; i < textParts.length; i++) {
                AttributedString s = getStyledText(textParts[i]);
                measurers[i] = new LineBreakMeasurer(s.getIterator(), frc);
            }
            currentMeasurer = measurers[currentIndex];
            currentText = textParts[currentIndex];
            first = true;
        }

        public boolean hasNext() {
            if (currentMeasurer.getPosition() < currentText.length())
                return true;
            else {
                currentIndex++;
                if (currentIndex < textParts.length) {
                    currentMeasurer = measurers[currentIndex];
                    currentText = textParts[currentIndex];
                    return hasNext();
                } else {
                    return false;
                }
            }
        }

        public TextLayout next() {
            if (first) {
                first = false;
            } else {
                y += ascent * lineHeight;
            }
            double layoutWidth = width == 0 ? Float.MAX_VALUE : width;

            TextLayout layout = currentMeasurer.nextLayout((float) layoutWidth);
            if (width == 0) {
                layoutWidth = layout.getAdvance();
                if (align == Align.RIGHT) {
                    x = -layoutWidth;
                } else if (align == Align.CENTER) {
                    x = -layoutWidth / 2.0;
                }
            } else if (align == Align.RIGHT) {
                x = width - layout.getAdvance();
            } else if (align == Align.CENTER) {
                x = (width - layout.getAdvance()) / 2.0;
            } else if (align == Align.JUSTIFY) {
                // Don't justify the last line.
                if (currentMeasurer.getPosition() < currentText.length()) {
                    layout = layout.getJustifiedLayout((float) width);
                }
            }
            ascent = layout.getAscent();
            // y += layout.getDescent() + layout.getLeading() + layout.getAscent();

            return layout;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void remove() {
            throw new AssertionError("This operation is not implemented");
        }
    }
}
