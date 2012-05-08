package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;

public class TranslateHandle extends AbstractHandle {

    public static final int HANDLE_LENGTH = 100;

    private enum DragState {
        NONE, CENTER, HORIZONTAL, VERTICAL
    }

    private String translateName;
    private double px, py;
    private double ox, oy;
    private float handleLength = HANDLE_LENGTH;
    private DragState dragState = DragState.NONE;

    public TranslateHandle() {
        this("translate");
    }

    public TranslateHandle(String translateName) {
        this.translateName = translateName;
        update();
    }

    @Override
    public void update() {
        setVisible(isConnected("shape"));
    }

    public void draw(GraphicsContext ctx) {
        Point cp = (Point) getValue(translateName);
        double x = cp.x;
        double y = cp.y;
        ctx.rectmode(GraphicsContext.RectMode.CENTER);
        Path p = new Path();
        p.setFillColor(HANDLE_COLOR);
        ctx.stroke(HANDLE_COLOR);
        p.setStrokeColor(null);
        ctx.nofill();
        drawDot(ctx, x, y);

        if (dragState == DragState.NONE) {
            // Horizontal and vertical direction lines.
            ctx.line(x, y, x + handleLength, y);
            ctx.line(x, y, x, y + handleLength);

            // Vertical arrow
            p.moveto(x, y + handleLength + 3);
            p.lineto(x - 5, y + handleLength - 3);
            p.lineto(x + 5, y + handleLength - 3);

            // Horizontal arrow
            p.moveto(x + handleLength + 3, y);
            p.lineto(x + handleLength - 3, y - 5);
            p.lineto(x + handleLength - 3, y + 5);
        } else if (dragState == DragState.CENTER) {
            ctx.line(px, py, x, y);
            drawDot(ctx, x, y);
        } else if (dragState == DragState.HORIZONTAL) {
            double x0, x1;
            ctx.line(px - handleLength, y, x + handleLength, y);
            if (x + handleLength > px - handleLength) {
                // arrow points right
                x0 = x + handleLength + 3;
                x1 = x + handleLength - 3;
            } else {
                // arrow points left
                x0 = x + handleLength - 3;
                x1 = x + handleLength + 3;
            }
            p.moveto(x0, y);
            p.lineto(x1, y - 5);
            p.lineto(x1, y + 5);
        } else if (dragState == DragState.VERTICAL) {
            double y0, y1;
            ctx.line(x, py - handleLength, x, y + handleLength);
            if (y + handleLength > py - handleLength) {
                // arrow points down
                y0 = y + handleLength + 3;
                y1 = y + handleLength - 3;
            } else {
                // arrow points up
                y0 = y + handleLength - 3;
                y1 = y + handleLength + 3;
            }
            p.moveto(x, y0);
            p.lineto(x - 5, y1);
            p.lineto(x + 5, y1);
        }
        ctx.nostroke();
        ctx.draw(p);
    }

    @Override
    public boolean mousePressed(Point pt) {
        px = pt.getX();
        py = pt.getY();

        Point cp = (Point) getValue(translateName);
        double x = ox = cp.x;
        double y = oy = cp.y;

        Rect centerRect = createHitRectangle(x, y);
        Rect horRect = createHitRectangle(x + handleLength, y);
        Rect vertRect = createHitRectangle(x, y + handleLength);

        if (centerRect.contains(pt))
            dragState = DragState.CENTER;
        else if (horRect.contains(pt))
            dragState = DragState.HORIZONTAL;
        else if (vertRect.contains(pt))
            dragState = DragState.VERTICAL;

        return (dragState != DragState.NONE);
    }

    @Override
    public boolean mouseDragged(Point pt) {
        if (dragState == DragState.NONE) return false;
        Point cp = (Point) getValue(translateName);
        double dx = pt.x - px;
        double dy = pt.y - py;
        if (dx == 0 && dy == 0) return false;
        startCombiningEdits("Set Value");
        if (dragState == DragState.CENTER) {
            silentSet(translateName, new Point(ox + dx, oy + dy));
        } else if (dragState == DragState.HORIZONTAL)
            silentSet(translateName, new Point(ox + dx, cp.y));
        else if (dragState == DragState.VERTICAL)
            silentSet(translateName, new Point(cp.x, oy + dy));
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        dragState = DragState.NONE;
        stopCombiningEdits();
        updateHandle();
        return true;
    }
}
