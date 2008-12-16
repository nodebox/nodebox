package net.nodebox.graphics;

import junit.framework.TestCase;

import java.awt.*;

public class GrobTest extends TestCase {

    public class TestGrob extends Grob {

        private double x, y, width, height;

        public TestGrob(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void draw(Graphics2D g) {
        }

        public Rect getBounds() {
            return new Rect(x, y, width, height);
        }

        public Grob clone() {
            return new TestGrob(x, y, width, height);
        }
    }

    public void testFrame() {
        TestGrob tg = new TestGrob(1, 2, 3, 4);
        assertEquals(new Rect(1, 2, 3, 4), tg.getBounds());
        tg.translate(200, 300);
        // Bounds don't change, but frame does.
        assertEquals(new Rect(1, 2, 3, 4), tg.getBounds());
        assertEquals(new Rect(201, 302, 3, 4), tg.getFrame());

    }
}
