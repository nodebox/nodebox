package nodebox.graphics;

import junit.framework.TestCase;

public class ContourTest extends TestCase {

    public void testPointAt() {
        Contour c1 = new Contour();
        c1.addPoint(0, 0);
        c1.addPoint(100, 0);
        assertEquals(new Point(0, 0), c1.pointAt(0));
        assertEquals(new Point(50f, 0), c1.pointAt(0.5f));
        assertEquals(new Point(100, 0), c1.pointAt(1f));
        assertEquals(new Point(-50, 0), c1.pointAt(-0.5f));
        assertEquals(new Point(150, 0), c1.pointAt(1.5f));
        Contour c2 = new Contour();
        c2.addPoint(new Point(0, 0, Point.LINE_TO));
        c2.addPoint(new Point(0, 0, Point.CURVE_DATA));
        c2.addPoint(new Point(100, 0, Point.CURVE_DATA));
        c2.addPoint(new Point(100, 0, Point.CURVE_TO));
        assertEquals(new Point(0, 0), c2.pointAt(0));
        assertEquals(new Point(50f, 0), c2.pointAt(0.5f));
        assertEquals(new Point(100, 0), c2.pointAt(1f));
        assertEquals(new Point(-50, 0), c2.pointAt(-0.5f));
        assertEquals(new Point(150, 0), c2.pointAt(1.5f));
    }

    public void testPointAtMultiple() {
        Contour c1 = new Contour();
        c1.addPoint(0, 0);
        c1.addPoint(50, 0);
        c1.addPoint(100, 0);
        assertEquals(new Point(-50, 0), c1.pointAt(-0.5f));
        assertEquals(new Point(0, 0), c1.pointAt(0));
        assertEquals(new Point(25, 0), c1.pointAt(0.25f));
        assertEquals(new Point(50, 0), c1.pointAt(0.5f));
        assertEquals(new Point(60, 0), c1.pointAt(0.6f));
        assertEquals(new Point(100, 0), c1.pointAt(1f));
        assertEquals(new Point(150, 0), c1.pointAt(1.5f));
    }

    public void testLength() {
        Contour c = new Contour();
        c.addPoint(0, 0);
        c.addPoint(100, 0);
        assertEquals(100f, c.getLength());
    }
}
