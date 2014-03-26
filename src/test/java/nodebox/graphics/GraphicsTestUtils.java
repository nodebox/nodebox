package nodebox.graphics;

import static junit.framework.Assert.assertEquals;

public class GraphicsTestUtils {

    public static final double SIDE = 50;

    public static void assertPointEquals(double x, double y, Point actual) {
        assertEquals(x, actual.x, 0.001);
        assertEquals(y, actual.y, 0.001);
    }

    public static void addRect(IGeometry g, double x, double y, double width, double height) {
        g.addPoint(x, y);
        g.addPoint(x + width, y);
        g.addPoint(x + width, y + height);
        g.addPoint(x, y + height);
    }

}
