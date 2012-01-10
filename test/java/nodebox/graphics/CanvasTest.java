package nodebox.graphics;

import junit.framework.TestCase;

public class CanvasTest extends TestCase {

    public void testCloning() {
        Color backgroundColor = new Color(0.1, 0.2, 0.3);
        Canvas c = new Canvas(200, 300);
        c.setBackground(backgroundColor);
        Canvas c2 = c.clone();
        assertEquals(200f, c2.getWidth());
        assertEquals(300f, c2.getHeight());
        assertEquals(backgroundColor, c.getBackground());
    }

}
