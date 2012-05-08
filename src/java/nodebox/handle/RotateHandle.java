package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.util.Geometry;

public class RotateHandle extends AbstractHandle {
    public static final int HANDLE_LENGTH = 50;

    private enum DragState {
        NONE, HANDLE, CIRCLE
    }

    private String angleName, positionName;
    private double pa, ca, oa;
    private float handleLength = HANDLE_LENGTH;
    private DragState dragState = DragState.NONE;

    public RotateHandle() {
        this("angle");
    }

    public RotateHandle(String angleName) {
        this(angleName, null);
    }

    public RotateHandle(String angleName, String positionName) {
        this.angleName = angleName;
        this.positionName = positionName;
        update();
    }

    @Override
    public void update() {
        setVisible(isConnected("shape"));
    }

    private Point getCenter() {
        if (positionName != null)
            return (Point) getValue(positionName);
        else
            return Point.ZERO;
    }

    public void draw(GraphicsContext ctx) {
        Point c = getCenter();
        double cx = c.x;
        double cy = c.y;
        ctx.ellipsemode(GraphicsContext.EllipseMode.CENTER);
        ctx.nofill();
        ctx.stroke(HANDLE_COLOR);
        ctx.ellipse(cx, cy, handleLength * 2, handleLength * 2);
        double[] xy;
        if (dragState == DragState.NONE || dragState == DragState.HANDLE)
            xy = Geometry.coordinates(cx, cy, handleLength, (Double) getValue(angleName));
        else {
            xy = Geometry.coordinates(cx, cy, handleLength, pa);
            ctx.line(cx, cy, (float) xy[0], (float) xy[1]);
            xy = Geometry.coordinates(cx, cy, handleLength, ca);
        }
        float x = (float) xy[0];
        float y = (float) xy[1];
        ctx.line(cx, cy, x, y);
        ctx.fill(1);
        ctx.ellipse(x, y, 6, 6);
        if (dragState == DragState.HANDLE) {
            xy = Geometry.coordinates(cx, cy, handleLength, oa);
            ctx.line(cx, cy, (float) xy[0], (float) xy[1]);
        }
    }

    @Override
    public boolean mousePressed(Point pt) {
        Point c = getCenter();
        double cx = c.x;
        double cy = c.y;
        // original angle
        oa = (Double) getValue(angleName);
        double[] xy = Geometry.coordinates(cx, cy, handleLength, oa);
        float x = (float) xy[0];
        float y = (float) xy[1];
        Path p = new Path();
        p.ellipse(cx, cy, handleLength * 2, handleLength * 2);
        Rect handleRect = createHitRectangle(x, y);
        float a = (float) Geometry.angle(cx, cy, pt.x, pt.y);
        xy = Geometry.coordinates(cx, cy, handleLength, a);
        float x1 = (float) xy[0];
        float y1 = (float) xy[1];
        Rect circleRect = createHitRectangle(x1, y1);
        if (handleRect.contains(pt))
            dragState = DragState.HANDLE;
        else if (circleRect.contains(pt)) {
            pa = a; // pressed angle
            dragState = DragState.CIRCLE;
        } else
            dragState = DragState.NONE;
        return (dragState != DragState.NONE);
    }

    @Override
    public boolean mouseDragged(Point pt) {
        if (dragState == DragState.NONE) return false;
        Point c = getCenter();
        double cx = c.x;
        double cy = c.y;
        float a = (float) Geometry.angle(cx, cy, pt.x, pt.y);
        ca = a; // current angle
        handleLength = (float) Geometry.distance(cx, cy, pt.x, pt.y);
        if (dragState == DragState.HANDLE)
            silentSet(angleName, a);
        else if (dragState == DragState.CIRCLE)
            silentSet(angleName, oa + a - pa);
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        dragState = DragState.NONE;
        handleLength = HANDLE_LENGTH;
        updateHandle();
        return true;
    }
}
