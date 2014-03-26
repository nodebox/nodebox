package nodebox.graphics;

import org.junit.Test;

import java.util.Iterator;

import static junit.framework.Assert.*;

public class GraphicsContextTest {

    @Test
    public void testSize() {
        CanvasContext ctx = new CanvasContext();
        ctx.size(200, 300);
        assertEquals(200.0, ctx.getWIDTH());
        assertEquals(300.0, ctx.getHeight());
        assertEquals(200.0, ctx.getCanvas().getWidth());
        assertEquals(300.0, ctx.getCanvas().getHeight());
    }

    @Test
    public void testInheritFromContext() {
        CanvasContext ctx = new CanvasContext();
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

    @Test
    public void testGrid() {
        CanvasContext ctx = new CanvasContext();
        Iterator<Point> points = ctx.grid(2, 3, 3, 5);
        assertNextPoint(points, 0, 0);
        assertNextPoint(points, 3, 0);
        assertNextPoint(points, 0, 5);
        assertNextPoint(points, 3, 5);
        assertNextPoint(points, 0, 10);
        assertNextPoint(points, 3, 10);
        assertFalse(points.hasNext());
    }

    private void assertNextPoint(Iterator<Point> points, double x, double y) {
        assertTrue(points.hasNext());
        assertEquals(new Point(x, y), points.next());
    }

}
