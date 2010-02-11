package nodebox.handle;

import nodebox.client.Viewer;
import nodebox.graphics.*;
import nodebox.node.Node;

import java.awt.event.KeyEvent;


public abstract class AbstractHandle implements Handle {

    public static final int HANDLE_SIZE = 6;
    public static final int HALF_HANDLE_SIZE = HANDLE_SIZE / 2;
    public static final Color HANDLE_COLOR = new Color(0.41, 0.39, 0.68);

    public static final int SHIFT_DOWN = KeyEvent.SHIFT_DOWN_MASK;
    public static final int CTRL_DOWN = KeyEvent.CTRL_DOWN_MASK;
    public static final int ALT_DOWN = KeyEvent.ALT_DOWN_MASK;
    public static final int META_DOWN = KeyEvent.META_DOWN_MASK;

    protected Node node;
    protected Viewer viewer;
    private boolean visible = true;

    protected AbstractHandle(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public Viewer getViewer() {
        return viewer;
    }

    public void setViewer(Viewer viewer) {
        this.viewer = viewer;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    //// Stub implementations of event handling ////

    public boolean mouseClicked(Point pt) {
        return false;
    }

    public boolean mousePressed(Point pt) {
        return false;
    }

    public boolean mouseReleased(Point pt) {
        return false;
    }

    public boolean mouseEntered(Point pt) {
        return false;
    }

    public boolean mouseExited(Point pt) {
        return false;
    }

    public boolean mouseDragged(Point pt) {
        return false;
    }

    public boolean mouseMoved(Point pt) {
        return false;
    }

    public boolean keyTyped(int keyCode, int modifiers) {
       return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        return false;
    }

    public boolean keyReleased(int keyCode, int modifiers) {
        return false;
    }

    //// Node events ////

    public void update() {
    }

    //// Utility methods ////

    protected void drawDot(GraphicsContext ctx, float x, float y) {
        Path p = new Path();
        p.setFillColor(HANDLE_COLOR);
        p.rect(x, y, HANDLE_SIZE, HANDLE_SIZE);
        ctx.draw(p);
    }

    protected void drawDot(Path p, float x, float y) {
        p.rect(x, y, HANDLE_SIZE, HANDLE_SIZE);
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
