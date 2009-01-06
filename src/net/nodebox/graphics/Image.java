package net.nodebox.graphics;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Image extends Grob {

    private static HashMap<String, RenderedImage> imageCache = new HashMap<String, RenderedImage>();

    private double x, y;
    private double desiredWidth, desiredHeight;
    private double alpha = 1.0F;

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

    // todo: native width and desired width are incosistently presented.

    public double getWidth() {
        if (image == null) return 0;
        return image.getWidth();
    }

    public double getHeight() {
        if (image == null) return 0;
        return image.getHeight();
    }

    public void setWidth(double width) {
        this.desiredWidth = width;
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

    public RenderedImage getAwtImage() {
        return image;
    }

    public Size getSize() {
        return new Size(image.getWidth(), image.getHeight());
    }

    //// Grob support ////

    public Rect getBounds() {
        if (image == null) return new Rect();
        return new Rect(x, y, image.getWidth(), image.getHeight());
    }

    public void draw(Graphics2D g) {
        double srcW = image.getWidth();
        double srcH = image.getHeight();
        // Width or height given
        if (desiredWidth != 0 || desiredHeight != 0) {
            double factor;
            if (desiredWidth != 0 && desiredHeight != 0) {
                // Both width and height were given, constrain to smallest
                factor = Math.min(desiredWidth / srcW, desiredHeight / srcH);
            } else if (desiredWidth != 0) {
                factor = desiredWidth / srcW;
            } else {
                factor = desiredHeight / srcH;
            }

            Transform imageTrans = new Transform(g.getTransform());

            // Do current transformation.
            imageTrans.append(getTransform());

            // Scale the image according to the factors.
            imageTrans.translate(x, y);  // Here we add the positioning of the image.
            imageTrans.scale(factor);

            // Draw the actual image
            g.drawRenderedImage(image, imageTrans.getAffineTransform());

            // No width or height given
        } else {

            Transform imageTrans = new Transform(g.getTransform());

            // Do current transformation.
            imageTrans.append(getTransform());
            imageTrans.translate(x, y);

            // Draw the actual image
            g.drawRenderedImage(image, imageTrans.getAffineTransform());
        }
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

}
