package nodebox.graphics;

import junit.framework.TestCase;

public class GroupTest extends TestCase {

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

    public void testGetPaths() {
        Group root = new Group();
        Group parent1 = new Group();
        Group parent2 = new Group();
        root.add(parent1);
        root.add(parent2);
        assertEquals(0, root.getPaths().length);
        Path p1 = new Path();
        parent1.add(p1);
        assertEquals(1, root.getPaths().length);
        assertSame(p1, root.getPaths()[0]);
        Path p2 = new Path();
        parent2.add(p2);
        assertEquals(2, root.getPaths().length);
        assertSame(p1, root.getPaths()[0]);
        assertSame(p2, root.getPaths()[1]);
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

}
