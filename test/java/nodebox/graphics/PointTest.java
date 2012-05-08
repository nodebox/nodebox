package nodebox.graphics;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PointTest {

    @Test
    public void testMap() {
        Point pt = new Point(22, 33);
        assertEquals(22.0, pt.get("x"));
        assertEquals(33.0, pt.get("y"));
        assertEquals(1, pt.get("type"));
    }

}
