package nodebox.graphics;

import junit.framework.TestCase;

import java.util.Iterator;

public class PointTest extends TestCase {

    public void testIterator() {
        Point pt = new Point(22, 33);
        Iterator<Float> iter = pt.iterator();
        assertEquals(22f, iter.next());
        assertEquals(33f, iter.next());
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should have thrown an exception");
        } catch (Exception ignored) {
        }
    }
}
