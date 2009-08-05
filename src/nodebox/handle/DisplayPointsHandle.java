package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Text;
import nodebox.node.Node;

public class DisplayPointsHandle extends AbstractHandle {

    private boolean displayPointNumbers = false;

    public DisplayPointsHandle(Node node) {
        super(node);
    }

    public void draw(GraphicsContext ctx) {
        if (!(node.getOutputValue() instanceof Path)) return;
        Path dots = new Path();
        dots.setFillColor(HANDLE_COLOR);
        dots.setStrokeWidth(0f);
        drawDots(ctx, (Path) node.getOutputValue(), dots);
        ctx.draw(dots);
    }

    public boolean isDisplayPointNumbers() {
        return displayPointNumbers;
    }

    public void setDisplayPointNumbers(boolean displayPointNumbers) {
        this.displayPointNumbers = displayPointNumbers;
    }

    private void drawDots(GraphicsContext ctx, Path path, Path dots) {
        boolean displayPointNumbers = this.displayPointNumbers;
        int i = 0;
        for (Point pt : path.getPoints()) {
            drawDot(dots, pt.x, pt.y);
            if (displayPointNumbers) {
                Text t = ctx.text(String.valueOf(i), pt.x + 7, pt.y);
                t.setFontSize(10);
            }
            i++;
        }

    }
}