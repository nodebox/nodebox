package net.nodebox.graphics;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import java.awt.*;
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

    public Image(File file) {
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

    public Image(String fname) {
        this(new File(fname));
    }

    public Image(RenderedImage image) {
        this.image = image;
    }

    public Image(Image image) {
        this.x = image.x;
        this.y = image.y;
        this.desiredWidth = image.desiredWidth;
        this.desiredHeight = image.desiredHeight;
        this.alpha = image.alpha;
        this.setTransform(image.getTransform().clone());
        this.image = image.image;
    }

    //// Attribute access ////

    // todo: native width and desired width are incosistently presented.

    public float getWidth() {
        if (image == null) return 0;
        return image.getWidth();
    }

    public float getHeight() {
        if (image == null) return 0;
        return image.getHeight();
    }

    public void setWidth(float width) {
        this.desiredWidth = width;
    }

    public void setHeight(float height) {
        this.desiredHeight = height;
    }

    public double getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public RenderedImage getAwtImage() {
        return image;
    }

    //// Grob support ////

    public Rect getBounds() {
        if (image == null) return new Rect();
        return new Rect(x, y, image.getWidth(), image.getHeight());
    }

    public void draw(Graphics2D g) {
        float srcW = image.getWidth();
        float srcH = image.getHeight();
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


}
