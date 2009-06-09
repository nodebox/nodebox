package nodebox.graphics;

import junit.framework.TestCase;

public class GroupTest extends TestCase {

    public void testBounds() {
        BezierPath r1 = new BezierPath();
        r1.rect(10, 20, 30, 40);
        Group g1 = new Group();
        g1.add(r1);
        assertEquals(Rect.centeredRect(10, 20, 30, 40), g1.getBounds());
    }

    public void testTransformedBounds() {
        BezierPath r1 = new BezierPath();
        r1.rect(10, 20, 30, 40);
        r1.translate(200, 300);
        Group g = new Group();
        g.add(r1);
        assertEquals(Rect.centeredRect(210, 320, 30, 40), g.getBounds());
    }

    public void testTransformedElements() {
        BezierPath r1 = new BezierPath();
        r1.rect(10, 20, 30, 40);
        BezierPath r2 = new BezierPath();
        r2.rect(10, 120, 30, 40);
        Group g = new Group();
        g.add(r1);
        g.add(r2);
        Rect rect1 = Rect.centeredRect(10, 20, 30, 40);
        Rect rect2 = Rect.centeredRect(10, 120, 30, 40);
        assertEquals(rect1.united(rect2), g.getBounds());
    }


}
