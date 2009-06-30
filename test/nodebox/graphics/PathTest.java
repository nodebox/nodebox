package nodebox.graphics;

import junit.framework.TestCase;

/**
 * Use cases for geometric operations.
 */
public class PathTest extends TestCase {

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
        Path p = new Path();
        p.line(0, 0, 50, 0);
        p.line(50, 0, 100, 0);
        assertEquals(100f, p.getLength());
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


}
