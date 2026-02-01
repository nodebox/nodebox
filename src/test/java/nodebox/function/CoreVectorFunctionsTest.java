package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;

import static nodebox.function.CoreVectorFunctions.*;
import static nodebox.graphics.SVGRenderer.*;
import static org.junit.Assert.*;

/**
 * Tests for CoreVectorFunctions with SVG output generation.
 * These tests serve as golden master tests that can be compared with Rust implementations.
 */
public class CoreVectorFunctionsTest {

    private static final String OUTPUT_DIR = "build/test-output/svg";
    private static final Rectangle2D BOUNDS = new Rectangle2D.Float(0, 0, 200, 200);
    private static final Rectangle2D CENTERED_BOUNDS = new Rectangle2D.Float(-100, -100, 200, 200);

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    // ============================================================================
    // Generator Tests
    // ============================================================================

    @Test
    public void testEllipse() {
        Path ellipse = ellipse(new Point(100, 100), 80, 60);
        assertNotNull(ellipse);
        Rect bounds = ellipse.getBounds();
        assertEquals(60.0, bounds.getX(), 0.1);
        assertEquals(70.0, bounds.getY(), 0.1);
        assertEquals(80.0, bounds.getWidth(), 0.1);
        assertEquals(60.0, bounds.getHeight(), 0.1);

        writeSvg("ellipse", ImmutableList.of(ellipse));
    }

    @Test
    public void testRect() {
        Path rect = rect(new Point(100, 100), 80, 60, Point.ZERO);
        assertNotNull(rect);
        Rect bounds = rect.getBounds();
        assertEquals(60.0, bounds.getX(), 0.1);
        assertEquals(70.0, bounds.getY(), 0.1);
        assertEquals(80.0, bounds.getWidth(), 0.1);
        assertEquals(60.0, bounds.getHeight(), 0.1);

        writeSvg("rect", ImmutableList.of(rect));
    }

    @Test
    public void testRectRounded() {
        Path rect = rect(new Point(100, 100), 80, 60, new Point(10, 10));
        assertNotNull(rect);
        // Rounded rect should have curves
        assertTrue(rect.getPointCount() > 4);

        writeSvg("rect_rounded", ImmutableList.of(rect));
    }

    @Test
    public void testLine() {
        Path line = line(new Point(20, 20), new Point(180, 180), 2);
        assertNotNull(line);
        assertEquals(2, line.getPointCount());
        assertNull(line.getFill());
        assertNotNull(line.getStroke());

        writeSvg("line", ImmutableList.of(line));
    }

    @Test
    public void testLineWithPoints() {
        Path line = line(new Point(20, 20), new Point(180, 180), 5);
        assertNotNull(line);
        assertEquals(5, line.getPointCount());

        writeSvg("line_points", ImmutableList.of(line));
    }

    @Test
    public void testLineAngle() {
        Path line = lineAngle(new Point(100, 100), 45, 80, 2);
        assertNotNull(line);

        writeSvg("line_angle", ImmutableList.of(line));
    }

    @Test
    public void testArcPie() {
        Path arc = arc(new Point(100, 100), 80, 80, 0, 90, "pie");
        assertNotNull(arc);

        writeSvg("arc_pie", ImmutableList.of(arc));
    }

    @Test
    public void testArcChord() {
        Path arc = arc(new Point(100, 100), 80, 80, 0, 90, "chord");
        assertNotNull(arc);

        writeSvg("arc_chord", ImmutableList.of(arc));
    }

    @Test
    public void testArcOpen() {
        Path arc = arc(new Point(100, 100), 80, 80, 0, 90, "open");
        assertNotNull(arc);

        writeSvg("arc_open", ImmutableList.of(arc));
    }

    @Test
    public void testArc270() {
        Path arc = arc(new Point(100, 100), 80, 80, 0, 270, "pie");
        assertNotNull(arc);

        writeSvg("arc_270", ImmutableList.of(arc));
    }

    @Test
    public void testGrid() {
        List<Point> points = grid(4, 3, 160, 120, new Point(100, 100));
        assertNotNull(points);
        assertEquals(12, points.size());

        // First point
        assertEquals(20.0, points.get(0).x, 0.1);
        assertEquals(40.0, points.get(0).y, 0.1);

        // Last point
        assertEquals(180.0, points.get(11).x, 0.1);
        assertEquals(160.0, points.get(11).y, 0.1);

        // Visualize grid with small circles
        Geometry geo = new Geometry();
        for (Point p : points) {
            Path circle = new Path();
            circle.ellipse(p.x, p.y, 5, 5);
            geo.add(circle);
        }
        writeSvg("grid", geo);
    }

