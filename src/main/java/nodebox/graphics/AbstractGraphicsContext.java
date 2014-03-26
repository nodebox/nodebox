package nodebox.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public abstract class AbstractGraphicsContext implements GraphicsContext {

    // TODO: Support output mode
    protected Color.Mode colorMode;
    protected double colorRange;
    protected Color fillColor;
    protected Color strokeColor;
    protected double strokeWidth;
    protected Path path;
    protected boolean autoClosePath;
    protected boolean pathClosed;
    protected Transform.Mode transformMode;
    protected Transform transform = new Transform();
    protected ArrayList<Transform> transformStack;
    protected String fontName;
    protected double fontSize;
    protected double lineHeight;
    protected Text.Align align;
    protected GraphicsContext.RectMode rectMode = GraphicsContext.RectMode.CORNER;
    protected GraphicsContext.EllipseMode ellipseMode = GraphicsContext.EllipseMode.CORNER;

    public void resetContext() {
        colorMode = Color.Mode.RGB;
        colorRange = 1;
        fillColor = new Color();
        strokeColor = null;
        strokeWidth = 1;
        path = null;
        autoClosePath = true;
        transformMode = Transform.Mode.CENTER;
        transform = new Transform();
        transformStack = new ArrayList<Transform>();
        fontName = "Helvetica";
        fontSize = 24;
        lineHeight = 1.2;
        align = Text.Align.LEFT;
    }

    //// Primitives ////

    // TODO: Support rect modes.


    public RectMode rectmode() {
        return rectMode;
    }

    public RectMode rectmode(RectMode m) {
        return rectMode = m;
    }

    public RectMode rectmode(String m) {
        try {
            RectMode newMode = RectMode.valueOf(m.toUpperCase(Locale.US));
            return rectMode = newMode;
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("rectmode: available types for rectmode() are CORNER, CENTER, CORNERS and RADIUS\\n\"");
        }
    }

    public RectMode rectmode(int m) {
        try {
            RectMode newMode = RectMode.values()[m];
            return rectMode = newMode;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("rectmode: available types for rectmode() are CORNER, CENTER, CORNERS and RADIUS\\n\"");
        }
    }

    private Path createPath() {
        Path p = new Path();
        p.setTransformDelegate(new ContextTransformDelegate(this));
        return p;
    }

    public Path Path() {
        return createPath();
    }

    public Path BezierPath() {
        return createPath();
    }

    public Path rect(Rect r) {
        return rect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), true);
    }

    public Path rect(Rect r, boolean draw) {
        return rect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), draw);
    }

    public Path rect(double x, double y, double width, double height) {
        return rect(x, y, width, height, true);
    }

    public Path rect(double x, double y, double width, double height, boolean draw) {
        Path p = createPath();
        switch (rectMode) {
            case CENTER:
                p.rect(x, y, width, height);
                break;
            case CORNER:
                p.cornerRect(x, y, width, height);
                break;
        }
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    public Path rect(Rect r, double roundness) {
        return rect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), roundness, roundness, true);
    }

    public Path rect(Rect r, double roundness, boolean draw) {
        return rect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), roundness, roundness, draw);
    }

    public Path rect(double x, double y, double width, double height, double roundness) {
        return rect(x, y, width, height, roundness, roundness, true);
    }

    public Path rect(double x, double y, double width, double height, double roundness, boolean draw) {
        return rect(x, y, width, height, roundness, roundness, draw);
    }

    public Path rect(double x, double y, double width, double height, double rx, double ry) {
        return rect(x, y, width, height, rx, ry, true);
    }

    public Path rect(double x, double y, double width, double height, double rx, double ry, boolean draw) {
        Path p = createPath();
        switch (rectMode) {
            case CENTER:
                p.rect(x, y, width, height, rx, ry);
                break;
            case CORNER:
                p.cornerRect(x, y, width, height, rx, ry);
                break;
        }
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    public EllipseMode ellipsemode() {
        return ellipseMode;
    }

    public EllipseMode ellipsemode(EllipseMode m) {
        return ellipseMode = m;
    }

    public EllipseMode ellipsemode(String m) {
        try {
            EllipseMode newMode = EllipseMode.valueOf(m.toUpperCase(Locale.US));
            return ellipseMode = newMode;
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("ellipsemode: available types for ellipsemode() are CORNER, CENTER, CORNERS and RADIUS\\n\"");
        }
    }

    public EllipseMode ellipsemode(int m) {
        try {
            EllipseMode newMode = EllipseMode.values()[m];
            return ellipseMode = newMode;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("ellipsemode: available types for ellipsemode() are CORNER, CENTER, CORNERS and RADIUS\\n\"");
        }
    }

    public Path oval(double x, double y, double width, double height) {
        // TODO: Deprecation warning
        return ellipse(x, y, width, height, true);
    }

    public Path oval(double x, double y, double width, double height, boolean draw) {
        // TODO: Deprecation warning
        return ellipse(x, y, width, height, draw);
    }

    public Path ellipse(double x, double y, double width, double height) {
        return ellipse(x, y, width, height, true);
    }

    public Path ellipse(double x, double y, double width, double height, boolean draw) {
        Path p = createPath();
        switch (ellipseMode) {
            case CENTER:
                p.ellipse(x, y, width, height);
                break;
            case CORNER:
                p.cornerEllipse(x, y, width, height);
                break;
        }
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    public Path line(double x1, double y1, double x2, double y2) {
        return line(x1, y1, x2, y2, true);
    }

    public Path line(double x1, double y1, double x2, double y2, boolean draw) {
        Path p = createPath();
        p.line(x1, y1, x2, y2);
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    public Path star(double cx, double cy) {
        return star(cx, cy, 20, 100, 50, true);
    }

    public Path star(double cx, double cy, int points) {
        return star(cx, cy, points, 100, 50, true);
    }

    public Path star(double cx, double cy, int points, double outer) {
        return star(cx, cy, points, outer, 50, true);
    }

    public Path star(double cx, double cy, int points, double outer, double inner) {
        return star(cx, cy, points, outer, inner, true);
    }

    public Path star(double cx, double cy, int points, double outer, double inner, boolean draw) {
        double PI = (double) Math.PI;
        Path p = createPath();
        p.moveto(cx, cy + outer);
        for (int i = 1; i < points * 2; i++) {
            double angle = i * PI / points;
            double x = (double) Math.sin(angle);
            double y = (double) Math.cos(angle);
            double radius = i % 2 == 0 ? outer : inner;
            x += cx + radius * x;
            y += cy + radius * y;
            p.lineto(x, y);
        }
        p.close();
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    public Path arrow(double x, double y) {
        return arrow(x, y, 100, ArrowType.NORMAL, true);
    }

    public Path arrow(double x, double y, ArrowType type) {
        return arrow(x, y, 100, type, true);
    }

    public Path arrow(double x, double y, String type) {
        return arrow(x, y, 100, type, true);
    }

    public Path arrow(double x, double y, int type) {
        return arrow(x, y, 100, type, true);
    }

    public Path arrow(double x, double y, double width) {
        return arrow(x, y, width, NORMAL, true);
    }

    public Path arrow(double x, double y, double width, boolean draw) {
        return arrow(x, y, width, NORMAL, draw);
    }

    public Path arrow(double x, double y, double width, ArrowType type) {
        return arrow(x, y, width, type, true);
    }

    public Path arrow(double x, double y, double width, String type) {
        return arrow(x, y, width, type, true);
    }

    public Path arrow(double x, double y, double width, int type) {
        return arrow(x, y, width, type, true);
    }

    public Path arrow(double x, double y, double width, String type, boolean draw) {
        try {
            ArrowType arrowType = ArrowType.valueOf(type.toUpperCase(Locale.US));
            return arrow(x, y, width, arrowType, draw);
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("arrow: available types for arrow() are NORMAL and FORTYFIVE\\n\"");
        }
    }

    public Path arrow(double x, double y, double width, int type, boolean draw) {
        try {
            ArrowType arrowType = ArrowType.values()[type];
            return arrow(x, y, width, arrowType, draw);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("arrow: available types for arrow() are NORMAL and FORTYFIVE\\n\"");
        }
    }

    public Path arrow(double x, double y, double width, ArrowType type, boolean draw) {
        if (type == ArrowType.NORMAL)
            return arrowNormal(x, y, width, draw);
        else
            return arrowFortyFive(x, y, width, draw);
    }

    private Path arrowNormal(double x, double y, double width, boolean draw) {
        double head = width * .4;
        double tail = width * .2;

        Path p = createPath();
        p.moveto(x, y);
        p.lineto(x - head, y + head);
        p.lineto(x - head, y + tail);
        p.lineto(x - width, y + tail);
        p.lineto(x - width, y - tail);
        p.lineto(x - head, y - tail);
        p.lineto(x - head, y - head);
        p.lineto(x, y);
        p.close();
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    private Path arrowFortyFive(double x, double y, double width, boolean draw) {
        double head = .3;
        double tail = 1 + head;

        Path p = createPath();
        p.moveto(x, y);
        p.lineto(x, y + width * (1 - head));
        p.lineto(x - width * head, y + width);
        p.lineto(x - width * head, y + width * tail * .4);
        p.lineto(x - width * tail * .6, y + width);
        p.lineto(x - width, y + width * tail * .6);
        p.lineto(x - width * tail * .4, y + width * head);
        p.lineto(x - width, y + width * head);
        p.lineto(x - width * (1 - head), y);
        p.lineto(x, y);
        p.close();
        inheritFromContext(p);
        if (draw)
            addPath(p);
        return p;
    }

    //// Path commands ////

    public void beginpath() {
        path = createPath();
        pathClosed = false;
    }

    public void beginpath(double x, double y) {
        beginpath();
        moveto(x, y);
    }

    public void moveto(double x, double y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        path.moveto(x, y);
    }

    public void lineto(double x, double y) {
        if (path == null)
            throw new NodeBoxError("No current path. Use beginpath() first.");
        path.lineto(x, y);
    }

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
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
            addPath(p);
        // Initialize a new path
        path = null;
        pathClosed = false;
        return p;
    }

    public void drawpath(Path path) {
        inheritFromContext(path);
        addPath(path);
    }

    public void drawpath(Iterable<Point> points) {
        Path path = createPath();
        for (Point pt : points) {
            path.addPoint(pt);
        }
        inheritFromContext(path);
        addPath(path);
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

    public Path findpath(List<Point> points, double curvature) {
        Path path = Path.findPath(points, curvature);
        inheritFromContext(path);
        addPath(path);
        return path;
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


    public Transform.Mode transform() {
        return transformMode;
    }

    public Transform.Mode transform(Transform.Mode mode) {
        return transformMode = mode;
    }

    public Transform.Mode transform(String mode) {
        try {
            Transform.Mode newMode = Transform.Mode.valueOf(mode.toUpperCase(Locale.US));
            return transformMode = newMode;
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("transform: available types for transform() are CORNER and CENTER\\n\"");
        }
    }

    public Transform.Mode transform(int mode) {
        try {
            Transform.Mode newMode = Transform.Mode.values()[mode];
            return transformMode = newMode;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("transform: available types for transform() are CORNER and CENTER\\n\"");
        }
    }

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

    public String outputmode() {
        throw new RuntimeException("outputmode is not implemented yet.");
    }

    public String outputmode(String mode) {
        throw new RuntimeException("outputmode is not implemented yet.");
    }

    public Color.Mode colormode() {
        return colorMode;
    }

    public Color.Mode colormode(Color.Mode mode) {
        return colorMode = mode;
    }

    public Color.Mode colormode(Color.Mode mode, double range) {
        colorRange = range;
        return colorMode = mode;
    }

    public Color.Mode colormode(String mode) {
        return colormode(mode, colorRange);
    }

    public Color.Mode colormode(String mode, double range) {
        try {
            Color.Mode newMode = Color.Mode.valueOf(mode.toUpperCase(Locale.US));
            return colormode(newMode, range);
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("colormode: available types for colormode() are RGB, HSB and CMYK\\n\"");
        }
    }

    public Color.Mode colormode(int mode) {
        return colormode(mode, colorRange);
    }

    public Color.Mode colormode(int mode, double range) {
        try {
            Color.Mode newMode = Color.Mode.values()[mode];
            return colormode(newMode, range);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("colormode: available types for colormode() are RGB, HSB and CMYK\\n\"");
        }
    }

    public double colorrange() {
        return colorRange;
    }

    public double colorrange(double range) {
        return colorRange = range;
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
    public Color color(double x) {
        double nx = normalize(x);
        return new Color(nx, nx, nx);
    }

    /**
     * Create a new color with the given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the new color.
     */
    public Color color(double x, double y) {
        double nx = normalize(x);
        return new Color(nx, nx, nx, normalize(y));
    }

    /**
     * Create a new color with the the given R/G/B or H/S/B value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @return the new color.
     */
    public Color color(double x, double y, double z) {
        return new Color(normalize(x), normalize(y), normalize(z), colormode());
    }

    /**
     * Create a new color with the the given R/G/B/A or H/S/B/A value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @param a the alpha component.
     * @return the new color.
     */
    public Color color(double x, double y, double z, double a) {
        return new Color(normalize(x), normalize(y), normalize(z), normalize(a), colormode());
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
    public Color fill(double x) {
        double nx = normalize(x);
        return fillColor = new Color(nx, nx, nx);
    }

    /**
     * Set the current fill color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current fill color.
     */
    public Color fill(double x, double y) {
        double nx = normalize(x);
        return fillColor = new Color(nx, nx, nx, normalize(y));
    }

    /**
     * Set the current fill color to the given R/G/B or H/S/B value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @return the current fill color.
     */
    public Color fill(double x, double y, double z) {
        return fillColor = new Color(normalize(x), normalize(y), normalize(z), colormode());
    }

    /**
     * Set the current fill color to the given R/G/B/A or H/S/B/A value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @param a the alpha component.
     * @return the current fill color.
     */
    public Color fill(double x, double y, double z, double a) {
        return fillColor = new Color(normalize(x), normalize(y), normalize(z), normalize(a), colormode());
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
    public Color stroke(double x) {
        double nx = normalize(x);
        return strokeColor = new Color(nx, nx, nx);
    }

    /**
     * Set the current stroke color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y) {
        double nx = normalize(x);
        return strokeColor = new Color(nx, nx, nx, normalize(y));
    }

    /**
     * Set the current stroke color to the given R/G/B or H/S/B value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y, double z) {
        return strokeColor = new Color(normalize(x), normalize(y), normalize(z), colormode());
    }

    /**
     * Set the current stroke color to the given R/G/B/A or H/S/B/A value.
     *
     * @param x the red or hue component.
     * @param y the green or saturation component.
     * @param z the blue or brightness component.
     * @param a the alpha component.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y, double z, double a) {
        return strokeColor = new Color(normalize(x), normalize(y), normalize(z), normalize(a), colormode());
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

    public double strokewidth() {
        return strokeWidth;
    }

    public double strokewidth(double w) {
        return strokeWidth = w;
    }

    //// Image commands ////

    public Image image(String path, double x, double y) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(String path, double x, double y, double width) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(String path, double x, double y, double width, double height) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(String path, double x, double y, double width, double height, double alpha) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(String path, double x, double y, double width, double height, boolean draw) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(String path, double x, double y, double width, double height, double alpha, boolean draw) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(Image img, double x, double y, double width, double height, double alpha, boolean draw) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Image image(BufferedImage img, double x, double y, double width, double height, double alpha, boolean draw) {
        throw new RuntimeException("'image' is not applicable to this type of GraphicsContext.");
    }

    public Size imagesize(String path) {
        throw new RuntimeException("'imagesize' is not applicable to this type of GraphicsContext.");
    }

    public Size imagesize(Image img) {
        throw new RuntimeException("'imagesize' is not applicable to this type of GraphicsContext.");
    }

    public Size imagesize(BufferedImage img) {
        throw new RuntimeException("'imagesize' is not applicable to this type of GraphicsContext.");
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

    public String font(String fontName, double fontSize) {
        font(fontName);
        fontsize(fontSize);
        return fontName;
    }

    public double fontsize() {
        return fontSize;
    }

    public double fontsize(double s) {
        return fontSize = s;
    }

    public double lineheight() {
        return lineHeight;
    }

    public double lineheight(double lineHeight) {
        return this.lineHeight = lineHeight;
    }

    public Text.Align align() {
        return align;
    }

    public Text.Align align(Text.Align align) {
        return this.align = align;
    }

    public Text.Align align(String align) {
        try {
            Text.Align newAlign = Text.Align.valueOf(align.toUpperCase(Locale.US));
            return this.align = newAlign;
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("align: available types for align() are LEFT, RIGHT, CENTER and JUSTIFY\\n\"");
        }
    }

    public Text.Align align(int align) {
        try {
            Text.Align newAlign = Text.Align.values()[align];
            return this.align = newAlign;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("align: available types for align() are LEFT, RIGHT, CENTER and JUSTIFY\\n\"");
        }
    }

    public Text text(String text, double x, double y) {
        return text(text, x, y, 0, 0, true);
    }

    public Text text(String text, double x, double y, double width) {
        return text(text, x, y, width, 0, true);
    }

    public Text text(String text, double x, double y, double width, double height) {
        return text(text, x, y, width, height, true);
    }

    public Text text(String text, double x, double y, double width, double height, boolean draw) {
        Text t = new Text(text, x, y, width, height);
        t.setTransformDelegate(new ContextTransformDelegate(this));
        inheritFromContext(t);
        if (draw)
            addText(t);
        return t;
    }

    public Path textpath(String text, double x, double y) {
        return textpath(text, x, y, 0, 0);
    }

    public Path textpath(String text, double x, double y, double width) {
        return textpath(text, x, y, width, 0);
    }

    public Path textpath(String text, double x, double y, double width, double height) {
        Text t = new Text(text, x, y, width, height);
        inheritFontAttributesFromContext(t);
        Path path = t.getPath();
        path.setTransformDelegate(new ContextTransformDelegate(this));
        inheritFromContext(path);
        return path;
    }

    public Rect textmetrics(String text) {
        return textmetrics(text, 0, 0);
    }

    public Rect textmetrics(String text, double width) {
        return textmetrics(text, width, 0);
    }

    public Rect textmetrics(String text, double width, double height) {
        Text t = new Text(text, 0, 0, width, height);
        inheritFromContext(t);
        return t.getMetrics();
    }

    public double textwidth(String text) {
        return textmetrics(text, 0, 0).getWidth();
    }

    public double textwidth(String text, double width) {
        return textmetrics(text, width).getWidth();
    }

    public double textheight(String text) {
        return textmetrics(text, 0, 0).getHeight();
    }

    public double textheight(String text, double width) {
        return textmetrics(text, width).getHeight();
    }

    //// Utility methods ////

    public void var(String name, VarType type) {
        var(name, type, null, 0, 1000);
    }

    public void var(String name, String type) {
        var(name, type, null, 0, 1000);
    }

    public void var(String name, int type) {
        var(name, type, 0, 0, 1000);
    }

    public void var(String name, VarType type, Object value) {
        var(name, type, value, 0, 1000);
    }

    public void var(String name, String type, Object value) {
        var(name, type, value, 0, 1000);
    }

    public void var(String name, int type, Object value) {
        var(name, type, value, 0, 1000);
    }

    public void var(String name, String type, Object value, double min, double max) {
        try {
            var(name, VarType.valueOf(type.toUpperCase(Locale.US)), value, min, max);
        } catch (IllegalArgumentException e) {
            throw new NodeBoxError("var: available types for var() are NUMBER, TEXT, BOOLEAN and FONT \\n\"");
        }
    }

    public void var(String name, int type, Object value, double min, double max) {
        try {
            var(name, VarType.values()[type], value, min, max);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NodeBoxError("var: available types for var() are NUMBER, TEXT, BOOLEAN and FONT \\n\"");
        }
    }

    public void var(String name, VarType type, Object value, double min, double max) {
//        Node node = ProcessingContext.getCurrentContext().getNode();
//        if (node == null) return;
//        Parameter p = node.getInput(name);
//        if (p != null) {
//            if (p.getType() != type.type) {
//                p.setType(type.type);
//            }
//            if (p.getWidget() != type.widget) {
//                p.setWidget(type.widget);
//            }
//            if (p.getMinimumValue() != null && !p.getMinimumValue().equals(min)) {
//                p.setMinimumValue(min);
//            }
//            if (p.getMaximumValue() != null && !p.getMaximumValue().equals(max)) {
//                p.setMaximumValue(max);
//            }
//        } else {
//            p = node.addPort(name, type.type);
//            p.setWidget(type.widget);
//            if (value != null) {
//                p.setValue(value);
//            }
//            if (min != null || max != null) {
//                p.setBoundingMethod(Parameter.BoundingMethod.HARD);
//                p.setMinimumValue(min);
//                p.setMaximumValue(max);
//            }
//            node.updateDependencies(ProcessingContext.getCurrentContext());
//        }
    }

    public Object findVar(String name) {
//        Node node = ProcessingContext.getCurrentContext().getNode();
//        if (node == null) return null;
//        return node.getInput(name);
        return null;
    }

    protected double normalize(double v) {
        // Bring the color into the 0-1 scale for the current colorrange
        if (colorRange == 1) return v;
        return v / colorRange;

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
        return objects.get((int) random(objects.size() - 1));
    }

    public Iterator<Point> grid(int columns, int rows) {
        return grid(columns, rows, 1, 1);
    }

    public Iterator<Point> grid(double columns, double rows) {
        return grid(Math.round(columns), Math.round(rows), 1, 1);
    }

    public Iterator<Point> grid(double columns, double rows, double columnSize, double rowSize) {
        return grid(Math.round(columns), Math.round(rows), columnSize, rowSize);
    }

    public Iterator<Point> grid(final int columns, final int rows, final double columnSize, final double rowSize) {
        return new Iterator<Point>() {
            int x = 0;
            int y = 0;

            public boolean hasNext() {
                return y < rows;
            }

            public Point next() {
                Point pt = new Point((double) (x * columnSize), (double) (y * rowSize));
                x++;
                if (x >= columns) {
                    x = 0;
                    y++;
                }
                return pt;
            }

            public void remove() {
            }
        };
    }


    //// Context drawing /////

    public void draw(Grob g) {
        if (g instanceof Path) {
            addPath((Path) g);
        } else if (g instanceof Geometry) {
            for (Path path : ((Geometry) g).getPaths()) {
                addPath(path);
            }
        } else if (g instanceof Contour) {
            addPath(((Contour) g).toPath());
        } else if (g instanceof Text) {
            addText((Text) g);
        } else {
            throw new IllegalArgumentException("Don't know how to add a " + g + " to the current context.");
        }
    }

    protected abstract void addPath(Path p);

    protected abstract void addText(Text t);

    protected void inheritFromContext(Path p) {
        p.setFillColor(fillColor == null ? null : fillColor.clone());
        p.setStrokeColor(strokeColor == null ? null : strokeColor.clone());
        p.setStrokeWidth(strokeWidth);
        TransformDelegate d = p.getTransformDelegate();
        d.transform(p, transform, true);
    }

    protected void inheritFromContext(Text t) {
        t.setFillColor(fillColor == null ? null : fillColor.clone());
        inheritFontAttributesFromContext(t);
        // todo: check if this is sufficient.
        TransformDelegate d = t.getTransformDelegate();
        d.transform(t, transform, true);
/*        Rect r = t.getBounds();
        double dx = r.getX() + r.getWidth() / 2;
        double dy = r.getY() + r.getHeight() / 2;
        if (transformMode == Transform.Mode.CENTER) {
            Transform trans = new Transform();
            trans.translate(dx, dy);
            trans.append(transform);
            trans.translate(-dx, -dy);
            t.setTransform(trans);
        } else {
            t.setTransform(transform);
        } */
    }

    private void inheritFontAttributesFromContext(Text t) {
        t.setFontName(fontName);
        t.setFontSize(fontSize);
        t.setLineHeight(lineHeight);
        t.setAlign(align);
    }

}
