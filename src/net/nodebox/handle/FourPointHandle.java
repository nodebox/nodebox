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
    public static final Color HANDLE_COLOR = new Color(0.41, 0.39, 0.68);

    private String xName, yName, widthName, heightName;
    private DragState dragState = DragState.NONE;
    private int px, py;
    private double ox, oy, owidth, oheight;

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

    private Rectangle createRectangle(double x, double y) {
        int ix = (int) x;
        int iy = (int) y;
        return new Rectangle(ix - HALF_HANDLE_SIZE, iy - HALF_HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    }

    private void drawDot(BezierPath p, double x, double y) {
        p.addRect(x - HALF_HANDLE_SIZE, y - HALF_HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
    }

    public void draw(GraphicsContext ctx) {
        double x = node.asFloat(xName);
        double y = node.asFloat(yName);
        double width = node.asFloat(widthName);
        double height = node.asFloat(heightName);
        BezierPath cornerPath = new BezierPath();
        cornerPath.setFillColor(HANDLE_COLOR);
        cornerPath.setStrokeWidth(0.0);
        drawDot(cornerPath, x, y);
        drawDot(cornerPath, x + width, y);
        drawDot(cornerPath, x + width, y + height);
        drawDot(cornerPath, x, y + height);
        ctx.getCanvas().add(cornerPath);
        BezierPath strokePath = new BezierPath();
        strokePath.setFillColor(null);
        strokePath.setStrokeColor(HANDLE_COLOR);
        strokePath.addRect(x, y, width, height);
        ctx.getCanvas().add(strokePath);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point pt = e.getPoint();
        px = e.getX();
        py = e.getY();

        ox = node.asFloat(xName);
        oy = node.asFloat(yName);
        owidth = node.asFloat(widthName);
        oheight = node.asFloat(heightName);

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
                node.set(xName, ox + dx);
                node.set(yName, oy + dy);
                node.set(widthName, owidth - dx);
                node.set(heightName, oheight - dy);
                break;
            case TOP_RIGHT:
                node.set(yName, oy + dy);
                node.set(heightName, oheight - dy);
                node.set(widthName, owidth + dx);
                break;
            case BOTTOM_LEFT:
                node.set(xName, ox + dx);
                node.set(widthName, owidth - dx);
                node.set(heightName, oheight + dy);
                break;
            case BOTTOM_RIGHT:
                node.set(widthName, owidth + dx);
                node.set(heightName, oheight + dy);
                break;
            case CENTER:
                node.set(xName, ox + dx);
                node.set(yName, oy + dy);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragState = DragState.NONE;
    }
}
