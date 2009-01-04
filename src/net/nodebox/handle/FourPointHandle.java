package net.nodebox.handle;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Color;
import net.nodebox.graphics.GraphicsContext;
import net.nodebox.node.Node;

import java.awt.*;
import java.awt.event.MouseEvent;

public class FourPointHandle extends AbstractHandle {

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
    }

    public static final int HANDLE_SIZE = 6;
    public static final int HALF_HANDLE_SIZE = HANDLE_SIZE / 2;
    public static final Color HANDLE_COLOR = new Color(1, 0, 0);

    private DragState dragState = DragState.NONE;
    private int px, py;
    private double ox, oy, owidth, oheight;

    private Rectangle topLeft, topRight, bottomLeft, bottomRight, center;

    public FourPointHandle(Node node) {
        super(node);
    }

    private Rectangle createRectangle(double x, double y) {
        int ix = (int) x;
        int iy = (int) y;
        return new Rectangle(ix - HALF_HANDLE_SIZE, iy - HALF_HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    }

    private void drawDot(BezierPath p, double x, double y) {
        p.addRect(x - HALF_HANDLE_SIZE, y - HALF_HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    }

    public void draw(GraphicsContext ctx) {
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        double width = node.asFloat("width");
        double height = node.asFloat("height");
        BezierPath p = new BezierPath();
        p.setFillColor(HANDLE_COLOR);
        drawDot(p, x, y);
        drawDot(p, x + width, y);
        drawDot(p, x + width, y + height);
        drawDot(p, x, y + height);
        ctx.getCanvas().add(p);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point pt = e.getPoint();
        px = e.getX();
        py = e.getY();

        ox = node.asFloat("x");
        oy = node.asFloat("y");
        owidth = node.asFloat("width");
        oheight = node.asFloat("height");

        Rectangle topLeft = createRectangle(ox, oy);
        Rectangle topRight = createRectangle(ox + owidth, oy);
        Rectangle bottomLeft = createRectangle(ox, oy + oheight);
        Rectangle bottomRight = createRectangle(ox + owidth, oy + oheight);
        Rectangle center = new Rectangle((int) ox, (int) oy, (int) owidth, (int) oheight);

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
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragState == DragState.NONE) return;
        int x = e.getX();
        int y = e.getY();
        int dx = x - px;
        int dy = y - py;
        if (dx == 0 && dy == 0) return;
        switch (dragState) {
            case TOP_LEFT:
                node.set("x", ox + dx);
                node.set("y", oy + dy);
                node.set("width", owidth - dx);
                node.set("height", oheight - dy);
                break;
            case TOP_RIGHT:
                node.set("y", oy + dy);
                node.set("height", oheight - dy);
                node.set("width", owidth + dx);
                break;
            case BOTTOM_LEFT:
                node.set("x", ox + dx);
                node.set("width", owidth - dx);
                node.set("height", oheight + dy);
                break;
            case BOTTOM_RIGHT:
                node.set("width", owidth + dx);
                node.set("height", oheight + dy);
                break;
            case CENTER:
                node.set("x", ox + dx);
                node.set("y", oy + dy);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragState = DragState.NONE;
    }
}
