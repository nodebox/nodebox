package nodebox.client.visualizer;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public interface Visualizer {

    public boolean accepts(Iterable<?> objects, Class listClass);

    public Rectangle2D getBounds(Iterable<?> objects);

    public Point2D getOffset(Iterable<?> objects, Dimension2D viewerSize);

    public void draw(Graphics2D g, Iterable<?> objects);

}
