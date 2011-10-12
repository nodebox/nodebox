package nodebox.handle;

import nodebox.graphics.*;
import nodebox.node.Node;

public class FourPointHandle extends AbstractHandle {

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
    }

    private String xName, yName, widthName, heightName;
    private DragState dragState = DragState.NONE;
    private float px, py;
    private float ocx, ocy, owidth, oheight;

    public FourPointHandle(Node node) {
        this(node, "x", "y", "width", "height");
    }

    public FourPointHandle(Node node, String xName, String yName, String widthName, String heightName) {
        super(node);
        this.xName = xName;
        this.yName = yName;
        this.widthName = widthName;
        this.heightName = heightName;
    }

    public void draw(GraphicsContext ctx) {
        float cx = node.asFloat(xName);
        float cy = node.asFloat(yName);
        float width = node.asFloat(widthName);
        float height = node.asFloat(heightName);
        float left = cx - width / 2;
        float right = cx + width / 2;
        float top = cy - height / 2;
        float bottom = cy + height / 2;
        Path cornerPath = new Path();
        cornerPath.setFillColor(HANDLE_COLOR);
        cornerPath.setStrokeWidth(0);
        drawDot(cornerPath, left, top);
        drawDot(cornerPath, right, top);
        drawDot(cornerPath, right, bottom);
        drawDot(cornerPath, left, bottom);
        drawDot(cornerPath, cx, cy);
        ctx.draw(cornerPath);
        Path strokePath = new Path();
        strokePath.setFillColor(null);
        strokePath.setStrokeColor(HANDLE_COLOR);
        strokePath.rect(cx, cy, width, height);
        ctx.draw(strokePath);
    }

    @Override
    public boolean mousePressed(Point pt) {
        px = pt.getX();
        py = pt.getY();

        ocx = node.asFloat(xName);
        ocy = node.asFloat(yName);
        owidth = node.asFloat(widthName);
        oheight = node.asFloat(heightName);

        float left = ocx - owidth / 2;
        float right = ocx + owidth / 2;
        float top = ocy - oheight / 2;
        float bottom = ocy + oheight / 2;

        Rect topLeft = createHitRectangle(left, top);
        Rect topRight = createHitRectangle(right, top);
        Rect bottomLeft = createHitRectangle(left, bottom);
        Rect bottomRight = createHitRectangle(right, bottom);
        Rect center = new Rect(left, top, owidth, oheight);

        if (topLeft.contains(pt)) {
            dragState = DragState.TOP_LEFT;
        } else if (topRight.contains(pt)) {
            dragState = DragState.TOP_RIGHT;
        } else if (bottomLeft.contains(pt)) {
            dragState = DragState.BOTTOM_LEFT;
        } else if (bottomRight.contains(pt)) {
            dragState = DragState.BOTTOM_RIGHT;
        } else if (center.contains(pt)) {
            dragState = DragState.CENTER;
        } else {
            dragState = DragState.NONE;
            return false;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Point pt) {
        if (dragState == DragState.NONE) return false;
        float x = pt.getX();
        float y = pt.getY();
        float dx = x - px;
        float dy = y - py;
        // The delta value is multiplied by 2 to create the float effect of moving
        // the top left corner down and the bottom left corner up (in the case of
        // the top left handle).
        if (dx == 0 && dy == 0) return false;
        switch (dragState) {
            case TOP_LEFT:
                silentSet(widthName, owidth - dx * 2);
                silentSet(heightName, oheight - dy * 2);
                break;
            case TOP_RIGHT:
                silentSet(heightName, oheight - dy * 2);
                silentSet(widthName, owidth + dx * 2);
                break;
            case BOTTOM_LEFT:
                silentSet(widthName, owidth - dx * 2);
                silentSet(heightName, oheight + dy * 2);
                break;
            case BOTTOM_RIGHT:
                silentSet(widthName, owidth + dx * 2);
                silentSet(heightName, oheight + dy * 2);
                break;
            case CENTER:
                silentSet(xName, ocx + dx);
                silentSet(yName, ocy + dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        dragState = DragState.NONE;
        return true;
    }

    public boolean hasDragState() {
        return dragState != DragState.NONE;
    }
}
