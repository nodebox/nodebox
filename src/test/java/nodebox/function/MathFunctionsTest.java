package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.node.*;
import nodebox.util.Assertions;
import org.junit.Test;

import java.util.List;

import static nodebox.function.MathFunctions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathFunctionsTest {

    private final FunctionLibrary mathLibrary = MathFunctions.LIBRARY;
    private final FunctionRepository functions = FunctionRepository.of(mathLibrary);
    private final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);

    private List<?> renderNode(Node node) {
        return new NodeContext(testLibrary.withRoot(node)).renderNode("/");
    }

    @Test
    public void testInvertExists() {
        assertTrue(functions.hasFunction("math/negate"));
        assertTrue(mathLibrary.hasFunction("negate"));
        Function function = functions.getFunction("math/negate");
        assertEquals("negate", function.getName());
    }

    @Test(expected = NodeRenderException.class)
    public void testCallInvertWithNoArguments() {
        Node invertNode = Node.ROOT.withFunction("math/negate");
        renderNode(invertNode);
    }

    @Test
    public void testCallInvert() {
        Node invertNode = Node.ROOT
                .withFunction("math/negate")
                .withInputAdded(Port.floatPort("value", 5));
        assertEquals(ImmutableList.of(-5.0), renderNode(invertNode));
    }

    /**
     * Test if the insertion order of the ports is respected.
     * <p/>
     * This method tests a non-commutative operation in two directions to see if both work.
     */
    @Test
    public void testPortOrder() {
        Node subtract1 = Node.ROOT
                .withFunction("math/subtract")
                .withInputAdded(Port.floatPort("a", 10))
                .withInputAdded(Port.floatPort("b", 3));
        assertEquals(ImmutableList.of(7.0), renderNode(subtract1));

        Node subtract2 = Node.ROOT
                .withName("subtract2")
                .withFunction("math/subtract")
                .withInputAdded(Port.floatPort("b", 3))
                .withInputAdded(Port.floatPort("a", 10));
        assertEquals(ImmutableList.of(-7.0), renderNode(subtract2));
    }

    @Test
    public void testAbs() {
        assertEquals(0.0, MathFunctions.abs(0.0), 0.0);
        assertEquals(42.0, MathFunctions.abs(42.0), 0.0);
        assertEquals(42.0, MathFunctions.abs(-42.0), 0.0);
    }

    @Test
    public void testMod() {
        assertEquals(0.0, MathFunctions.mod(10, 2), 0.0);
        assertEquals(3.0, MathFunctions.mod(10, 7), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModByZero() {
        MathFunctions.mod(10, 0);
    }

    @Test
    public void testSum() {
        assertEquals(0.0, MathFunctions.sum(null), 0.001);
        assertEquals(0.0, MathFunctions.sum(ImmutableList.<Double>of()), 0.001);
        assertEquals(6.0, MathFunctions.sum(ImmutableList.of(1.0, 2.0, 3.0)), 0.001);
        assertEquals(-6.0, MathFunctions.sum(ImmutableList.of(-1.0, -2.0, -3.0)), 0.001);
    }

    @Test
    public void testMax() {
        assertEquals(0.0, MathFunctions.max(ImmutableList.<Double>of()), 0.001);
        assertEquals(3.0, MathFunctions.max(ImmutableList.of(1.0, 2.0, 3.0)), 0.001);
        assertEquals(-1.0, MathFunctions.max(ImmutableList.of(-1.0, -2.0, -3.0)), 0.001);
    }

    @Test
    public void testMin() {
        assertEquals(0.0, MathFunctions.min(ImmutableList.<Double>of()), 0.001);
        assertEquals(1.0, MathFunctions.min(ImmutableList.of(1.0, 2.0, 3.0)), 0.001);
        assertEquals(-3.0, MathFunctions.min(ImmutableList.of(-1.0, -2.0, -3.0)), 0.001);
    }

    @Test
    public void testRandomNumbers() {
        List<Double> numbers = MathFunctions.randomNumbers(3, -10.0, 10.0, 42);
        assertEquals(3.89263, numbers.get(0), 0.001);
        assertEquals(4.95359, numbers.get(1), 0.001);
        assertEquals(4.66839, numbers.get(2), 0.001);
    }

    @Test
    public void testSample() {
        Assertions.assertResultsEqual(sample(0, 1, 2));
        Assertions.assertResultsEqual(sample(1, 100, 200), 150.0);
        Assertions.assertResultsEqual(sample(2, 100, 200), 100.0, 200.0);
        Assertions.assertResultsEqual(sample(3, 100, 200), 100.0, 150.0, 200.0);
        Assertions.assertResultsEqual(sample(4, 100, 250), 100.0, 150.0, 200.0, 250.0);
        Assertions.assertResultsEqual(sample(3, 200, 100), 200.0, 150.0, 100.0);
        Assertions.assertResultsEqual(sample(3, 1, 1), 1.0, 1.0, 1.0);
        List<Double> values = sample(1000, 0, 100);
        double lastValue = values.get(values.size() - 1);
        assertEquals("The last value needs to be exactly 100.", 100.0, lastValue, 0.0);
        assertTrue("The last value needs to be exactly 100.", lastValue <= 100.0);
    }

    @Test
    public void testConvertRange() {
        assertEquals(0.5, convertRange(50, 0, 100, 0, 1, OVERFLOW_IGNORE), 0.001);
        assertEquals(0.25, convertRange(75, 0, 100, 1, 0, OVERFLOW_IGNORE), 0.001);
    }

    @Test
    public void testRound() {
        assertEquals(5L, round(5.0));
        assertEquals(5L, round(5.4));
        assertEquals(6L, round(5.5));
        assertEquals(6L, round(5.9));
    }

}
