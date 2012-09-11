package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;

public class SnapHandle extends AbstractHandle {

    @Override
    protected Rect createHitRectangle(double x, double y) {
        return new Rect(-1000, -1000, 2000, 2000);
    }

    public void draw(GraphicsContext ctx) {
        Point pos = (Point) getValue("position");
        double snapX = pos.x;
        double snapY = pos.y;
        double distance = (Double) getValue("distance");
        ctx.stroke(0.4, 0.4, 0.4, 0.5);
        ctx.strokewidth(1.0);
        Path p = new Path();
        for (int i = -100; i < 100; i++) {
            double x = -snapX + (i * distance);
            double y = -snapY + (i * distance);
            p.line(x, -1000, x, 1000);
            p.line(-1000, y, 1000, y);
        }
        ctx.drawpath(p);
    }

}
