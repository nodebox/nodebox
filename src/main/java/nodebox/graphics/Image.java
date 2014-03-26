package nodebox.graphics;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;

import static nodebox.graphics.MathUtils.clamp;

public class Image extends AbstractGrob {

    private double x, y;
    private double desiredWidth, desiredHeight;
    private double alpha = 1;

    private BufferedImage image;
    private static BufferedImage blankImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    public static final String BLANK_IMAGE = "__blank";

    public Image() {
        this(new File(BLANK_IMAGE));
    }

    public Image(File file) {
        if (file == null || file.getPath().equals(BLANK_IMAGE)) {
            image = blankImage;
        } else {
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new RuntimeErrorException(null, "Could not read image " + file);
            }
        }
    }

    public Image(String fname) {
        this(new File(fname));
    }

    public Image(String fname, double cx, double cy) {
        this(new File(fname));
        this.x = cx;
        this.y = cy;
    }

    public Image(BufferedImage image) {
        this.image = image;
    }

    public Image(Image other) {
        super(other);
        this.x = other.x;
        this.y = other.y;
        this.desiredWidth = other.desiredWidth;
        this.desiredHeight = other.desiredHeight;
        this.alpha = other.alpha;
        this.image = other.image;
    }

    public static Image fromData(byte[] data) {
        InputStream istream = new BufferedInputStream(new ByteArrayInputStream(data));
        try {
            return new Image(ImageIO.read(istream));
        } catch (IOException e) {
            throw new RuntimeErrorException(null, "Could not read image data.");
        }
    }

    //// Attribute access ////

    public double getOriginalWidth() {
        if (image == null) return 0;
        return image.getWidth();
    }

    public double getOriginalHeight() {
        if (image == null) return 0;
        return image.getHeight();
    }

    public double getWidth() {
        return getOriginalWidth() * getScaleFactor();
    }

    public void setWidth(double width) {
        this.desiredWidth = width;
    }

    public double getHeight() {
        return getOriginalHeight() * getScaleFactor();
    }

    public void setHeight(double height) {
        this.desiredHeight = height;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public BufferedImage getAwtImage() {
        return image;
    }

    public Size getSize() {
        return new Size(image.getWidth(), image.getHeight());
    }

    //// Transformations ////

    protected void setupTransform(Graphics2D g) {
        saveTransform(g);
        AffineTransform trans = g.getTransform();
        trans.concatenate(getTransform().getAffineTransform());
        g.setTransform(trans);
    }

    //// Grob support ////


    public boolean isEmpty() {
        return image == null || image.getWidth() == 0 || image.getHeight() == 0;
    }

    public Rect getBounds() {
        if (image == null) return new Rect();
        double factor = getScaleFactor();
        double finalWidth = image.getWidth() * factor;
        double finalHeight = image.getHeight() * factor;
        return new Rect(x - finalWidth / 2, y - finalHeight / 2, finalWidth, finalHeight);
    }

    public double getScaleFactor() {
        if (desiredWidth != 0 || desiredHeight != 0) {
            double srcW = image.getWidth();
            double srcH = image.getHeight();
            if (desiredWidth != 0 && desiredHeight != 0) {
                // Both width and height were given, constrain to smallest
                return Math.min(desiredWidth / srcW, desiredHeight / srcH);
            } else if (desiredWidth != 0) {
                return desiredWidth / srcW;
            } else {
                return desiredHeight / srcH;
            }
        } else {
            return 1;
        }
    }

    public void draw(Graphics2D g) {
        setupTransform(g);
        // You can only position an image using an affine transformation.
        // We use the transformation to translate the image to the specified
        // position, and scale it according to the given width and height.
        Transform imageTrans = new Transform();
        // Move to the image position. Convert x, y, which are centered coordinates,
        // to "real" coordinates. 
        double factor = getScaleFactor();
        double finalWidth = image.getWidth() * factor;
        double finalHeight = image.getHeight() * factor;
        imageTrans.translate(x - finalWidth / 2, y - finalHeight / 2);
        // Scaling only applies to image that have their desired width and/or height set.
        // However, getScaleFactor return 1 if height/width are not set, in effect negating
        // the effect of the scale.
        imageTrans.scale(getScaleFactor());
        double a = clamp(alpha);
        Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a);
        Composite oldComposite = g.getComposite();
        g.setComposite(composite);
        g.drawRenderedImage(image, imageTrans.getAffineTransform());
        g.setComposite(oldComposite);
        restoreTransform(g);
    }


    public Image clone() {
        return new Image(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Image)) return false;
        Image other = (Image) obj;
        return this.x == other.x
                && this.y == other.y
                && this.desiredWidth == other.desiredWidth
                && this.desiredHeight == other.desiredHeight
                && this.alpha == other.alpha
                && this.image.equals(other.image)
                && super.equals(other);
    }

    @Override
    public String toString() {
        return "<Image (" + getWidth() + ", " + getHeight() + ")>";
    }
}