    @Test
    public void testConnect() {
        List<Point> points = ImmutableList.of(
            new Point(20, 20),
            new Point(100, 180),
            new Point(180, 20)
        );
        Path path = connect(points, true);
        assertNotNull(path);
        assertTrue(path.getContours().get(0).isClosed());

        writeSvg("connect", ImmutableList.of(path));
    }

    @Test
    public void testConnectOpen() {
        List<Point> points = ImmutableList.of(
            new Point(20, 20),
            new Point(100, 180),
            new Point(180, 20)
        );
        Path path = connect(points, false);
        assertNotNull(path);
        assertFalse(path.getContours().get(0).isClosed());

        writeSvg("connect_open", ImmutableList.of(path));
    }

    @Test
    public void testFreehand() {
        Path path = freehand("M0,100 50,50 100,100 M100,100 150,150 200,100");
        assertNotNull(path);
        assertEquals(2, path.getContours().size());

        writeSvg("freehand", ImmutableList.of(path));
    }

    // ============================================================================
    // Filter Tests
    // ============================================================================

    @Test
    public void testAlign() {
        Path rect = rect(new Point(150, 150), 40, 40, Point.ZERO);

        // Align to center
        AbstractGeometry aligned = align(rect, new Point(100, 100), "center", "middle");
        assertNotNull(aligned);
        Point c = centroid((IGeometry) aligned);
        assertEquals(100.0, c.x, 0.1);
        assertEquals(100.0, c.y, 0.1);

        writeSvg("align_center", ImmutableList.of(aligned));
    }

    @Test
    public void testAlignLeftTop() {
        Path rect = rect(new Point(100, 100), 40, 40, Point.ZERO);

        AbstractGeometry aligned = align(rect, new Point(50, 50), "left", "top");
        assertNotNull(aligned);
        Rect bounds = aligned.getBounds();
        assertEquals(50.0, bounds.x, 0.1);
        assertEquals(50.0, bounds.y, 0.1);

        writeSvg("align_left_top", ImmutableList.of(aligned));
    }

    @Test
    public void testCentroid() {
        Path rect = rect(new Point(100, 100), 80, 60, Point.ZERO);
        Point c = centroid(rect);
        assertNotNull(c);
        assertEquals(100.0, c.x, 0.1);
        assertEquals(100.0, c.y, 0.1);
    }

    @Test
    public void testColorize() {
        Path ellipse = ellipse(new Point(100, 100), 80, 80);
        Colorizable colored = colorize(ellipse, new Color(1, 0, 0), new Color(0, 0, 1), 3);

        Path p = (Path) colored;
        assertEquals(new Color(1, 0, 0), p.getFill());
        assertEquals(new Color(0, 0, 1), p.getStroke());
        assertEquals(3.0, p.getStrokeWidth(), 0.01);

        writeSvg("colorize", ImmutableList.of(p));
    }

    @Test
    public void testCopy() {
        Path rect = rect(new Point(20, 100), 15, 15, Point.ZERO);
        rect.setFill(new Color(0.3, 0.5, 0.8));

        List<IGeometry> copies = copy(rect, 8, "tsr", new Point(22, 0), 0, new Point(100, 100));
        assertNotNull(copies);
        assertEquals(8, copies.size());

        Geometry geo = new Geometry();
        for (IGeometry g : copies) {
            geo.add((Path) g);
        }
        writeSvg("copy", geo);
    }

    @Test
    public void testCopyRotate() {
        Path rect = rect(new Point(100, 50), 10, 30, Point.ZERO);
        rect.setFill(new Color(0.8, 0.3, 0.3));

        List<IGeometry> copies = copy(rect, 12, "trs", Point.ZERO, 30, new Point(100, 100));

        Geometry geo = new Geometry();
        for (IGeometry g : copies) {
            geo.add((Path) g);
        }
        writeSvg("copy_rotate", geo);
    }

