package nodebox.graphics;

import org.junit.Test;

import java.awt.*;

import static junit.framework.Assert.assertEquals;

public class GrobTest {

    public class TestGrob extends AbstractGrob {

        private float x, y, width, height;

        public TestGrob(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void draw(Graphics2D g) {
        }

        public boolean isEmpty() {
            return false;
        }

        public Rect getBounds() {
            return getTransform().map(new Rect(x, y, width, height));
        }

        public Grob clone() {
            return new TestGrob(x, y, width, height);
        }
    }

    @Test
    public void testTransform() {
        TestGrob tg = new TestGrob(1, 2, 3, 4);
        assertEquals(new Rect(1, 2, 3, 4), tg.getBounds());
        tg.translate(200, 300);
        assertEquals(new Rect(201, 302, 3, 4), tg.getBounds());

    }
}
