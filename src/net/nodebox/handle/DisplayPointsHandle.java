package net.nodebox.handle;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.GraphicsContext;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.PathElement;
import net.nodebox.node.Node;

public class DisplayPointsHandle extends AbstractHandle {

    public DisplayPointsHandle(Node node) {
        super(node);
    }

    public void draw(GraphicsContext ctx) {
        BezierPath dots = new BezierPath();
        dots.setFillColor(HANDLE_COLOR);
        dots.setStrokeWidth(0.0);
        Grob grob = (Grob) node.getOutputValue();
        for (Grob child : grob.getChildren(BezierPath.class)) {
            BezierPath p = (BezierPath) child;
            for (PathElement el : p.getElements()) {
                drawDot(dots, el.getX(), el.getY());
            }
        }
        ctx.draw(dots);
    }
}
