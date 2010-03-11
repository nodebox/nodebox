package nodebox.graphics;

import junit.framework.TestCase;

public class GraphicsContextTest extends TestCase {

    public void testInheritFromContext() {
        GraphicsContext ctx = new GraphicsContext();
        Color c = new Color();
        assertEquals(c, ctx.fill());
        ctx.rect(0, 0, 100, 100);
        Path p = (Path) ctx.getCanvas().getItems().get(0);
        assertEquals(c, p.getFillColor());

        Color red = new Color(1, 0, 0);
        ctx.fill(red);
        ctx.align(Text.Align.RIGHT);
        Text t = ctx.text("hello", 20, 20);
        assertEquals(red, t.getFillColor());
        assertEquals(Text.Align.RIGHT, t.getAlign());
    }

}