    @Test
    public void testFit() {
        Path rect = rect(new Point(100, 100), 200, 100, Point.ZERO);

        IGeometry fitted = fit(rect, new Point(100, 100), 80, 80, true);
        assertNotNull(fitted);
        Rect bounds = fitted.getBounds();
        assertTrue(bounds.width <= 80.1);
        assertTrue(bounds.height <= 40.1); // Maintains 2:1 aspect ratio

        // Draw the target bounds for reference
        Path target = rect(new Point(100, 100), 80, 80, Point.ZERO);
        target.setFill(null);
        target.setStroke(new Color(0.8, 0.8, 0.8));
        target.setStrokeWidth(1);

        Geometry geo = new Geometry();
        geo.add(target);
        geo.add((Path) fitted);
        writeSvg("fit", geo);
    }

    @Test
    public void testFitNoProportions() {
        Path rect = rect(new Point(100, 100), 200, 100, Point.ZERO);

        IGeometry fitted = fit(rect, new Point(100, 100), 80, 80, false);
        assertNotNull(fitted);
        Rect bounds = fitted.getBounds();
        assertEquals(80.0, bounds.width, 0.5);
        assertEquals(80.0, bounds.height, 0.5);

        writeSvg("fit_no_proportions", ImmutableList.of((Path) fitted));
    }

    @Test
    public void testGroup() {
        Path rect = rect(new Point(60, 100), 40, 40, Point.ZERO);
        Path ellipse = ellipse(new Point(140, 100), 40, 40);

        Geometry grouped = group(ImmutableList.<IGeometry>of(rect, ellipse));
        assertNotNull(grouped);
        assertEquals(2, grouped.getPaths().size());

        writeSvg("group", grouped);
    }

    @Test
    public void testUngroup() {
        Path rect = rect(new Point(60, 100), 40, 40, Point.ZERO);
        Path ellipse = ellipse(new Point(140, 100), 40, 40);
        Geometry grouped = group(ImmutableList.<IGeometry>of(rect, ellipse));

        List<Path> paths = ungroup(grouped);
        assertEquals(2, paths.size());
    }

    @Test
    public void testSkew() {
        Path rect = rect(new Point(100, 100), 60, 60, Point.ZERO);
        Object skewed = skew(rect, new Point(20, 0), new Point(100, 100));
        assertNotNull(skewed);

        writeSvg("skew", ImmutableList.of((Path) skewed));
    }

    @Test
    public void testSnap() {
        Path rect = rect(new Point(73, 67), 40, 40, Point.ZERO);
        AbstractGeometry snapped = snap(rect, 20, 100, Point.ZERO);
        assertNotNull(snapped);

        // Draw grid for reference
        Geometry geo = new Geometry();
        for (int x = 0; x <= 200; x += 20) {
            Path line = new Path();
            line.line(x, 0, x, 200);
            line.setFill(null);
            line.setStroke(new Color(0.9, 0.9, 0.9));
            line.setStrokeWidth(0.5);
            geo.add(line);
        }
        for (int y = 0; y <= 200; y += 20) {
            Path line = new Path();
            line.line(0, y, 200, y);
            line.setFill(null);
            line.setStroke(new Color(0.9, 0.9, 0.9));
            line.setStrokeWidth(0.5);
            geo.add(line);
        }
        geo.add((Path) snapped);
        writeSvg("snap", geo);
    }

    @Test
    public void testLink() {
        Path rect1 = rect(new Point(40, 100), 40, 60, Point.ZERO);
        Path rect2 = rect(new Point(160, 100), 40, 60, Point.ZERO);

        Path link = link(rect1, rect2, "horizontal");
        assertNotNull(link);

        Geometry geo = new Geometry();
        geo.add(rect1);
        geo.add(rect2);
        geo.add(link);
        writeSvg("link", geo);
    }

    @Test
    public void testLinkVertical() {
        Path rect1 = rect(new Point(100, 40), 60, 40, Point.ZERO);
        Path rect2 = rect(new Point(100, 160), 60, 40, Point.ZERO);

        Path link = link(rect1, rect2, "vertical");
        assertNotNull(link);

        Geometry geo = new Geometry();
        geo.add(rect1);
        geo.add(rect2);
        geo.add(link);
        writeSvg("link_vertical", geo);
    }

    @Test
    public void testPointOnPath() {
        Path ellipse = ellipse(new Point(100, 100), 80, 80);

        Geometry geo = new Geometry();
        geo.add(ellipse);

        // Sample points along the path
        for (int i = 0; i <= 10; i++) {
            double t = i * 10.0;
            Point p = pointOnPath(ellipse, t);
            Path marker = new Path();
            marker.ellipse(p.x, p.y, 6, 6);
            marker.setFill(new Color(1, 0, 0));
            geo.add(marker);
        }

        writeSvg("point_on_path", geo);
    }

