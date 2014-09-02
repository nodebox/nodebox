package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;

public class FourPointHandle extends AbstractHandle {

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
    }

    private String positionName, widthName, heightName;
    private DragState dragState = DragState.NONE;
    private double px, py;
    private double ocx, ocy, owidth, oheight;

    public FourPointHandle() {
        this("position", "width", "height");
    }

    public FourPointHandle(String positionName, String widthName, String heightName) {
        this.positionName = positionName;
        this.widthName = widthName;
        this.heightName = heightName;
        update();
    }

    @Override
    public void update() {
        if (hasInput("shape"))
            setVisible(isConnected("shape"));
    }

    public void draw(GraphicsContext ctx) {
        Point cp = (Point) getValue(positionName);
        if (cp == null) {
            return;
        }
        double cx = cp.x;
        double cy = cp.y;
        double width = (Double) getValue(widthName);
        double height = (Double) getValue(heightName);
        double left = cx - width / 2;
        double right = cx + width / 2;
        double top = cy - height / 2;
        double bottom = cy + height / 2;
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

        Point op = (Point) getValue(positionName);
        ocx = op.x;
        ocy = op.y;
        owidth = (Double) getValue(widthName);
        oheight = (Double) getValue(heightName);

        double left = ocx - owidth / 2;
        double right = ocx + owidth / 2;
        double top = ocy - oheight / 2;
        double bottom = ocy + oheight / 2;

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
        double x = pt.getX();
        double y = pt.getY();
        double dx = x - px;
        double dy = y - py;
        // The delta value is multiplied by 2 to create the float effect of moving
        // the top left corner down and the bottom left corner up (in the case of
        // the top left handle).
        if (dx == 0 && dy == 0) return false;
        startCombiningEdits("Set Value");
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
                silentSet(positionName, new Point(ocx + dx, ocy + dy));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Point pt) {
        if (dragState == DragState.NONE) return false;
        dragState = DragState.NONE;
        stopCombiningEdits();
        return true;
    }

    public boolean hasDragState() {
        return dragState != DragState.NONE;
    }
}
