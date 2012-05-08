package nodebox.graphics;

import junit.framework.TestCase;

public class GraphicsTestCase extends TestCase {

    protected final float SIDE = 50;

    protected void assertPointEquals(float x, float y, Point actual) {
        assertEquals(x, actual.x, 0.001f);
        assertEquals(y, actual.y, 0.001f);
    }

    protected void addRect(IGeometry g, float x, float y, float width, float height) {
        g.addPoint(x, y);
        g.addPoint(x + width, y);
        g.addPoint(x + width, y + height);
        g.addPoint(x, y + height);
    }

    public void testDummy() {
        // One test necessary to keep JUnit happy.
    }
}
