package net.nodebox.handle;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.GraphicsContext;
import net.nodebox.node.Node;

import java.awt.*;
import java.awt.event.MouseEvent;

public class FourPointHandle extends AbstractHandle {

    private enum DragState {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
    }

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
        strokePath.rect(x, y, width, height);
        ctx.draw(strokePath);
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

        Rectangle topLeft = createHitRectangle(ox, oy);
        Rectangle topRight = createHitRectangle(ox + owidth, oy);
        Rectangle bottomLeft = createHitRectangle(ox, oy + oheight);
        Rectangle bottomRight = createHitRectangle(ox + owidth, oy + oheight);
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
                node.silentSet(xName, ox + dx);
                node.silentSet(yName, oy + dy);
                node.silentSet(widthName, owidth - dx);
                node.silentSet(heightName, oheight - dy);
                break;
            case TOP_RIGHT:
                node.silentSet(yName, oy + dy);
                node.silentSet(heightName, oheight - dy);
                node.silentSet(widthName, owidth + dx);
                break;
            case BOTTOM_LEFT:
                node.silentSet(xName, ox + dx);
                node.silentSet(widthName, owidth - dx);
                node.silentSet(heightName, oheight + dy);
                break;
            case BOTTOM_RIGHT:
                node.silentSet(widthName, owidth + dx);
                node.silentSet(heightName, oheight + dy);
                break;
            case CENTER:
                node.silentSet(xName, ox + dx);
                node.silentSet(yName, oy + dy);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragState = DragState.NONE;
    }
}
