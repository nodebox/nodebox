package nodebox.graphics;

import junit.framework.TestCase;

/**
 * Use cases for geometric operations.
 */
public class GeometryTest extends TestCase {

    public void testEmptyPath() {
        Path p = new Path();
        assertEquals(0, p.getPoints().size());
    }


    public void testMakeEllipse() {
        Path p = new Path();
        p.ellipse(10, 20, 30, 40);
        assertEquals(new Rect(10, 20, 30, 40), p.getBounds());
    }

    public void testTranslatePoints() {
        Path p = new Path();
        p.rect(10, 20, 30, 40);
        for (Point pt : p.getPoints()) {
            pt.move(5, 0);
        }
        assertEquals(new Rect(15, 20, 30, 40), p.getBounds());
    }

    public void testTranslatePointsOfGroup() {
        Path p1 = new Path();
        Path p2 = new Path();
        p1.rect(10, 20, 30, 40);
        p2.rect(40, 20, 30, 40);
        Group g = new Group();
        g.add(p1);
        g.add(p2);
        assertEquals(new Rect(10, 20, 60, 40), g.getBounds());
        for (Point pt : g.getPoints()) {
            pt.move(5, 7);
        }
        assertEquals(new Rect(15, 27, 60, 40), g.getBounds());
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
        assertEquals(new Rect(15, 27, 30, 40), p.getBounds());
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


}
