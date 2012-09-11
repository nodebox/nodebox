package nodebox.node.polygraph;

import junit.framework.TestCase;

public class PolygonTest extends TestCase {

    public void testBounds() {
        Polygon p;
        p = Polygon.rect(0, 0, 100, 100);
        assertEquals(new Rectangle(0, 0, 100, 100), p.getBounds());
        p = Polygon.rect(-32, -45, 12, 68);
        assertEquals(new Rectangle(-32, -45, 12, 68), p.getBounds());
    }

    public void testTranslated() {
        Polygon p;
        p = Polygon.rect(0, 0, 100, 100);
        Polygon p2 = p.translated(50, -40);
        assertEquals(new Rectangle(50, -40, 100, 100), p2.getBounds());
    }
}
