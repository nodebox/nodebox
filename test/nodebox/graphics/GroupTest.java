package nodebox.graphics;

import junit.framework.TestCase;

public class GroupTest extends TestCase {

    public void testBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Group g1 = new Group();
        g1.add(r1);
        assertEquals(new Rect(10, 20, 30, 40), g1.getBounds());
    }

    public void testTransformedBounds() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Transform t = new Transform();
        t.translate(200, 300);
        r1.transform(t);
        Group g = new Group();
        g.add(r1);
        assertEquals(new Rect(210, 320, 30, 40), g.getBounds());
    }

    public void testTransformedElements() {
        Path r1 = new Path();
        r1.rect(10, 20, 30, 40);
        Path r2 = new Path();
        r2.rect(10, 120, 30, 40);
        Group g = new Group();
        g.add(r1);
        g.add(r2);
        Rect rect1 = new Rect(10, 20, 30, 40);
        Rect rect2 = new Rect(10, 120, 30, 40);
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
        assertEquals(new Rect(15, 27, 30, 40), g.getBounds());
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


}
