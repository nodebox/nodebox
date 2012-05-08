package nodebox.graphics;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;
import static nodebox.graphics.GraphicsTestUtils.assertPointEquals;

public class GeometryTest {

    @Test
    public void testBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Geometry g1 = new Geometry();
        g1.add(r1);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), g1.getBounds());
    }

    @Test
    public void testTransformedBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Transform t = new Transform();
        t.translate(200, 300);
        r1.transform(t);
        Geometry g = new Geometry();
        g.add(r1);
        assertEquals(Rect.centeredRect(210, 320, 30, 40), g.getBounds());
    }

    /**
     * Check the bounds for an empty path.
     */
    @Test
    public void testEmptyBounds() {
        assertEquals(new Rect(), new Geometry().getBounds());
        Path p1 = new Path();
        p1.rect(100, 200, 30, 40);
        Geometry g1 = new Geometry();
        g1.add(p1);
        Rect r = Rect.centeredRect(100, 200, 30, 40);
        assertEquals(r, g1.getBounds());
        Path p2 = new Path();
        Geometry g2 = new Geometry();
        g2.add(p1);
        g2.add(p2);
        assertEquals(r, g2.getBounds());
    }

    /**
     * Check if a contour is empty.
     */
    @Test
    public void testIsEmpty() {
        Geometry g1 = new Geometry();
        assertTrue(g1.isEmpty());
        Geometry g2 = new Geometry();
        // Adding even an empty path makes the geometry not empty.
        g2.add(new Path());
        assertFalse(g2.isEmpty());
    }

    @Test
    public void testTransformedElements() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Path r2 = new Path();
        r2.rect(10, 120, 30, 40);
        Geometry g = new Geometry();
        g.add(r1);
        g.add(r2);
        Rect rect1 = Rect.centeredRect(10, 20, 30, 40);
        Rect rect2 = Rect.centeredRect(10, 120, 30, 40);
        assertEquals(rect1.united(rect2), g.getBounds());
    }

    /**
     * Geometry added to the group is not cloned. Test if you can still change
     * the original geometry.
     */
    @Test
    public void testAdd() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        Geometry g = new Geometry();
        g.add(p);
        p.transform(Transform.translated(5, 7));
        // Since the path is in the group and not cloned,
        // the bounds of the group will be those of the translated path.
        assertEquals(Rect.centeredRect(15, 27, 30, 40), g.getBounds());
    }

    @Test
    public void testTranslatePointsOfGroup() {
        Path p1 = new Path();
        Path p2 = new Path();
        p1.rect(10, 20, 30, 40);
        p2.rect(40, 20, 30, 40);
        Geometry g = new Geometry();
        g.add(p1);
        g.add(p2);
        assertEquals(Rect.centeredRect((40 - 10) / 2 + 10, 20, 60, 40), g.getBounds());
        Transform t = Transform.translated(5, 7);
        Geometry g2 = t.map(g);
        assertEquals(Rect.centeredRect(30, 27, 60, 40), g2.getBounds());
    }

    @Test
    public void testColors() {
        Path p1 = new Path();
        Path p2 = new Path();
        p1.rect(0, 0, 100, 100);
        p2.rect(150, 150, 100, 100);
        Geometry g = new Geometry();
        g.add(p1);
        g.add(p2);
        assertEquals(2, g.size());
        // Each path has 4 points.
        assertEquals(8, g.getPointCount());
        Color red = new Color(1, 0, 0);
        g.setFill(red);
        assertEquals(red, p1.getFillColor());
        assertEquals(red, p2.getFillColor());
    }

    @Test
    public void testMakePoints() {
        // Create a continuous line from 0,0 to 100,0.
        // The line is composed of one path from 0-50
        // and another path with two contours, from 50-75 and 75-100.
        Path p1 = new Path();
        p1.line(0, 0, 50, 0);
        Path p2 = new Path();
        p2.line(50, 0, 75, 0);
        p2.line(75, 0, 100, 0);
        Geometry g = new Geometry();
        g.add(p1);
        g.add(p2);
        assertEquals(100.0, g.getLength());
        Point[] points = g.makePoints(5);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(25, 0, points[1]);
        assertPointEquals(50, 0, points[2]);
        assertPointEquals(75, 0, points[3]);
        assertPointEquals(100, 0, points[4]);
        // Achieve the same result using resampleByAmount.
        Geometry resampledGeometry = g.resampleByAmount(5, false);
        List<Point> resampledPoints = resampledGeometry.getPoints();
        assertPointEquals(0, 0, resampledPoints.get(0));
        assertPointEquals(25, 0, resampledPoints.get(1));
        assertPointEquals(50, 0, resampledPoints.get(2));
        assertPointEquals(75, 0, resampledPoints.get(3));
        assertPointEquals(100, 0, resampledPoints.get(4));
    }

    /**
     * Group uses a path length cache to speed up pointAt, makePoints and resample operations.
     * Check if the cache is properly invalidated.
     */
    @Test
    public void testCacheInvalidation() {
        Geometry g = new Geometry();
        assertEquals(0.0, g.getLength());
        Path p1 = new Path();
        p1.line(0, 0, 50, 0);
        g.add(p1);
        assertEquals(50.0, g.getLength());
        // Change the Path after it was added to the Geometry.
        p1.line(50, 0, 75, 0);
        // This change is not detected by the Geometry, and thus the length is not updated.
        assertEquals(50.0, g.getLength());
        // Manually invalidate the group.
        g.invalidate();
        // This time, the length is correct.
        assertEquals(75.0, g.getLength());
    }

    @Test
    public void testLength() {
        Geometry g = new Geometry();
        Path p1 = new Path();
        p1.line(0, 0, 100, 0);
        Path p2 = new Path();
        p2.line(0, 100, 100, 100);
        g.add(p1);
        g.add(p2);
        assertEquals(200.0, g.getLength());
    }

}
