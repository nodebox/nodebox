package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;

public class ScaleHandle extends AbstractHandle {
    public static final int HANDLE_WIDTH = 100;
    public static final int HANDLE_HEIGHT = 100;

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private String scaleName;
    private double px, py;
    private double ox, oy;
    private double handleWidth = HANDLE_WIDTH;
    private double handleHeight = HANDLE_HEIGHT;
    private boolean scaleHorizontal = true;
    private boolean scaleVertical = true;
    private DragState dragState = DragState.NONE;

    public ScaleHandle() {
        this("scale");
    }

    public ScaleHandle(String scaleName) {
        this.scaleName = scaleName;
        update();
    }

    @Override
    public void update() {
        setVisible(isConnected("shape"));
    }

    public void draw(GraphicsContext ctx) {
        ctx.nofill();
        ctx.stroke(HANDLE_COLOR);
        double halfWidth = handleWidth / 2;
        double halfHeight = handleHeight / 2;
        ctx.rectmode(GraphicsContext.RectMode.CENTER);
        ctx.rect(0, 0, handleWidth, handleHeight);
        drawDot(ctx, -halfWidth, -halfHeight);
        drawDot(ctx, halfWidth, -halfHeight);
        drawDot(ctx, -halfWidth, halfHeight);
        drawDot(ctx, halfWidth, halfHeight);
    }

    @Override
    public boolean mousePressed(Point pt) {
        double left = -handleWidth / 2;
        double right = handleWidth / 2;
        double top = -handleHeight / 2;
        double bottom = handleHeight / 2;
        Rect topLeft = createHitRectangle(left, top);
        Rect topRight = createHitRectangle(right, top);
        Rect bottomLeft = createHitRectangle(left, bottom);
        Rect bottomRight = createHitRectangle(right, bottom);
        px = pt.getX();
        py = pt.getY();
        Point op = (Point) getValue(scaleName);
        ox = op.x;
        oy = op.y;
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
        double x = pt.getX();
        double y = pt.getY();
        double dx = x - px;
        double dy = y - py;
        if (dx == 0 && dy == 0) return false;
        startCombiningEdits("Set Value");
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
        double pctX = handleWidth / HANDLE_WIDTH;
        double pctY = handleHeight / HANDLE_HEIGHT;
        Point op = (Point) getValue(scaleName);
        if (scaleHorizontal)
            op = new Point(ox * pctX, op.y);
        else
            handleWidth = HANDLE_WIDTH;
        if (scaleVertical)
            op = new Point(op.x, oy * pctY);
        else
            handleHeight = HANDLE_HEIGHT;
        silentSet(scaleName, op);
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        handleWidth = HANDLE_WIDTH;
        handleHeight = HANDLE_HEIGHT;
        dragState = DragState.NONE;
        stopCombiningEdits();
        updateHandle();
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
