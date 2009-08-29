package nodebox.graphics;

/**
 * Use cases for geometric operations.
 */
public class PathTest extends GraphicsTestCase {

    public void testEmptyPath() {
        Path p = new Path();
        assertEquals(0, p.getPoints().size());
    }

    public void testMakeEllipse() {
        Path p = new Path();
        p.ellipse(10, 20, 30, 40);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), p.getBounds());
    }

    public void testTranslatePoints() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        for (Point pt : p.getPoints()) {
            pt.move(5, 0);
        }
        assertEquals(Rect.centeredRect(15, 20, 30, 40), p.getBounds());
    }

//    public void testMakeText() {
//        Text t = new Text("A", 0, 20);
//        Path p = t.getPath();
//        // The letter "A" has 2 contours: the outer shape and the "hole".
//        assertEquals(2, p.getContours().length);
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

    public void testCustomAttributes() {
        // Add a velocity to each point of the path.
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        assertEquals(4, p.getPointCount());
    }

    public void testTransform() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        p.transform(Transform.translated(5, 7));
        assertEquals(Rect.centeredRect(15, 27, 30, 40), p.getBounds());
    }

    /**
     * How easy is it to convert the contours of a path to paths themselves?
     */
    public void testContoursToPaths() {
        // Create a path with two contours.
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        p.rect(150, 150, 100, 100);
        // Create a new group that will contain the converted contours.
        Group g = new Group();
        // Convert each contour to a path of its own.
        for (Contour c : p.getContours()) {
            g.add(c.toPath());
        }
    }

    public void testLength() {
        testLength(0, 0);
        testLength(200, 300);
        Path p = new Path();
        p.line(0, 0, 50, 0);
        p.line(50, 0, 100, 0);
        assertEquals(100f, p.getLength());
    }

    private void testLength(float x, float y) {
        Path p = new Path();
        addRect(p, x, y, SIDE, SIDE);
        assertEquals(SIDE * 3, p.getLength());
        p.close();
        assertEquals(SIDE * 4, p.getLength());
    }

    public void testPointAt() {
        Path p = new Path();
        p.line(0, 0, 50, 0);
        p.line(50, 0, 100, 0);
        assertEquals(new Point(0, 0), p.pointAt(0f));
        assertEquals(new Point(10, 0), p.pointAt(0.1f));
        assertEquals(new Point(25, 0), p.pointAt(0.25f));
        assertEquals(new Point(40, 0), p.pointAt(0.4f));
        assertEquals(new Point(50, 0), p.pointAt(0.5f));
        assertEquals(new Point(75, 0), p.pointAt(0.75f));
        assertEquals(new Point(80, 0), p.pointAt(0.8f));
        assertEquals(new Point(100, 0), p.pointAt(1f));
    }

    public void testPointAtMultipleContours() {
        Path p = new Path();
        float x1 = 0;
        float y1 = 0;
        float x2 = 200;
        float y2 = 0;
        addRect(p, x1, y1, SIDE, SIDE);
        p.newContour();
        addRect(p, x2, y2, SIDE, SIDE);
        assertEquals(2, p.getContours().size());
        assertEquals(8, p.getPointCount());
        assertPointEquals(x1, y1, p.pointAt(0f));
        // At the beginning of the second contour.
        assertPointEquals(x2, y2, p.pointAt(1f / 8f));
        assertPointEquals(x2, y2 + SIDE, p.pointAt(1f));
    }

    public void testContour() {
        final float SIDE = 50;
        Point[] points;
        Path p = new Path();
        addRect(p, 0, 0, SIDE, SIDE);
        assertEquals(SIDE * 3, p.getLength());
        points = p.makePoints(7);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);

        // Closing the contour will encrease the length of the path and thus will also
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
        final float SIDE = 50;
        final float x2 = 200;
        final float y2 = 300;
        Point[] points;
        Path p1 = new Path();
        // Build the path using separate contours.
        // You can also build two contours using moveto/lineto/lineto/lineto/moveto/...
        Contour c1 = new Contour();
        Contour c2 = new Contour();
        addRect(c1, 0, 0, SIDE, SIDE);
        addRect(c2, x2, y2, SIDE, SIDE);
        p1.add(c1);
        p1.add(c2);
        assertEquals(SIDE * 6, p1.getLength());
        points = p1.makePoints(14);
        assertPointEquals(0, 0, points[0]);
        assertPointEquals(SIDE / 2, 0, points[1]);
        assertPointEquals(SIDE, 0, points[2]);
        assertPointEquals(0, SIDE, points[6]);
        assertPointEquals(x2, y2, points[7]);

        // Create a path with two closed contours.
        Path p2 = new Path();
        c1 = new Contour();
        c2 = new Contour();
        addRect(c1, 0, 0, SIDE, SIDE);
        addRect(c2, x2, y2, SIDE, SIDE);
        c1.close();
        c2.close();
        p2.add(c1);
        p2.add(c2);
        assertEquals(SIDE * 8, p2.getLength());
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

}
