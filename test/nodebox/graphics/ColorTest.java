package nodebox.graphics;

import junit.framework.TestCase;

public class ColorTest extends TestCase {

    public void testConstructors() {
        Color c = new Color(1, 0, 0);
        assertTrue(c.isVisible());
        assertEquals(1.0, c.getAlpha());
    }
}
