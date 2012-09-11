package nodebox.graphics;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static nodebox.graphics.GraphicsTestUtils.addRect;
import static nodebox.graphics.GraphicsTestUtils.assertPointEquals;

/**
 * Use cases for geometric operations.
 */
public class PathTest {

    public static final double SIDE = GraphicsTestUtils.SIDE;

    @Test
    public void testEmptyPath() {
        Path p = new Path();
        assertEquals(0, p.getPoints().size());
    }

    @Test
    public void testMakeEllipse() {
        Path p = new Path();
        p.ellipse(10, 20, 30, 40);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), p.getBounds());
    }

//    public void testTranslatePoints() {
//        Path p = new Path();
//        p.rect(10, 20, 30, 40);
//        for (Point pt : p.getPoints()) {
//            pt.move(5, 0);
//        }
//        assertEquals(Rect.centeredRect(15, 20, 30, 40), p.getBounds());
//    }

//    public void testMakeText() {
//        Text t = new Text("A", 0, 20);
//        Path p = t.getPath();
//        // The letter "A" has 2 contours: the outer shape and the "hole".
//        assertEquals(2, p.getContours().size());
//    }

//    public void testBooleanOperations() {
//        // Create two overlapping shapes.
//        Path p1 = new Path();
//        Path p2 = new Path();
//        p1.rect(0, 0, 20, 40);
//        p2.rect(10, 0, 20, 40);
//        Path result = p1.intersected(p2);
//        assertEquals(new Rect(10, 0, 10, 40), result.getBounds());
//    }

    @Test
    public void testCustomAttributes() {
        // Add a velocity to each point of the path.
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        assertEquals(4, p.getPointCount());
    }

    @Test
    public void testTransform() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        p.transform(Transform.translated(5, 7));
        assertEquals(Rect.centeredRect(15, 27, 30, 40), p.getBounds());
    }

    /**
     * How easy is it to convert the contours of a path to paths themselves?
     */
    @Test
    public void testContoursToPaths() {
        // Create a path with two contours.
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        p.rect(150, 150, 100, 100);
        // Create a new group that will contain the converted contours.
        Geometry g = new Geometry();
        // Convert each contour to a path of its own.
        for (Contour c : p.getContours()) {
            g.add(c.toPath());
        }
    }

    @Test
    public void testLength() {
        testLength(0, 0);
        testLength(200, 300);
        Path p = new Path();
        p.line(0, 0, 50, 0);
        p.line(50, 0, 100, 0);
        assertEquals(100.0, p.getLength());
    }

    private void testLength(float x, float y) {
        Path p = new Path();
        addRect(p, x, y, SIDE, SIDE);
        assertEquals(SIDE * 3, p.getLength());
        p.close();
        assertEquals(SIDE * 4, p.getLength());
    }

    public void testLengthMultipleContours() {
        Path p = new Path();
        p.line(0, 0, 100, 0);
        assertEquals(100.0, p.getLength());
        p.line(0, 100, 100, 100);
        assertEquals(200.0, p.getLength());
        p.close();
        assertEquals(300.0, p.getLength());
    }

    public void testPointAt() {
        Path p = new Path();
        p.line(0, 0, 50, 0);
        p.line(50, 0, 100, 0);
        assertPointEquals(0, 0, p.pointAt(0));
        assertPointEquals(10, 0, p.pointAt(0.1));
        assertPointEquals(25, 0, p.pointAt(0.25));
        assertPointEquals(40, 0, p.pointAt(0.4));
        assertPointEquals(50, 0, p.pointAt(0.5));
        assertPointEquals(75, 0, p.pointAt(0.75));
        assertPointEquals(80, 0, p.pointAt(0.8));
        assertPointEquals(100, 0, p.pointAt(1));
    }

    public void testPointAtMultipleContours() {
        // Create two contours that look like a single line.
        Path p = new Path();
        p.addPoint(0, 0);
        p.addPoint(50, 0);
        p.newContour();
        p.addPoint(50, 0);
        p.addPoint(100, 0);
        assertEquals(2, p.getContours().size());
        assertEquals(4, p.getPointCount());
        assertEquals(100.0, p.getLength());
        assertPointEquals(0, 0, p.pointAt(0));
        assertPointEquals(25, 0, p.pointAt(0.25));
        assertPointEquals(50, 0, p.pointAt(0.5));
        assertPointEquals(100, 0, p.pointAt(1.0));
    }

    public void testContour() {
        final double SIDE = 50;
        Point[] points;
        Path p = new Path();
        addRect(p, 0, 0, SIDE, SIDE);
        assertEquals(SIDE * 3, p.getLength());
        points = p.makePoints(7);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);

        // Closing the contour will increase the length of the path and thus will also
        // have an effect on point positions.
        p.close();
        assertEquals(SIDE * 4, p.getLength());
        points = p.makePoints(8);
        assertEquals(new Point(0, 0), points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);
        assertPointEquals(0, SIDE / 2, points[7]);
    }

    public void testMultipleContours() {
        Point[] points;
        // Create a path with separate contours.
        // Each contour describes a side of a rectangle.
        Path path = new Path();
        path.addPoint(0, 0);
        path.addPoint(SIDE, 0);
        path.newContour();
        path.addPoint(SIDE, 0);
        path.addPoint(SIDE, SIDE);
        path.newContour();
        path.addPoint(SIDE, SIDE);
        path.addPoint(0, SIDE);
        assertEquals(SIDE * 3, path.getLength());
        points = path.makePoints(4);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(SIDE, 0, points[1]);
        assertPointEquals(SIDE, SIDE, points[2]);
        assertPointEquals(0, SIDE, points[3]);
        // Get the same result by resampling the path.
        Path resampled = path.resampleByAmount(4, false);
        List<Point> resampledPoints = resampled.getPoints();
        assertPointEquals(0, 0, resampledPoints.get(0));
        assertPointEquals(SIDE, 0, resampledPoints.get(1));
        assertPointEquals(SIDE, SIDE, resampledPoints.get(2));
        assertPointEquals(0, SIDE, resampledPoints.get(3));
    }

    private Path cornerRect(float x, float y, float width, float height) {
        Path p = new Path();
        p.rect(x + width / 2, y + height / 2, width, height);
        return p;
    }

    public void testIntersected() {
        // Create two non-overlapping rectangles.
        Path p1 = cornerRect(0, 0, 100, 100);
        Path p2 = cornerRect(100, 0, 100, 100);
        // The intersection of the two is empty.
        assertEquals(new Rect(), p1.intersected(p2).getBounds());
        // Create two paths were one is entirely enclosed within the other.
        p1 = cornerRect(0, 0, 100, 100);
        p2 = cornerRect(20, 30, 10, 10);
        // The intersection is the smaller path.
        assertEquals(p2.getBounds(), p1.intersected(p2).getBounds());
    }

    /**
     * Path uses a contour length cache to speed up pointAt, makePoints and resample operations.
     * Check if the cache is properly invalidated.
     */
    public void testCacheInvalidation() {
        Path p = new Path();
        assertEquals(0.0, p.getLength());
        p.line(0, 0, 50, 0);
        assertEquals(50.0, p.getLength());
        Contour c = new Contour();
        c.addPoint(50, 0);
        c.addPoint(75, 0);
        p.add(c);
        assertEquals(75.0, p.getLength());
        // Add a point to the contour.
        c.addPoint(100, 0);
        // This change is not detected by the Path, and thus the length is not updated.
        assertEquals(75.0, p.getLength());
        // Manually invalidate the path.
        p.invalidate();
        // This time, the length is correct.
        assertEquals(100.0, p.getLength());
    }

    public void testNegativeBounds() {
        Path p1 = new Path();
        p1.rect(10, 20, 30, 40);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), p1.getBounds());

        Path p2 = new Path();
        p2.rect(-80, -200, 100, 100);
        assertEquals(Rect.centeredRect(-80, -200, 100, 100), p2.getBounds());
    }

    /**
     * Check the bounds for an empty path.
     */
    public void testEmptyBounds() {
        Path p1 = new Path();
        assertEquals(new Rect(), p1.getBounds());
        // Construct a path with an empty contour.
        Path p2 = new Path();
        p2.add(new Contour());
        assertEquals(new Rect(), p2.getBounds());
        // Construct a path with an empty and filled contour.
        Path p3 = new Path();
        p3.add(new Contour());
        Contour c2 = new Contour();
        Rect r = new Rect(20, 30, 40, 50);
        c2.addPoint(r.getX(), r.getY());
        c2.addPoint(r.getX() + r.getWidth(), r.getY() + r.getHeight());
        p3.add(c2);
        assertEquals(r, p3.getBounds());
    }

}
