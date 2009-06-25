package nodebox.handle;

import nodebox.graphics.*;
import nodebox.node.Node;

public class DisplayPointsHandle extends AbstractHandle {

    private boolean displayPointNumbers = false;

    public DisplayPointsHandle(Node node) {
        super(node);
    }

    public void draw(GraphicsContext ctx) {
        BezierPath dots = new BezierPath();
        dots.setFillColor(HANDLE_COLOR);
        dots.setStrokeWidth(0f);
        if (node.getOutputValue() instanceof BezierPath) {
            drawDots(ctx, (BezierPath) node.getOutputValue(), dots);
        } else {
            Grob grob = (Grob) node.getOutputValue();
            // TODO: Fix this
            /*
            for (Grob child : grob.getChildren(BezierPath.class)) {
                drawDots(ctx, (BezierPath) child, dots);
            }
            */
        }
        ctx.draw(dots);
    }

    public boolean isDisplayPointNumbers() {
        return displayPointNumbers;
    }

    public void setDisplayPointNumbers(boolean displayPointNumbers) {
        this.displayPointNumbers = displayPointNumbers;
    }

    private void drawDots(GraphicsContext ctx, BezierPath path, BezierPath dots) {
        boolean displayPointNumbers = this.displayPointNumbers;
        int i = 0;
        for (PathElement el : path.getElements()) {
            drawDot(dots, el.getX(), el.getY());
            if (displayPointNumbers) {
                Text t = ctx.text(String.valueOf(i), el.getX() + 7, el.getY());
                t.setFontSize(10);
            }
            i++;
        }

    }
}