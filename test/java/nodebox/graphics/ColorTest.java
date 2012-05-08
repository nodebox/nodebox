package nodebox.graphics;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ColorTest {

    @Test
    public void testConstructors() {
        Color c = new Color(1, 0, 0);
        assertTrue(c.isVisible());
        assertEquals(1.0, c.getAlpha());
    }
}
