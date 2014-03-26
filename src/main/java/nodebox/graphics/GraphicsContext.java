package nodebox.graphics;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

public interface GraphicsContext {

    public static final double inch = 72;
    public static final double cm = 28.3465;
    public static final double mm = 2.8346;

    public enum RectMode {
        CORNER, CORNERS, CENTER, RADIUS
    }

    public enum EllipseMode {
        CENTER, RADIUS, CORNER, CORNERS
    }

    public static enum VarType {
        NUMBER("float", "float"),
        TEXT("string", "text"),
        BOOLEAN("int", "toggle"),
        FONT("string", "font");

        public final String type;
        public final String widget;

        VarType(String type, String widget) {
            this.type = type;
            this.widget = widget;
        }
    }

    public enum ArrowType {NORMAL, FORTYFIVE}

    public static final String CORNER = "CORNER";
    public static final String CENTER = "CENTER";
    public static final String CORNERS = "CORNERS";
    public static final String RADIUS = "RADIUS";

    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String JUSTIFY = "JUSTIFY";

    public static final String RGB = "RGB";
    public static final String HSB = "HSB";
    public static final String CMYK = "CMYK";

    public static final String NUMBER = "NUMBER";
    public static final String TEXT = "TEXT";
    public static final String BOOLEAN = "BOOLEAN";
    public static final String FONT = "FONT";

    public static final String NORMAL = "NORMAL";
    public static final String FORTYFIVE = "FORTYFIVE";

    public RectMode rectmode();

    public RectMode rectmode(RectMode m);

    public RectMode rectmode(String m);

    public RectMode rectmode(int m);

    public Path rect(Rect r);

    public Path rect(double x, double y, double width, double height);

    public Path rect(Rect r, double roundness);

    public Path rect(double x, double y, double width, double height, double roundness);

    public Path rect(double x, double y, double width, double height, double rx, double ry);

    public EllipseMode ellipsemode();

    public EllipseMode ellipsemode(EllipseMode m);

    public EllipseMode ellipsemode(String m);

    public EllipseMode ellipsemode(int m);

    public Path oval(double x, double y, double width, double height);

    public Path oval(double x, double y, double width, double height, boolean draw);

    public Path ellipse(double x, double y, double width, double height);

    public Path ellipse(double x, double y, double width, double height, boolean draw);

    public Path line(double x1, double y1, double x2, double y2);

    public Path line(double x1, double y1, double x2, double y2, boolean draw);

    public Path star(double cx, double cy);

    public Path star(double cx, double cy, int points);

    public Path star(double cx, double cy, int points, double outer);

    public Path star(double cx, double cy, int points, double outer, double inner);

    public Path star(double cx, double cy, int points, double outer, double inner, boolean draw);

    public Path arrow(double x, double y);

    public Path arrow(double x, double y, ArrowType type);

    public Path arrow(double x, double y, String type);

    public Path arrow(double x, double y, int type);

    public Path arrow(double x, double y, double width);

    public Path arrow(double x, double y, double width, boolean draw);

    public Path arrow(double x, double y, double width, ArrowType type);

    public Path arrow(double x, double y, double width, String type);

    public Path arrow(double x, double y, double width, int type);

    public Path arrow(double x, double y, double width, ArrowType type, boolean draw);

    public Path arrow(double x, double y, double width, String type, boolean draw);

    public Path arrow(double x, double y, double width, int type, boolean draw);

    public void beginpath();

    public void beginpath(double x, double y);

    public void moveto(double x, double y);

    public void lineto(double x, double y);

    public void curveto(double x1, double y1, double x2, double y2, double x3, double y3);

    public void closepath();

    public Path endpath();

    public Path endpath(boolean draw);

    public void drawpath(Path path);

    public void drawpath(Iterable<Point> points);

    public boolean autoclosepath();

    public boolean autoclosepath(boolean c);

    public Path findpath(List<Point> points);

    public Path findpath(List<Point> points, double curvature);

    public void beginclip(Path p);

    public void endclip();

    public Transform.Mode transform();

    public Transform.Mode transform(Transform.Mode mode);

    public Transform.Mode transform(int mode);

    public Transform.Mode transform(String mode);

    public void push();

    public void pop();

    public void reset();

    public void translate(double tx, double ty);

    public void rotate(double r);

    public void scale(double scale);

    public void scale(double sx, double sy);

    public void skew(double skew);

    public void skew(double kx, double ky);

    public String outputmode();

    public String outputmode(String mode);

    public Color.Mode colormode();

    public Color.Mode colormode(Color.Mode mode);

    public Color.Mode colormode(Color.Mode mode, double range);

    public Color.Mode colormode(String mode);

    public Color.Mode colormode(String mode, double range);

    public Color.Mode colormode(int mode);

    public Color.Mode colormode(int mode, double range);

    public double colorrange();

    public double colorrange(double range);

    /**
     * Create an empty (black) color object.
     *
     * @return the new color.
     */
    public Color color();

