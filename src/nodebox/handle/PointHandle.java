package nodebox.handle;

import nodebox.graphics.Color;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.node.Node;

public class PointHandle extends AbstractHandle {

    public static final int HANDLE_SIZE = 6;
    public static final int HALF_HANDLE_SIZE = HANDLE_SIZE / 2;
    public static final Color HANDLE_COLOR = new Color(0.41, 0.39, 0.68);

    private String xName, yName;
    private boolean dragging;
    private double px, py;
    private double ox, oy;

    public PointHandle(Node node) {
        this(node, "x", "y");
    }

    public PointHandle(Node node, String xName, String yName) {
        super(node);
        this.xName = xName;
        this.yName = yName;
    }

    public void draw(GraphicsContext ctx) {
        double x = node.asFloat(xName);
        double y = node.asFloat(yName);
        drawDot(ctx, x, y);
    }

    @Override
    public void mousePressed(Point pt) {
        px = pt.getX();
        py = pt.getY();
        ox = node.asFloat(xName);
        oy = node.asFloat(yName);

        Rect hitRect = createHitRectangle(ox, oy);
        dragging = hitRect.contains(pt);
    }

    @Override
    public void mouseDragged(Point e) {
        if (!dragging) return;
        double x = e.getX();
        double y = e.getY();
        double dx = x - px;
        double dy = y - py;
        if (dx == 0 && dy == 0) return;
        // TODO: Temporary float fix to get a working compile. Doubles will be removed.
        node.setValue(xName, (float) (ox + dx));
        node.setValue(yName, (float) (oy + dy));
    }

    @Override
    public void mouseReleased(Point pt) {
        dragging = false;
    }
}
