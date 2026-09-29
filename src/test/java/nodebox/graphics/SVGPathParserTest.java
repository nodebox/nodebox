package nodebox.graphics;

import nodebox.client.PythonUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the bundled Python SVG path parser (src/main/python/svg).
 * <p>
 * These paths are parsed by Jython, so Python 2 semantics apply: math.ceil
 * returns a float and range() rejects floats.
 */
public class SVGPathParserTest {

    private static PythonInterpreter interpreter;

    @BeforeClass
    public static void setUpPython() {
        PythonUtils.initializePython();
        interpreter = new PythonInterpreter();
        interpreter.exec("import svg");
    }

    private Path parsePath(String d) {
        interpreter.set("d", d);
        PyObject result = interpreter.eval("svg.path_from_string(d)");
        return (Path) result.__tojava__(Path.class);
    }

    /**
     * An elliptical arc used to raise "TypeError: range() integer end argument
     * expected, got float." because arcToSegments passed math.ceil's float
     * straight into range().
     */
    @Test
    public void testEllipticalArc() {
        Path p = parsePath("M0,0 A10,10 0 0,1 10,10");
        assertTrue("Arc should produce at least one curve segment", p.getPointCount() > 1);
        assertPointEquals(0, 0, p.getPoints().get(0));
        assertPointEquals(10, 10, p.getPoints().get(p.getPointCount() - 1));
    }

    /**
     * Arcs sweeping more than 90 degrees are split into several segments, so
     * this covers the case where ceil() returns something other than 1.0.
     */
    @Test
    public void testLargeArc() {
        Path p = parsePath("M0,0 A10,10 0 1,1 10,10");
        assertTrue("Large arc should be split into multiple segments", p.getPointCount() > 4);
        assertPointEquals(10, 10, p.getPoints().get(p.getPointCount() - 1));
    }

    /**
     * Arc flags may be packed against each other and against the coordinate
     * that follows, which is what SVG optimizers emit. The shorthand regex
     * used to accept only integer radii, so decimal radii were misparsed.
     */
    @Test
    public void testArcWithCompactFlags() {
        Path p = parsePath("M10,10a4.368,4.368 0 011.236,3.22");
        assertPointEquals(11.236, 13.22, p.getPoints().get(p.getPointCount() - 1));

        Path negative = parsePath("M10,10a4.368,4.368 0 01-1.236-3.22");
        assertPointEquals(8.764, 6.78, negative.getPoints().get(negative.getPointCount() - 1));

        Path leadingDot = parsePath("M10,10a.5.5 0 011,2");
        assertPointEquals(11, 12, leadingDot.getPoints().get(leadingDot.getPointCount() - 1));

        Path absolute = parsePath("M10,10A4.368,4.368 0 0111.236,13.22");
        assertPointEquals(11.236, 13.22, absolute.getPoints().get(absolute.getPointCount() - 1));
    }

    /**
     * The spelling Illustrator produces: flags separated, but the following
     * coordinate packed against the sweep flag.
     */
    @Test
    public void testArcWithPackedCoordinates() {
        Path p = parsePath("M10,10a4.368,4.368 0 0 1-1.236-3.22");
        assertPointEquals(8.764, 6.78, p.getPoints().get(p.getPointCount() - 1));
    }

    /**
     * Quadratic curves used to raise NameError because the 'q' branch called
     * g.curveto() instead of path.curveto().
     */
    @Test
    public void testQuadraticCurve() {
        Path p = parsePath("M0,0 Q50,100 100,0");
        assertTrue("Quadratic curve should produce a curve segment", p.getPointCount() > 1);
        assertPointEquals(100, 0, p.getPoints().get(p.getPointCount() - 1));
    }

    @Test
    public void testSmoothQuadraticCurve() {
        Path p = parsePath("M0,0 Q50,100 100,0 T200,0");
        assertPointEquals(200, 0, p.getPoints().get(p.getPointCount() - 1));
    }

    /**
     * Illustrator omits the separator between a number ending in a decimal and
     * the next number starting with one, giving runs like ".812.232.456".
     * Splitting these used to consume the separating dot, so only every other
     * boundary was split and the leftovers failed to parse as floats.
     */
    @Test
    public void testConcatenatedDecimals() {
        Path two = parsePath("M0,0L.812.232");
        assertPointEquals(0.812, 0.232, two.getPoints().get(1));

        Path four = parsePath("M0,0L.812.232.456.789");
        assertEquals(3, four.getPointCount());
        assertPointEquals(0.812, 0.232, four.getPoints().get(1));
        assertPointEquals(0.456, 0.789, four.getPoints().get(2));

        Path many = parsePath("M0,0L1.084.413.578.708.037.5");
        assertEquals(4, many.getPointCount());
        assertPointEquals(1.084, 0.413, many.getPoints().get(1));
        assertPointEquals(0.578, 0.708, many.getPoints().get(2));
        assertPointEquals(0.037, 0.5, many.getPoints().get(3));
    }

    /**
     * Commands the parser already handled, to catch regressions in the shared
     * tokenizer.
     */
    @Test
    public void testLinesAndCurves() {
        Path line = parsePath("M0,0 L100,50");
        assertEquals(2, line.getPointCount());
        assertPointEquals(100, 50, line.getPoints().get(1));

        Path curve = parsePath("M0,0 C10,20 30,40 50,60");
        assertPointEquals(50, 60, curve.getPoints().get(curve.getPointCount() - 1));
    }

    private static void assertPointEquals(double x, double y, nodebox.graphics.Point p) {
        assertEquals(x, p.x, 0.001);
        assertEquals(y, p.y, 0.001);
    }

}