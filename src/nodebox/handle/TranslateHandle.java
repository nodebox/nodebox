package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.node.Node;

public class TranslateHandle extends AbstractHandle {

    public static final int HANDLE_LENGTH = 100;

    private enum DragState {
        NONE, CENTER, HORIZONTAL, VERTICAL
    }

    private String txName, tyName;
    private float px, py;
    private float ox, oy;
    private float handleLength = HANDLE_LENGTH;
    private DragState dragState = DragState.NONE;

    public TranslateHandle(Node node) {
        this(node, "tx", "ty");
    }

    public TranslateHandle(Node node, String txName, String tyName) {
        super(node);
        this.txName = txName;
        this.tyName = tyName;
    }

    public void draw(GraphicsContext ctx) {
        float x = node.asFloat(txName);
        float y = node.asFloat(tyName);
        Path p = new Path();
        p.setFillColor(HANDLE_COLOR);
        ctx.setStrokeColor(HANDLE_COLOR);
        p.setStrokeColor(null);
        ctx.setFillColor(null);
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
            float x0, x1;
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
            float y0, y1;
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
        ctx.setStrokeColor(null);
        ctx.draw(p);
    }

    @Override
    public boolean mousePressed(Point pt) {
        px = pt.getX();
        py = pt.getY();

        float x = ox = node.asFloat(txName);
        float y = oy = node.asFloat(tyName);

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
        float dx = pt.x - px;
        float dy = pt.y - py;
        if (dx == 0 && dy == 0) return false;
        if (dragState == DragState.CENTER) {
            node.silentSet(txName, ox + dx);
            node.silentSet(tyName, oy + dy);
        } else if (dragState == DragState.HORIZONTAL)
            node.silentSet(txName, ox + dx);
        else if (dragState == DragState.VERTICAL)
            node.silentSet(tyName, oy + dy);
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        dragState = DragState.NONE;
        viewer.repaint();
        return true;
    }
}
