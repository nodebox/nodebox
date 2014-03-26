package nodebox.graphics;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PointTest {

    @Test
    public void testMoved() {
        Point p1 = new Point(10, 20);
        Point p2 = p1.moved(5, 7);
        assertEquals(p1.x, 10.0);
        assertEquals(p1.y, 20.0);
        assertEquals(p2.x, 15.0);
        assertEquals(p2.y, 27.0);
    }

}
