package nodebox.client;

import nodebox.client.visualizer.Visualizer;
import nodebox.client.visualizer.VisualizerFactory;
import nodebox.graphics.CSVRenderer;
import nodebox.graphics.Drawable;
import nodebox.graphics.PDFRenderer;
import nodebox.graphics.SVGRenderer;
import nodebox.util.FileUtils;
import nodebox.util.ListUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ObjectsRenderer {

    public static void render(Iterable<?> objects, Rectangle2D bounds, File file, Map<String,?> options) {
        // TODO Remove reference to Viewer.getVisualizer.
        Visualizer v = VisualizerFactory.getVisualizer(objects, ListUtils.listClass(objects));
        if (file.getName().toLowerCase(Locale.US).endsWith(".pdf")) {
            LinkedVisualizer linkedVisualizer = new LinkedVisualizer(v, objects);
            PDFRenderer.render(linkedVisualizer, bounds, file);
        } else if (file.getName().toLowerCase(Locale.US).endsWith(".svg")) {
            SVGRenderer.renderToFile(objects, bounds, file);
        } else if (file.getName().toLowerCase(Locale.US).endsWith(".csv")) {
            char delimiter = ';';
            if (options.containsKey("delimiter")) {
                delimiter = (Character) options.get("delimiter");
            }
            boolean quotes = true;
            if (options.containsKey("quotes")) {
                quotes = (Boolean) options.get("quotes");
            }
            CSVRenderer.renderToFile(objects, file, delimiter, quotes);
        } else {
            try {
                ImageIO.write(createImage(objects, v, bounds, null), FileUtils.getExtension(file), file);
            } catch (IOException e) {
                throw new RuntimeException("Could not write image file " + file, e);
            }
        }
    }

    public static BufferedImage createMovieImage(Iterable<?> objects, Rectangle2D bounds) {
        Visualizer v = VisualizerFactory.getVisualizer(objects, ListUtils.listClass(objects));
        return createImage(objects, v, bounds, Color.WHITE);
    }

    private static BufferedImage createImage(Iterable<?> objects, Visualizer visualizer, Rectangle2D bounds, Color backgroundColor) {
        final int width = (int) Math.round(bounds.getWidth());
        final int height = (int) Math.round(bounds.getHeight());
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, width, height);
        }
        g.translate(-bounds.getX(), -bounds.getY());
        visualizer.draw(g, objects);
        img.flush();
        return img;
    }

    /**
     * A visualizer linked to its objects.
     */
    private static class LinkedVisualizer implements Drawable {
        private Visualizer visualizer;
        private Iterable<?> objects;

        private LinkedVisualizer(Visualizer visualizer, Iterable<?> objects) {
            this.visualizer = visualizer;
            this.objects = objects;
        }

        @Override
        public void draw(Graphics2D g) {
            visualizer.draw(g, objects);
        }
    }

}
