package nodebox.graphics;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;
import static nodebox.graphics.GraphicsTestUtils.addRect;
import static nodebox.graphics.GraphicsTestUtils.assertPointEquals;

public class ContourTest {

    public static final double SIDE = GraphicsTestUtils.SIDE;

    @Test
    public void testPointAt() {
        Contour c1 = new Contour();
        c1.addPoint(0, 0);
        c1.addPoint(100, 0);
        assertPointEquals(0, 0, c1.pointAt(0));
        assertPointEquals(50, 0, c1.pointAt(0.5));
        assertPointEquals(100, 0, c1.pointAt(1));
        assertPointEquals(-50, 0, c1.pointAt(-0.5));
        assertPointEquals(150, 0, c1.pointAt(1.5));
        Contour c2 = new Contour();
        c2.addPoint(new Point(0, 0, Point.LINE_TO));
        c2.addPoint(new Point(0, 0, Point.CURVE_DATA));
        c2.addPoint(new Point(100, 0, Point.CURVE_DATA));
        c2.addPoint(new Point(100, 0, Point.CURVE_TO));
        assertPointEquals(0, 0, c2.pointAt(0));
        assertPointEquals(50, 0, c2.pointAt(0.5));
        assertPointEquals(100, 0, c2.pointAt(1));
        //assertEquals(new Point(-50, 0), c2.pointAt(-0.5));
        //assertEquals(new Point(150, 0), c2.pointAt(1.5));
    }

    @Test
    public void testPointAtEmptyPath() {
        Contour c = new Contour();
        try {
            c.pointAt(0.1);
            fail("Should have thrown an error.");
        } catch (NodeBoxError e) {
        }

        c.addPoint(33, 44);
        assertPointEquals(33, 44, c.pointAt(0.1));
        assertPointEquals(33, 44, c.pointAt(100));
        assertPointEquals(33, 44, c.pointAt(-12));
    }

    @Test
    public void testPointAtClosed() {
        Contour c = new Contour();
        c.addPoint(0, 0);
        c.addPoint(SIDE, 0);
        c.addPoint(SIDE, SIDE);
        c.addPoint(0, SIDE);
        assertEquals(SIDE * 3, c.getLength());
        assertPointEquals(0, 0, c.pointAt(0));
        assertPointEquals(SIDE, SIDE / 2, c.pointAt(0.5));
        assertPointEquals(0, SIDE, c.pointAt(1));
        c.close();
        assertEquals(SIDE * 4, c.getLength());
        assertPointEquals(0, 0, c.pointAt(0));
        assertPointEquals(SIDE, SIDE, c.pointAt(0.5));
        assertPointEquals(0, 0, c.pointAt(1));
    }

    @Test
    public void testPointAtMultiple() {
        Contour c1 = new Contour();
        c1.addPoint(0, 0);
        c1.addPoint(50, 0);
        c1.addPoint(100, 0);
        assertPointEquals(-50, 0, c1.pointAt(-0.5));
        assertPointEquals(0, 0, c1.pointAt(0));
        assertPointEquals(25, 0, c1.pointAt(0.25));
        assertPointEquals(50, 0, c1.pointAt(0.5));
        assertPointEquals(60, 0, c1.pointAt(0.6));
        assertPointEquals(100, 0, c1.pointAt(1));
        assertPointEquals(150, 0, c1.pointAt(1.5));
    }

    @Test
    public void testLength() {
        assertLength(0, 0);
        assertLength(100, 200);
    }

    private void assertLength(double x, double y) {
        Contour c = new Contour();
        c.addPoint(x, y);
        c.addPoint(x + SIDE, y);
        c.addPoint(x + SIDE, y + SIDE);
        c.addPoint(x, y + SIDE);
        assertEquals(SIDE * 3, c.getLength());
        c.close();
        assertEquals(SIDE * 4, c.getLength());
    }

    @Test
    public void testMakePoints() {
        Point[] points;
        // A contour that is "open", which means it doesn't describe the last point.
        Contour c = new Contour();
        c.addPoint(0, 0);
        c.addPoint(SIDE, 0);
        c.addPoint(SIDE, SIDE);
        c.addPoint(0, SIDE);
        assertEquals(SIDE * 3, c.getLength());
        points = c.makePoints(7);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);

