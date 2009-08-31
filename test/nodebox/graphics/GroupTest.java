package nodebox.graphics;

import java.util.List;

public class GroupTest extends GraphicsTestCase {

    public void testBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Group g1 = new Group();
        g1.add(r1);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), g1.getBounds());
    }

    public void testTransformedBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Transform t = new Transform();
        t.translate(200, 300);
        r1.transform(t);
        Group g = new Group();
        g.add(r1);
        assertEquals(Rect.centeredRect(210, 320, 30, 40), g.getBounds());
    }

    public void testTransformedElements() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Path r2 = new Path();
        r2.rect(10, 120, 30, 40);
        Group g = new Group();
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
    public void testAdd() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        Group g = new Group();
        g.add(p);
        p.transform(Transform.translated(5, 7));
        // Since the path is in the group and not cloned,
        // the bounds of the group will be those of the translated path.
        assertEquals(Rect.centeredRect(15, 27, 30, 40), g.getBounds());
    }

    public void testTranslatePointsOfGroup() {
        Path p1 = new Path();
        Path p2 = new Path();
        p1.rect(10, 20, 30, 40);
        p2.rect(40, 20, 30, 40);
        Group g = new Group();
        g.add(p1);
        g.add(p2);
        assertEquals(Rect.centeredRect((40 - 10) / 2 + 10, 20, 60, 40), g.getBounds());
        for (Point pt : g.getPoints()) {
            pt.move(5, 7);
        }
        assertEquals(Rect.centeredRect(30, 27, 60, 40), g.getBounds());
    }

    public void testColors() {
        Path p1 = new Path();
        Path p2 = new Path();
        p1.rect(0, 0, 100, 100);
        p2.rect(150, 150, 100, 100);
        Group g = new Group();
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

    public void testMakePoints() {
        // Create a continuous line from 0,0 to 100,0.
        // The line is composed of one path from 0-50
        // and another path with two contours, from 50-75 and 75-100.
        Path p1 = new Path();
        p1.line(0, 0, 50, 0);
        Path p2 = new Path();
        p2.line(50, 0, 75, 0);
        p2.line(75, 0, 100, 0);
        Group g = new Group();
        g.add(p1);
        g.add(p2);
        assertEquals(100f, g.getLength());
        Point[] points = g.makePoints(5);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(25, 0, points[1]);
        assertPointEquals(50, 0, points[2]);
        assertPointEquals(75, 0, points[3]);
        assertPointEquals(100, 0, points[4]);
        // Achieve the same result using resampleByAmount.
        Group resampledGroup = g.resampleByAmount(5, false);
        List<Point> resampledPoints = resampledGroup.getPoints();
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
    public void testCacheInvalidation() {
        Group g = new Group();
        assertEquals(0f, g.getLength());
        Path p1 = new Path();
        p1.line(0, 0, 50, 0);
        g.add(p1);
        assertEquals(50f, g.getLength());
        // Change the Path after it was added to the Group.
        p1.line(50, 0, 75, 0);
        // This change is not detected by the Group, and thus the length is not updated.
        assertEquals(50f, g.getLength());
        // Manually invalidate the group.
        g.invalidate();
        // This time, the length is correct.
        assertEquals(75f, g.getLength());
        // Manually change the position of the last point.
        Point pt = g.getPoints().get(3);
        pt.x = 100;
        // This change is not detected by the Path, and thus the length is not updated.
        assertEquals(75f, g.getLength());
        // Manually invalidate the path.
        g.invalidate();
        // This time, the length is correct.
        assertEquals(100f, g.getLength());
    }

}
