package nodebox.handle;

import nodebox.graphics.*;
import nodebox.node.Node;

public abstract class AbstractHandle implements Handle {

    public static final int HANDLE_SIZE = 6;
    public static final int HALF_HANDLE_SIZE = HANDLE_SIZE / 2;
    public static final Color HANDLE_COLOR = new Color(0.41, 0.39, 0.68);

    protected Node node;

    protected AbstractHandle(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    //// Stub implementations of event handling ////

    public void mouseClicked(Point pt) {
    }

    public void mousePressed(Point pt) {

    }

    public void mouseReleased(Point pt) {

    }

    public void mouseEntered(Point pt) {

    }

    public void mouseExited(Point pt) {

    }

    public void mouseDragged(Point pt) {

    }

    public void mouseMoved(Point pt) {

    }

    //// Utility methods ////

    protected void drawDot(GraphicsContext ctx, double x, double y) {
        BezierPath p = new BezierPath();
        p.setFillColor(HANDLE_COLOR);
        p.rect((float) x, (float) y, HANDLE_SIZE, HANDLE_SIZE);
        ctx.draw(p);
    }

    protected void drawDot(BezierPath p, double x, double y) {
        p.rect((float) x, (float) y, HANDLE_SIZE, HANDLE_SIZE);
    }

    /**
     * Create a rectangle that can be used to test if a point is inside of it. (hit testing)
     * The X and Y coordinates form the center of a rectangle that represents the handle size.
     *
     * @param x the center x position of the rectangle
     * @param y the center y position of the rectangle
     * @return a rectangle the size of the handle.
     */
    protected Rect createHitRectangle(double x, double y) {
        int ix = (int) x;
        int iy = (int) y;
        return new Rect(ix - HALF_HANDLE_SIZE, iy - HALF_HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    }

}
