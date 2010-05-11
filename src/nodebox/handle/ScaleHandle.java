package nodebox.handle;

import nodebox.graphics.CanvasContext;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.node.Node;

public class ScaleHandle extends AbstractHandle {
    public static final int HANDLE_WIDTH = 100;
    public static final int HANDLE_HEIGHT = 100;

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private String sxName, syName;
    private float px, py;
    private float ox, oy;
    private float handleWidth = HANDLE_WIDTH;
    private float handleHeight = HANDLE_HEIGHT;
    private boolean scaleHorizontal = true;
    private boolean scaleVertical = true;
    private DragState dragState = DragState.NONE;

    public ScaleHandle(Node node) {
        this(node, "sx", "sy");
    }

    public ScaleHandle(Node node, String sxName, String syName) {
        super(node);
        this.sxName = sxName;
        this.syName = syName;
    }

    public void draw(GraphicsContext ctx) {
        ctx.nofill();
        ctx.stroke(HANDLE_COLOR);
        float halfWidth = handleWidth / 2;
        float halfHeight = handleHeight / 2;
        ctx.rectmode(GraphicsContext.RectMode.CENTER);
        ctx.rect(0, 0, handleWidth, handleHeight);
        drawDot(ctx, -halfWidth, -halfHeight);
        drawDot(ctx, halfWidth, -halfHeight);
        drawDot(ctx, -halfWidth, halfHeight);
        drawDot(ctx, halfWidth, halfHeight);
    }

    @Override
    public boolean mousePressed(Point pt) {
        float left = -handleWidth / 2;
        float right = handleWidth / 2;
        float top = -handleHeight / 2;
        float bottom = handleHeight / 2;
        Rect topLeft = createHitRectangle(left, top);
        Rect topRight = createHitRectangle(right, top);
        Rect bottomLeft = createHitRectangle(left, bottom);
        Rect bottomRight = createHitRectangle(right, bottom);
        px = pt.getX();
        py = pt.getY();
        ox = node.asFloat(sxName);
        oy = node.asFloat(syName);
        if (topLeft.contains(pt))
            dragState = DragState.TOP_LEFT;
        else if (topRight.contains(pt))
            dragState = DragState.TOP_RIGHT;
        else if (bottomLeft.contains(pt))
            dragState = DragState.BOTTOM_LEFT;
        else if (bottomRight.contains(pt))
            dragState = DragState.BOTTOM_RIGHT;
        else
            dragState = DragState.NONE;
        return (dragState != DragState.NONE);
    }

    @Override
    public boolean mouseDragged(Point pt) {
        if (dragState == DragState.NONE) return false;
        float x = pt.getX();
        float y = pt.getY();
        float dx = x - px;
        float dy = y - py;
        if (dx == 0 && dy == 0) return false;
        if (dragState == DragState.TOP_LEFT) {
            handleWidth = HANDLE_WIDTH - dx * 2;
            handleHeight = HANDLE_HEIGHT - dy * 2;
        } else if (dragState == DragState.TOP_RIGHT) {
            handleWidth = HANDLE_WIDTH + dx * 2;
            handleHeight = HANDLE_HEIGHT - dy * 2;
        } else if (dragState == DragState.BOTTOM_LEFT) {
            handleWidth = HANDLE_WIDTH - dx * 2;
            handleHeight = HANDLE_HEIGHT + dy * 2;
        } else if (dragState == DragState.BOTTOM_RIGHT) {
            handleWidth = HANDLE_WIDTH + dx * 2;
            handleHeight = HANDLE_HEIGHT + dy * 2;
        }
        float pctX = handleWidth / HANDLE_WIDTH;
        float pctY = handleHeight / HANDLE_HEIGHT;
        if (scaleHorizontal)
            node.silentSet(sxName, ox * pctX);
        else
            handleWidth = HANDLE_WIDTH;
        if (scaleVertical)
            node.silentSet(syName, oy * pctY);
        else
            handleHeight = HANDLE_HEIGHT;
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        handleWidth = HANDLE_WIDTH;
        handleHeight = HANDLE_HEIGHT;
        dragState = DragState.NONE;
        viewer.repaint();
        return true;
    }


    @Override
    public boolean keyPressed(int keyCode, int modifiers) {
        if ((modifiers & SHIFT_DOWN) != 0) scaleVertical = false;
        else if ((modifiers & META_DOWN) != 0) scaleHorizontal = false;
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int modifiers) {
        scaleHorizontal = true;
        scaleVertical = true;
        return false;
    }
}
