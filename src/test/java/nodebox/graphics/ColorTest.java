package nodebox.graphics;

import org.junit.Test;

import static junit.framework.TestCase.*;

public class ColorTest {

    @Test
    public void testConstructors() {
        Color c = new Color(1, 0, 0);
        assertTrue(c.isVisible());
        assertEquals(1.0, c.getAlpha());
    }

    @Test
    public void testHex() {
        assertEquals(new Color("#000"), Color.BLACK);
        assertEquals(new Color("#f00"), new Color(1, 0, 0));
        assertEquals(new Color("#ffffff"), Color.WHITE);
    }
}