    /**
     * Create a new color with the given grayscale value.
     *
     * @param x the gray component.
     * @return the new color.
     */
    public Color color(double x);

    /**
     * Create a new color with the given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the new color.
     */
    public Color color(double x, double y);

    /**
     * Create a new color with the the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the new color.
     */
    public Color color(double x, double y, double z);

    /**
     * Create a new color with the the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the new color.
     */
    public Color color(double x, double y, double z, double a);

    /**
     * Create a new color with the the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the new color is turned off (same as nocolor).
     *
     * @param c the color object.
     * @return the new color.
     */
    public Color color(Color c);

    /**
     * Get the current fill color.
     *
     * @return the current fill color.
     */
    public Color fill();

    /**
     * Set the current fill color to given grayscale value.
     *
     * @param x the gray component.
     * @return the current fill color.
     */
    public Color fill(double x);

    /**
     * Set the current fill color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current fill color.
     */
    public Color fill(double x, double y);

    /**
     * Set the current fill color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current fill color.
     */
    public Color fill(double x, double y, double z);

    /**
     * Set the current fill color to the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the current fill color.
     */
    public Color fill(double x, double y, double z, double a);

    /**
     * Set the current fill color to the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the current fill color is turned off (same as nofill).
     *
     * @param c the color object.
     * @return the current fill color.
     */
    public Color fill(Color c);

    /**
     * Turn off the fill color.
     */
    public void nofill();

    /**
     * Get the current stroke color.
     *
     * @return the current stroke color.
     */
    public Color stroke();

    /**
     * Set the current stroke color to given grayscale value.
     *
     * @param x the gray component.
     * @return the current stroke color.
     */
    public Color stroke(double x);

    /**
     * Set the current stroke color to given grayscale and alpha value.
     *
     * @param x the grayscale value.
     * @param y the alpha value.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y);

    /**
     * Set the current stroke color to the given R/G/B value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y, double z);

    /**
     * Set the current stroke color to the given R/G/B/A value.
     *
     * @param x the red component.
     * @param y the green component.
     * @param z the blue component.
     * @param a the alpha component.
     * @return the current stroke color.
     */
    public Color stroke(double x, double y, double z, double a);

    /**
     * Set the current stroke color to the given color.
     * <p/>
     * The color object is cloned; you can change the original afterwards.
     * If the color object is null, the current stroke color is turned off (same as nostroke).
     *
     * @param c the color object.
     * @return the current stroke color.
     */
    public Color stroke(Color c);

    /**
     * Turn off the stroke color.
     */
    public void nostroke();

    public double strokewidth();

    public double strokewidth(double w);

    public String font();

    public String font(String fontName);

    public String font(String fontName, double fontSize);

    public double fontsize();

    public double fontsize(double s);

    public double lineheight();

    public double lineheight(double lineHeight);

    public Text.Align align();

    public Text.Align align(Text.Align align);

    public Text.Align align(String align);

    public Text.Align align(int align);

    public Image image(String path, double x, double y);

    public Image image(String path, double x, double y, double width);

    public Image image(String path, double x, double y, double width, double height);

    public Image image(String path, double x, double y, double width, double height, double alpha);

    public Image image(String path, double x, double y, double width, double height, boolean draw);

    public Image image(String path, double x, double y, double width, double height, double alpha, boolean draw);

    public Image image(Image img, double x, double y, double width, double height, double alpha, boolean draw);

    public Image image(BufferedImage img, double x, double y, double width, double height, double alpha, boolean draw);

    public Size imagesize(String path);

    public Size imagesize(Image img);

    public Size imagesize(BufferedImage img);

    public Text text(String text, double x, double y);

    public Text text(String text, double x, double y, double width);

    public Text text(String text, double x, double y, double width, double height);

    public Text text(String text, double x, double y, double width, double height, boolean draw);

    public Path textpath(String text, double x, double y);

    public Path textpath(String text, double x, double y, double width);

    public Path textpath(String text, double x, double y, double width, double height);

    public Rect textmetrics(String text);

    public Rect textmetrics(String text, double width);

    public Rect textmetrics(String text, double width, double height);

    public double textwidth(String text);

    public double textwidth(String text, double width);

    public double textheight(String text);

    public double textheight(String text, double width);

    public void var(String name, VarType type);

    public void var(String name, String type);

    public void var(String name, int type);

    public void var(String name, VarType type, Object value);

    public void var(String name, String type, Object value);

    public void var(String name, int type, Object value);

    public void var(String name, VarType type, Object value, double min, double max);

    public void var(String name, String type, Object value, double min, double max);

    public void var(String name, int type, Object value, double min, double max);

    public Object findVar(String name);

    public double random();

    public long random(int max);

    public long random(int min, int max);

    public double random(double max);

    public double random(double min, double max);

    public Object choice(List objects);

    public Iterator<Point> grid(int columns, int rows);

    public Iterator<Point> grid(int columns, int rows, double columnSize, double rowSize);

    public void draw(Grob g);
}
