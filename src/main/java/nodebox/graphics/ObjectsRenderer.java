package nodebox.graphics;

import nodebox.client.Viewer;
import nodebox.client.visualizer.Visualizer;
import nodebox.client.visualizer.VisualizerFactory;
import nodebox.util.FileUtils;
import nodebox.util.ListUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ObjectsRenderer {

    public static void render(Iterable<?> objects, File file) {
        // TODO Remove reference to Viewer.getVisualizer.
        Visualizer v = VisualizerFactory.getVisualizer(objects, ListUtils.listClass(objects));
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            PDFRenderer.render(file, v, objects);
        } else {
            try {
                ImageIO.write(createImage(v, objects), FileUtils.getExtension(file), file);
            } catch (IOException e) {
                throw new RuntimeException("Could not write image file " + file, e);
            }
        }
    }

    public static BufferedImage createImage(Iterable<?> objects) {
        Visualizer v = VisualizerFactory.getVisualizer(objects, ListUtils.listClass(objects));
        return createImage(v, objects);
    }

    public static BufferedImage createMovieImage(Iterable<?> objects, int width, int height) {
        Visualizer v = VisualizerFactory.getVisualizer(objects, ListUtils.listClass(objects));
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.translate(width / 2, height / 2);
        v.draw(g, objects);
        img.flush();
        return img;
    }

    private static BufferedImage createImage(Visualizer visualizer, Iterable<?> objects) {
        Rectangle2D bounds = visualizer.getBounds(objects);
        BufferedImage img = new BufferedImage((int) Math.round(bounds.getWidth()), (int) Math.round(bounds.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(-bounds.getX(), -bounds.getY());
        visualizer.draw(g, objects);
        img.flush();
        return img;
    }

}