    @Test
    public void testToPoints() {
        Path ellipse = ellipse(new Point(100, 100), 80, 80);
        List<Point> points = toPoints(ellipse);
        assertNotNull(points);
        assertTrue(points.size() > 0);
    }

    @Test
    public void testTextpath() {
        Path text = textpath("Hello", "Helvetica", 24, "CENTER", new Point(100, 100), 200);
        assertNotNull(text);

        writeSvg("textpath", ImmutableList.of(text));
    }

    @Test
    public void testMakePoint() {
        Point p = makePoint(50, 75);
        assertEquals(50.0, p.x, 0.01);
        assertEquals(75.0, p.y, 0.01);
    }

    // ============================================================================
    // Complex Examples
    // ============================================================================

    @Test
    public void testSpiral() {
        // Create a spiral using copy with rotation and scaling
        Path rect = rect(new Point(100, 20), 8, 3, Point.ZERO);
        rect.setFill(new Color(0.2, 0.4, 0.8));

        List<IGeometry> copies = copy(rect, 50, "trs", new Point(0, 2), 12, new Point(98, 100));

        Geometry geo = new Geometry();
        for (IGeometry g : copies) {
            geo.add((Path) g);
        }
        writeSvg("spiral", geo);
    }

    @Test
    public void testRadialPattern() {
        // Create a radial pattern
        Path petal = new Path();
        petal.ellipse(100, 60, 20, 60);
        petal.setFill(new Color(0.9, 0.4, 0.4, 0.7));

        List<IGeometry> copies = copy(petal, 8, "trs", Point.ZERO, 45, new Point(100, 100));

        // Add center
        Path center = ellipse(new Point(100, 100), 30, 30);
        center.setFill(new Color(0.9, 0.8, 0.2));

        Geometry geo = new Geometry();
        for (IGeometry g : copies) {
            geo.add((Path) g);
        }
        geo.add(center);
        writeSvg("radial_pattern", geo);
    }

    @Test
    public void testGridOfShapes() {
        List<Point> points = grid(5, 5, 160, 160, new Point(100, 100));

        Geometry geo = new Geometry();
        int i = 0;
        for (Point p : points) {
            float hue = (float) i / points.size();
            Path shape = (i % 2 == 0)
                ? rect(p, 20, 20, Point.ZERO)
                : ellipse(p, 20, 20);
            shape.setFill(Color.fromHSB(hue, 0.7, 0.9));
            geo.add(shape);
            i++;
        }
        writeSvg("grid_of_shapes", geo);
    }

    @Test
    public void testAllArcTypes() {
        // Show all arc types in a single image
        Geometry geo = new Geometry();

        // Pie arc
        Path arcPie = arc(new Point(50, 50), 60, 60, 0, 120, "pie");
        arcPie.setFill(new Color(0.8, 0.2, 0.2));
        geo.add(arcPie);

        // Chord arc
        Path arcChord = arc(new Point(150, 50), 60, 60, 0, 120, "chord");
        arcChord.setFill(new Color(0.2, 0.8, 0.2));
        geo.add(arcChord);

        // Open arc
        Path arcOpen = arc(new Point(50, 150), 60, 60, 0, 120, "open");
        arcOpen.setFill(null);
        arcOpen.setStroke(new Color(0.2, 0.2, 0.8));
        arcOpen.setStrokeWidth(3);
        geo.add(arcOpen);

        // Elliptical arc
        Path arcElliptical = arc(new Point(150, 150), 60, 40, 30, 270, "pie");
        arcElliptical.setFill(new Color(0.8, 0.5, 0.2));
        geo.add(arcElliptical);

        writeSvg("all_arc_types", geo);
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private void writeSvg(String name, Object geometry) {
        File file = new File(OUTPUT_DIR, name + ".svg");
        if (geometry instanceof Geometry) {
            renderToFile(ImmutableList.of(geometry), BOUNDS, file);
        } else if (geometry instanceof Iterable) {
            renderToFile((Iterable<?>) geometry, BOUNDS, file);
        } else {
            renderToFile(ImmutableList.of(geometry), BOUNDS, file);
        }
    }
}