        // Closing the contour will encrease the length of the path and thus will also
        // have an effect on point positions.
        c.close();
        assertEquals(SIDE * 4, c.getLength());
        points = c.makePoints(8);
        assertEquals(new Point(0, 0), points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);
        assertPointEquals(0, SIDE / 2, points[7]);
//
//        // A contour that is "closed", which means that the last and first point are equal.
//        Contour closedContour = new Contour();
//        closedContour.addPoint(0, 0);
//        closedContour.addPoint(50, 0);
//        closedContour.addPoint(50, 50);
//        closedContour.addPoint(0, 0);
//        assertEquals(150.0, closedContour.getLength());
//        points = closedContour.makePoints(6);
//        // The first and last points overlap.
//        assertEquals(new Point(0, 0), points[0]);
//        assertEquals(new Point(25, 0), points[1]);
//        assertEquals(new Point(50, 0), points[2]);
//        assertEquals(new Point(50, 25), points[3]);
//        assertEquals(new Point(50, 50), points[4]);
//        assertEquals(new Point(25, 25), points[5]);
//        // Because the first and last points overlap, closing the contour has no effect.
//        // The length does not increase.
//        closedContour.close();
//        assertEquals(150.0, closedContour.getLength());
//        // Point positions remain unchanged.
//        points = closedContour.makePoints(6);
//        assertEquals(new Point(0, 0), points[0]);
//        assertEquals(new Point(25, 0), points[1]);
//        assertEquals(new Point(50, 0), points[2]);
//        assertEquals(new Point(50, 25), points[3]);
//        assertEquals(new Point(50, 50), points[4]);
//        assertEquals(new Point(25, 25), points[5]);
    }

    @Test
    public void testMakePointsEmptyPath() {
        Contour c = new Contour();
        Point[] points = c.makePoints(10);
        assertEquals(0, points.length);
    }

    @Test
    public void testResample() {
        Contour r;
        List<Point> points;

        Contour c1 = new Contour();
        r = c1.resampleByAmount(10);
        assertEquals(0, r.getPointCount());
        assertFalse(r.isClosed());

        Contour c2 = new Contour();
        addRect(c2, 0, 0, SIDE, SIDE);
        r = c2.resampleByAmount(4);
        assertEquals(4, r.getPointCount());
        assertFalse(r.isClosed());
        points = r.getPoints();
        assertPointEquals(0, 0, points.get(0));
        assertPointEquals(SIDE, 0, points.get(1));
        assertPointEquals(SIDE, SIDE, points.get(2));
        assertPointEquals(0, SIDE, points.get(3));

        c2.close();
        r = c2.resampleByAmount(4);
        assertEquals(4, r.getPointCount());
        assertTrue(r.isClosed());
        points = r.getPoints();
        assertPointEquals(0, 0, points.get(0));
        assertPointEquals(0, SIDE, points.get(3));
    }

    @Test
    public void testResampleByLength() {
        Contour r;
        Contour c1 = new Contour();
        r = c1.resampleByLength(1);
        assertEquals(0, r.getPointCount());
        assertFalse(r.isClosed());

        Contour c2 = new Contour();
        addRect(c2, 0, 0, SIDE, SIDE);
        r = c2.resampleByLength(SIDE);
        assertFalse(r.isClosed());
        assertRectPoints(r, 0, 0, SIDE, SIDE);

        c2.close();
        r = c2.resampleByLength(SIDE);
        assertTrue(r.isClosed());
        assertRectPoints(r, 0, 0, SIDE, SIDE);
    }

    /**
     * Contour uses a length cache to speed up pointAt, makePoints and resample operations.
     * Check if the cache is properly invalidated.
     */
    @Test
    public void testCacheInvalidation() {
        Contour c = new Contour();
        c.addPoint(0, 0);
        c.addPoint(50, 0);
        assertEquals(50.0, c.getLength());
        // Add a point
        c.addPoint(100, 0);
        // Check the length again.
        assertEquals(100.0, c.getLength());
    }

    /**
     * Check the bounds for an empty contour.
     */
    @Test
    public void testEmptyBounds() {
        Contour c = new Contour();
        Rect r = c.getBounds();
        assertEquals(new Rect(), r);
    }

    private void assertRectPoints(IGeometry g, double x, double y, double width, double height) {
        assertEquals(4, g.getPointCount());
        List<Point> points = g.getPoints();
        assertPointEquals(x, y, points.get(0));
        assertPointEquals(x + width, y, points.get(1));
        assertPointEquals(x + width, y + height, points.get(2));
        assertPointEquals(x, y + height, points.get(3));
    }

}
