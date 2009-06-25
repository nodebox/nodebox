package nodebox.graphics;

import nodebox.util.MathUtils;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Image extends AbstractGrob {

    private static HashMap<String, RenderedImage> imageCache = new HashMap<String, RenderedImage>();

    private float x, y;
    private float desiredWidth, desiredHeight;
    private float alpha = 1.0F;

    private RenderedImage image;
    private static BufferedImage blankImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    public static final String BLANK_IMAGE = "__blank";

    public Image() {
        this(new File(BLANK_IMAGE));
    }

    public Image(File file) {
        if (file == null || file.getPath().equals(BLANK_IMAGE)) {
            image = blankImage;
        } else {
            image = imageCache.get(file.getAbsolutePath());
            if (image == null) {
                try {
                    image = ImageIO.read(file);
                    imageCache.put(file.getAbsolutePath(), image);
                } catch (IOException e) {
                    throw new RuntimeErrorException(null, "Could not read image " + file);
                }
            }
        }
    }

    public Image(String fname) {
        this(new File(fname));
    }

    public Image(String fname, float cx, float cy) {
        this(new File(fname));
        this.x = cx;
        this.y = cy;
    }

    public Image(RenderedImage image) {
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

    //// Attribute access ////

    public float getOriginalWidth() {
        if (image == null) return 0;
        return image.getWidth();
    }

    public float getOriginalHeight() {
        if (image == null) return 0;
        return image.getHeight();
    }

    public float getWidth() {
        return getOriginalWidth() * getScaleFactor();
    }

    public void setWidth(float width) {
        this.desiredWidth = width;
    }

    public float getHeight() {
        return getOriginalHeight() * getScaleFactor();
    }

    public void setHeight(float height) {
        this.desiredHeight = height;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public RenderedImage getAwtImage() {
        return image;
    }

    public Size getSize() {
        return new Size(image.getWidth(), image.getHeight());
    }
    
    //// Transformations ////

    public Transform getCenteredTransform() {
        Rect bounds = getBounds();
        Transform t = new Transform();
        float dx = bounds.getX() + bounds.getWidth() / 2;
        float dy = bounds.getY() + bounds.getHeight() / 2;
        t.translate(dx, dy);
        t.append(getTransform());
        t.translate(-dx, -dy);
        return t;
    }

    protected void setupTransform(Graphics2D g) {
        saveTransform(g);
        AffineTransform trans = g.getTransform();
        trans.concatenate(getCenteredTransform().getAffineTransform());
        g.setTransform(trans);
    }

    //// Grob support ////

    public Rect getBounds() {
        if (image == null) return new Rect();
        float factor = getScaleFactor();
        float finalWidth = image.getWidth() * factor;
        float finalHeight = image.getHeight() * factor;
        return new Rect(x - finalWidth / 2, y - finalHeight / 2, finalWidth, finalHeight);
    }

    public float getScaleFactor() {
        if (desiredWidth != 0 || desiredHeight != 0) {
            float srcW = image.getWidth();
            float srcH = image.getHeight();
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

    public void inheritFromContext(GraphicsContext ctx) {
        // TODO: Implement
    }

    public void draw(Graphics2D g) {
        setupTransform(g);
        // You can only position an image using an affine transformation.
        // We use the transformation to translate the image to the specified
        // position, and scale it according to the given width and height.
        Transform imageTrans = new Transform();
        // Move to the image position. Convert x, y, which are centered coordinates,
        // to "real" coordinates. 
        float factor = getScaleFactor();
        float finalWidth = image.getWidth() * factor;
        float finalHeight = image.getHeight() * factor;
        imageTrans.translate(x - finalWidth / 2, y - finalHeight / 2);
        // Scaling only applies to image that have their desired width and/or height set.
        // However, getScaleFactor return 1 if height/width are not set, in effect negating
        // the effect of the scale.
        imageTrans.scale(getScaleFactor());
        float a = MathUtils.clamp(alpha);
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
