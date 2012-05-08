package nodebox.handle;

import nodebox.graphics.Color;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;

public class PointHandle extends AbstractHandle {

    public static final int HANDLE_SIZE = 6;
    public static final int HALF_HANDLE_SIZE = HANDLE_SIZE / 2;
    public static final Color HANDLE_COLOR = new Color(0.41, 0.39, 0.68);

    private String positionName;
    private boolean dragging;
    private double px, py;
    private double ox, oy;

    public PointHandle() {
        this("position");
    }

    public PointHandle(String positionName) {
        this.positionName = positionName;
        update();
    }

    @Override
    public void update() {
        if (hasInput("shape"))
            setVisible(isConnected("shape"));
    }


    public void draw(GraphicsContext ctx) {
        Point pt = (Point) getValue(positionName);
        drawDot(ctx, (float) pt.x, (float) pt.y);
    }

    @Override
    public boolean mousePressed(Point pt) {
        px = pt.getX();
        py = pt.getY();
        Point op = (Point) getValue(positionName);
        ox = op.x;
        oy = op.y;

        Rect hitRect = createHitRectangle(ox, oy);
        dragging = hitRect.contains(pt);
        return dragging;
    }

    @Override
    public boolean mouseDragged(Point e) {
        if (!dragging) return false;
        double x = e.getX();
        double y = e.getY();
        double dx = x - px;
        double dy = y - py;
        if (dx == 0 && dy == 0) return false;
        startCombiningEdits("Set Value");
        // TODO: Temporary float fix to get a working compile. Doubles will be removed.
        silentSet(positionName, new Point((float) (ox + dx), (float) (oy + dy)));
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (!dragging) return false;
        dragging = false;
        stopCombiningEdits();
        return true;
    }
}
