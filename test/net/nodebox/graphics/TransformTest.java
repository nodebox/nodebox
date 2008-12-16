package net.nodebox.graphics;

import junit.framework.TestCase;

public class TransformTest extends TestCase {

    public void testTranslate() {
        Transform t = new Transform();
        t.translate(0, 0);
        Point p = new Point(1, 2);
        assertEquals(new Point(1, 2), t.map(p));
        t.translate(0, 100);
        assertEquals(new Point(1, 102), t.map(p));
        t.translate(0, 100);
        assertEquals(new Point(1, 202), t.map(p));
    }
}
