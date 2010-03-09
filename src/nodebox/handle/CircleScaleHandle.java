package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.node.Node;
import nodebox.util.Geometry;

public class CircleScaleHandle extends AbstractHandle {

    public enum Mode {
        RADIUS, DIAMETER
    }

    private boolean dragging = false;
    private String radiusName;
    private String xName = null;
    private String yName = null;
    private Mode mode;
    private Point pt = null;

    public CircleScaleHandle(Node node) {
        this(node, "radius", Mode.RADIUS);
    }

    public CircleScaleHandle(Node node, String radiusName) {
        this(node, radiusName, Mode.RADIUS);
    }

    public CircleScaleHandle(Node node, Mode mode) {
        this(node, "radius", mode);
    }

    public CircleScaleHandle(Node node, String radiusName, Mode mode) {
        this(node, radiusName, mode, null, null);
    }

    public CircleScaleHandle(Node node, String radiusName, Mode mode, String xName, String yName) {
        super(node);
        this.radiusName = radiusName;
        this.mode = mode;
        this.xName = xName;
        this.yName = yName;
    }

    private float getCenterX() {
        if (xName != null)
            return node.asFloat(xName);
        else
            return 0;
    }

    private float getCenterY() {
        if (yName != null)
            return node.asFloat(yName);
        else
            return 0;
    }

    private float getRadius() {
        float val = node.asFloat(radiusName);
        if (mode == Mode.DIAMETER)
            return Math.abs(val / 2);
        return Math.abs(val);
    }

    public void draw(GraphicsContext ctx) {
        float x = getCenterX();
        float y = getCenterY();
        float radius = getRadius();
        ctx.setFillColor(null);
        ctx.setStrokeColor(HANDLE_COLOR);
        ctx.ellipse(x, y, radius * 2, radius * 2);
        if (pt != null)
            drawDot(ctx, pt.x, pt.y);
    }

    @Override
    public boolean mousePressed(Point pt) {
        this.pt = null;
        float radius = getRadius();
        float d = (float) Geometry.distance(getCenterX(), getCenterY(), pt.x, pt.y);
        dragging = (radius - 4 <= d && d <= radius + 4);
        return dragging;
    }

    @Override
    public boolean mouseDragged(Point pt) {
        if (!dragging) return false;
        float newSize = (float) Geometry.distance(getCenterX(), getCenterY(), pt.x, pt.y);
        if (mode == Mode.DIAMETER)
            newSize *= 2;
        if (newSize == getRadius()) return false;
        node.silentSet(radiusName, newSize);
        this.pt = pt;
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (!dragging) return false;
        dragging = false;
        this.pt = null;
        viewer.repaint();
        return true;
    }

    @Override
    public boolean mouseMoved(Point pt) {
        float x = getCenterX();
        float y = getCenterY();
        float radius = getRadius();
        float d = (float) Geometry.distance(x, y, pt.x, pt.y);
        if (radius - 4 <= d && d <= radius + 4) {
            float a = (float) Geometry.angle(x, y, pt.x, pt.y);
            double[] xy;
            xy = Geometry.coordinates(x, y, radius, a);
            this.pt = new Point((float) xy[0], (float) xy[1]);
        } else {
            this.pt = null;
        }
        viewer.repaint();
        return true;
    }
}
