package nodebox.graphics;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

public class TransformTest {

    @Test
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

    @Test
    public void testMapPath() {
        Path p = new Path();
        p.addPoint(0, 0);
        p.addPoint(10, 20);
        Transform t = new Transform();
        t.translate(10, 5);
        Path newPath = t.map(p);

        List<Point> oldPoints = p.getPoints();
        assertEquals(new Point(0, 0), oldPoints.get(0));
        assertEquals(new Point(10, 20), oldPoints.get(1));

        List<Point> newPoints = newPath.getPoints();
        assertEquals(new Point(10, 5), newPoints.get(0));
        assertEquals(new Point(20, 25), newPoints.get(1));
    }

}