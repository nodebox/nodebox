package nodebox.graphics;

import junit.framework.TestCase;

public class GraphicsContextTest extends TestCase {

    public void testInheritFromContext() {
        GraphicsContext ctx = new GraphicsContext();
        Color c = new Color();
        assertEquals(c, ctx.getFillColor());
        ctx.rect(0, 0, 100, 100);
        BezierPath p = (BezierPath) ctx.getCanvas().getGrobs().get(0);
        assertEquals(c, p.getFillColor());

        Color red = new Color(1, 0, 0);
        ctx.setFillColor(red);
        ctx.setAlign(Text.Align.RIGHT);
        Text t = ctx.text("hello", 20, 20);
        assertEquals(red, t.getFillColor());
        assertEquals(Text.Align.RIGHT, t.getAlign());
    }

}
