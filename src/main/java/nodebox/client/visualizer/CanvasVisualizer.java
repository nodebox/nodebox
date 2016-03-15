package nodebox.client.visualizer;

import com.google.common.collect.Iterables;
import nodebox.graphics.Canvas;
import nodebox.graphics.Grob;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static com.google.common.base.Preconditions.checkArgument;

public class CanvasVisualizer implements Visualizer {

    public static final CanvasVisualizer INSTANCE = new CanvasVisualizer();
    private Canvas firstCanvas;

    private CanvasVisualizer() {
    }

    public boolean accepts(Iterable<?> objects, Class listClass) {
        return nodebox.graphics.Canvas.class.isAssignableFrom(listClass);
    }

    public Rectangle2D getBounds(Iterable<?> objects) {
        Rectangle2D r = new Rectangle2D.Double();
        for (Object o : objects) {
            if (o instanceof Grob) {
                r.add(((Grob) o).getBounds().getRectangle2D());
            }
        }
        return r;
    }

    public Point2D getOffset(Iterable<?> objects, Dimension2D size) {
        return new Point2D.Double(size.getWidth() / 2, size.getHeight() / 2);
    }

    @SuppressWarnings("unchecked")
    public void draw(Graphics2D g, Iterable<?> objects) {
        firstCanvas = getFirstCanvas(objects);
        drawCanvasBounds(g, firstCanvas);
        GrobVisualizer.drawGrobs(g, (Iterable<Grob>) objects);
    }

    private static void drawCanvasBounds(Graphics2D g, Canvas canvas) {
        Rectangle2D canvasBounds = canvas.getBounds().getRectangle2D();
        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(1f));
        g.draw(canvasBounds);
    }

    private static Canvas getFirstCanvas(Iterable<?> objects) {
        Canvas c = (Canvas) Iterables.getFirst(objects, null);
        return c != null ? c : new Canvas();
    }

}
