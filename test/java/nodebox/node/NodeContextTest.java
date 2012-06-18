package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.function.CoreVectorFunctions;
import nodebox.function.FunctionRepository;
import nodebox.function.ListFunctions;
import nodebox.function.MathFunctions;
import nodebox.function.StringFunctions;
import nodebox.function.TestFunctions;
import nodebox.graphics.Point;
import nodebox.util.SideEffects;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static nodebox.util.Assertions.assertResultsEqual;

public class NodeContextTest {

    public static final Node numberNode = Node.ROOT
            .withName("number")
            .withFunction("math/number")
            .withInputAdded(Port.floatPort("number", 0));

    public static final Node valuesToPointNode = Node.ROOT
            .withName("values_to_point")
            .withFunction("corevector/makePoint")
            .withOutputType("point")
            .withInputAdded(Port.floatPort("x", 0))
            .withInputAdded(Port.floatPort("y", 0));

    public static final Node addNode = Node.ROOT
            .withName("add")
            .withFunction("math/add")
            .withInputAdded(Port.floatPort("v1", 0.0))
            .withInputAdded(Port.floatPort("v2", 0.0));

    public static final Node invertNode = Node.ROOT
            .withName("negate")
            .withFunction("math/negate")
            .withInputAdded(Port.floatPort("value", 0.0));

    public static final Node makeNumbersNode = Node.ROOT
            .withName("makeNumbers")
            .withFunction("math/makeNumbers")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.stringPort("string", ""))
            .withInputAdded(Port.stringPort("separator", " "));

    public static final Node threeNumbers = makeNumbersNode
            .extend()
            .withName("threeNumbers")
            .withInputValue("string", "1 2 3");

    public static final Node fiveNumbers = makeNumbersNode
            .extend()
            .withName("fiveNumbers")
            .withInputValue("string", "100 200 300 400 500");

    public static final Node sampleNode = Node.ROOT
            .withName("sample")
            .withFunction("math/sample")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.intPort("amount", 10))
            .withInputAdded(Port.floatPort("start", 0))
            .withInputAdded(Port.floatPort("end", 100));

    public static final Node makeStringsNode = Node.ROOT
            .withName("makeStrings")
            .withFunction("string/makeStrings")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.stringPort("string", "Alpha;Beta;Gamma"))
            .withInputAdded(Port.stringPort("sep", ";"));

    public static final Node calculateMultipleNode = Node.ROOT
            .withName("calculateMultiple")
            .withFunction("test/calculateMultipleArgs")
            .withInputAdded(Port.floatPort("v1", 0))
            .withInputAdded(Port.floatPort("v2", 0))
            .withInputAdded(Port.floatPort("v3", 0))
            .withInputAdded(Port.floatPort("v4", 0));

    public static final FunctionRepository functions = FunctionRepository.of(CoreVectorFunctions.LIBRARY, MathFunctions.LIBRARY, ListFunctions.LIBRARY, StringFunctions.LIBRARY, SideEffects.LIBRARY, TestFunctions.LIBRARY);
    public static final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);
    private NodeContext context;

    @Before
    public void setUp() throws Exception {
        context = new NodeContext(testLibrary);
        SideEffects.reset();
    }

    @Test
    public void testSingleOutput() {
        context.renderNode(valuesToPointNode);
        Map<Node, Iterable<?>> resultsMap = context.getResultsMap();
        assertEquals(1, resultsMap.size());
        Iterable<?> results = context.getResults(valuesToPointNode);
        List resultsList = ImmutableList.copyOf(results);
        assertEquals(1, resultsList.size());
        assertResultsEqual(resultsList, Point.ZERO);
    }

    @Test
    public void testSameOutputPort() {
        Node invert1 = invertNode.extend().withName("invert1").withInputValue("value", 1.0);
        Node invert2 = invertNode.extend().withName("invert2").withInputValue("value", 10.0);
        assertResultsEqual(context.renderNode(invert1), -1.0);
        assertResultsEqual(context.renderNode(invert2), -10.0);
    }

    @Test
    public void testListAwareProcessing() {
        Node makeNumbers1 = makeNumbersNode.extend().withInputValue("string", "1 2 3 4");
        assertResultsEqual(context.renderNode(makeNumbers1), 1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void testListUnawareProcessing() {
        Node invert1 = invertNode.extend().withName("invert1").withInputValue("value", 42.0);
        assertResultsEqual(context.renderNode(invert1), -42.0);
    }

    @Test
    public void testConnectedListProcessing() {
        Node makeNumbers1 = makeNumbersNode.extend().withName("makeNumbers1").withInputValue("string", "1 2 3 4");
        Node invert1 = invertNode.extend().withName("invert1");
        Node net = Node.ROOT
                .withChildAdded(makeNumbers1)
                .withChildAdded(invert1)
                .connect("makeNumbers1", "invert1", "value")
                .withRenderedChildName("invert1");
        assertResultsEqual(context.renderChild(net, invert1), -1.0, -2.0, -3.0, -4.0);
    }

    @Test
    public void testEmptyListProcessing() {
        Node noNumbers = makeNumbersNode.extend().withName("noNumbers").withInputValue("string", "");
        Node add1 = addNode.extend().withName("add1");
        Node net = Node.ROOT
                .withChildAdded(noNumbers)
                .withChildAdded(add1)
                .connect("noNumbers", "add1", "v1");
        context.renderChild(net, add1);
        assertResultsEqual(context.renderChild(net, add1));
    }

    /**
     * Some nodes are not "pure" but produce side-effects, for example by fetching from an input device
     * or writing to an output device. Those nodes typically do not have inputs or outputs.
     */
    @Test
    public void testInputSideEffect() {
        Node getNumberNode = Node.ROOT
                .withFunction("side-effects/getNumber");
        SideEffects.theInput = 42;
        assertResultsEqual(context.renderNode(getNumberNode), 42L);
    }

    @Test
    public void testOutputSideEffect() {
        Node setNumberNode = Node.ROOT
                .withFunction("side-effects/setNumber")
                .withInputAdded(Port.intPort("number", 42));
        context.renderNode(setNumberNode);
        assertEquals(SideEffects.theOutput, 42L);
    }

    @Test
    public void testSamePrototypeTwice() {
        Node invert1Node = invertNode.withName("invert1").withInputValue("value", 42.0);
        Node invert2Node = invertNode.withName("invert2");
        Node net = Node.ROOT
                .withChildAdded(invert1Node)
                .withChildAdded(invert2Node)
                .connect("invert1", "invert2", "value");
        assertResultsEqual(context.renderChild(net, invert2Node), 42.0);
    }

    /**
     * Test that the node function is executed the exact amount we expect.
     */
    @Test
    public void testExecuteAmount() {
        Node makeNumbers1 = makeNumbersNode.withName("makeNumbers1").withInputValue("string", "1 2 3");
        Node incNode = Node.ROOT
                .withName("inc")
                .withFunction("side-effects/increaseAndCount")
                .withInputAdded(Port.floatPort("number", 0));
        Node net = Node.ROOT
                .withChildAdded(makeNumbers1)
                .withChildAdded(incNode)
                .connect("makeNumbers1", "inc", "number");
        context.renderChild(net, incNode);
        assertEquals(3, SideEffects.theCounter);
        Iterable<?> results = context.getResults(incNode);
        assertResultsEqual(results, 2.0, 3.0, 4.0);
    }

    /**
     * Test the combination of a list input and port value.
     */
    @Test
    public void testListWithValue() {
        Node makeNumbers1 = makeNumbersNode.withName("makeNumbers1").withInputValue("string", "1 2 3");
        Node add1 = addNode.extend().withName("add1").withInputValue("v2", 100.0);
        Node net = Node.ROOT
                .withChildAdded(makeNumbers1)
                .withChildAdded(add1)
                .connect("makeNumbers1", "add1", "v1");
        assertResultsEqual(context.renderChild(net, add1), 101.0, 102.0, 103.0);
    }

    @Test
    public void testLongestList() {
        Node net = Node.ROOT
                .withChildAdded(threeNumbers)
                .withChildAdded(fiveNumbers)
                .withChildAdded(addNode)
                .connect("threeNumbers", addNode.getName(), "v1")
                .connect("fiveNumbers", addNode.getName(), "v2");
        assertResultsEqual(context.renderChild(net, addNode), 101.0, 202.0, 303.0, 401.0, 502.0);
    }

    @Test
    public void testPortRangeMatching() {
        Node sum = Node.ROOT
                .withName("sum")
                .withFunction("math/sum")
                .withInputAdded(Port.floatPort("numbers", 0))
                .withInputRange("numbers", Port.Range.LIST);
        Node net = Node.ROOT
                .withChildAdded(sum)
                .withChildAdded(threeNumbers)
                .connect("threeNumbers", sum.getName(), "numbers");
        assertResultsEqual(context.renderChild(net, sum), 6.0);
    }

    /**
     * Returned lists should not contain nulls.
     */
    @Test
    public void testListWithNulls() {
        Node makeNull = Node.ROOT
                .withName("makeNull")
                .withFunction("test/makeNull")
                .withInputAdded(Port.floatPort("value", 0.0));
        Node net = Node.ROOT
                .withChildAdded(threeNumbers)
                .withChildAdded(makeNull)
                .connect("threeNumbers", "makeNull", "value");
        assertResultsEqual(net, makeNull);
    }

    @Test
    public void testNestedLists() {
        Node makeStrings = makeStringsNode.extend()
                .withInputValue("string", "1,2;3,4;5,6");
        Node makeNumbers = makeNumbersNode.extend()
                .withName("makeNumbers")
                .withInputValue("separator", ",");
        Node net = Node.ROOT
                .withChildAdded(makeStrings)
                .withChildAdded(makeNumbers)
                .withRenderedChildName("makeNumbers")
                .connect("makeStrings", "makeNumbers", "string");
        assertResultsEqual(context.renderNode(net), ImmutableList.of(1.0, 2.0), ImmutableList.of(3.0, 4.0), ImmutableList.of(5.0, 6.0));
    }

    @Test
    public void testNestedListsLevel0() {
        Node net = createNestedNetwork("3,4", "2,3,4");
        assertResultsEqual(context.renderNode(net), 22.0, 29.0, 34.0);
    }

    @Test
    public void testNestedListsLevel1() {
        Node net = createNestedNetwork("3,4;1,8", "2,3,4");
        assertResultsEqual(context.renderNode(net), ImmutableList.of(22.0, 29.0, 34.0), ImmutableList.of(20.0, 33.0, 32.0));
    }

    @Test
    public void testNestedListsLevel2() {
        Node net = createNestedNetwork("3,4;1,8", "1,2;2,3,4");
        assertResultsEqual(context.renderNode(net),
                ImmutableList.of(
                        ImmutableList.of(16.0, 23.0), ImmutableList.of(22.0, 29.0, 34.0)),
                ImmutableList.of(
                        ImmutableList.of(14.0, 27.0), ImmutableList.of(20.0, 33.0, 32.0)));
    }

    @Test
    public void testNestedListAsSingleValue() {
        Node sample1 = createSampleNode("sample1", 4, 3.0, 9.0);
        Node sample2 = createSampleNode("sample2", 4, 9.0, 24.0);
        Node sample3 = sampleNode.extend()
                .withName("sample3")
                .withInputValue("amount", 4);
        Node net = Node.ROOT
                .withChildAdded(sample1)
                .withChildAdded(sample2)
                .withChildAdded(sample3)
                .withRenderedChildName("sample3")
                .connect("sample1", "sample3", "start")
                .connect("sample2", "sample3", "end");
        assertResultsEqual(context.renderNode(net),
                ImmutableList.of(3.0, 5.0, 7.0, 9.0),
                ImmutableList.of(5.0, 8.0, 11.0, 14.0),
                ImmutableList.of(7.0, 11.0, 15.0, 19.0),
                ImmutableList.of(9.0, 14.0, 19.0, 24.0));
        Node sample4 = createSampleNode("sample4", 2, 2.0, 3.0);
        Node slice = Node.ROOT
                .withName("slice")
                .withFunction("list/slice")
                .withOutputRange(Port.Range.LIST)
                .withInputAdded(Port.floatPort("list", 0))
                .withInputRange("list", Port.Range.LIST)
                .withInputAdded(Port.intPort("start_index", 1))
                .withInputAdded(Port.intPort("size", 2));
        net = net.withChildAdded(slice)
                .withChildAdded(sample4)
                .withRenderedChildName("slice")
                .connect("sample3", "slice", "list")
                .connect("sample4", "slice", "size");
        Iterable<?> results = context.renderNode(net);
        assertEquals(4, Iterables.size(results));
        assertResultsEqual((Iterable<?>) Iterables.get(results, 0), 5.0, 7.0);
        assertResultsEqual((Iterable<?>) Iterables.get(results, 1), 8.0, 11.0, 14.0);
        assertResultsEqual((Iterable<?>) Iterables.get(results, 2), 11.0, 15.0);
        assertResultsEqual((Iterable<?>) Iterables.get(results, 3), 14.0, 19.0, 24.0);
    }

    @Test
    public void testRenderSubnetwork() {
        Node subnet = createSubnetwork("subnet1", 1.0, 2.0);
        Node net = Node.ROOT
                .withChildAdded(subnet)
                .withRenderedChildName("subnet1");
        context.renderNetwork(net);
        assertResultsEqual(context.getResults(subnet), 3.0);
    }

    @Test
    public void testValidSubnetworkResults() {
        Node subnet1 = createSubnetwork("subnet1", 1.0, 2.0);
        Node subnet2 = createSubnetwork("subnet2", 3.0, 4.0);
        Node add1 = addNode.extend().withName("add1");
        Node net = Node.ROOT
                .withChildAdded(subnet1)
                .withChildAdded(subnet2)
                .withChildAdded(add1)
                .withRenderedChildName("add1")
                .connect("subnet1", "add1", "v1")
                .connect("subnet2", "add1", "v2");
        context.renderNetwork(net);
        assertResultsEqual(context.getResults(add1), 10.0);
    }

    @Test
    public void testRenderNetworkWithPublishedPort() {
        Node subnet = createSubnetwork("subnet1", 0.0, 0.0)
                .publish("number1", "number", "value1")
                .publish("number2", "number", "value2")
                .withInputValue("value1", 2.0)
                .withInputValue("value2", 3.0);
        Node net = Node.ROOT
                .withChildAdded(subnet)
                .withRenderedChildName("subnet1");
        context.renderNetwork(net);
        assertResultsEqual(context.getResults(subnet), 5.0);
    }

    @Test
    public void testRenderNetworkWithConnectedPublishedPort() {
        Node subnet = createSubnetwork("subnet1", 0.0, 1.0)
                .publish("number1", "number", "value1")
                .withInputValue("value1", 2.0);
        Node number1 = numberNode.extend()
                .withName("number1")
                .withInputValue("number", 10.0);
        Node net = Node.ROOT
                .withChildAdded(subnet)
                .withChildAdded(number1)
                .withRenderedChildName("subnet1")
                .connect("number1", "subnet1", "value1");
        context.renderNetwork(net);
        assertResultsEqual(context.getResults(subnet), 11.0);
    }

    @Test
    public void testFrame() {
        Node frame = Node.ROOT
                .withName("frame")
                .withFunction("core/frame")
                .withInputAdded(Port.customPort("context", "context"));
        assertResultsEqual(frame, 1.0);
    }

    // TODO Check list-aware node with no inputs.
    // TODO Check list-aware node with no outputs.
    // TODO Check list-aware node with single output.
    // TODO Check list-aware node with multiple outputs.

    // TODO Check list-unaware node with single output.
    // TODO Check list-unaware node with multiple outputs.
    // TODO Check list-unaware node with multiple inputs, single output.

    private Node createSubnetwork(String name, double v1, double v2) {
        Node number1 = numberNode.extend()
                .withName("number1")
                .withInputValue("number", v1);
        Node number2 = numberNode.extend()
                .withName("number2")
                .withInputValue("number", v2);
        Node add1 = addNode.extend().withName("add1");
        Node subnet = Node.ROOT
                .withName(name)
                .withChildAdded(number1)
                .withChildAdded(number2)
                .withChildAdded(add1)
                .withRenderedChildName("add1")
                .connect("number1", "add1", "v1")
                .connect("number2", "add1", "v2");
        return subnet;
    }

    private Node createSampleNode(String name, int amount, double start, double end) {
        return sampleNode.extend()
                .withName(name)
                .withInputValue("amount", amount)
                .withInputValue("start", start)
                .withInputValue("end", end);
    }

    private Node createNestedNetwork(String makeStrings1Value, String makeStrings2Value) {
        Node makeStrings1 = makeStringsNode.extend()
                .withName("makeStrings1")
                .withInputValue("string", makeStrings1Value);
        Node makeStrings2 = makeStringsNode.extend()
                .withName("makeStrings2")
                .withInputValue("string", makeStrings2Value);
        Node makeNumbers1 = makeNumbersNode.extend()
                .withName("makeNumbers1")
                .withInputValue("separator", ",");
        Node makeNumbers2 = makeNumbersNode.extend()
                .withName("makeNumbers2")
                .withInputValue("separator", ",");
        Node calculate = calculateMultipleNode.extend()
                .withInputValue("v2", 6.0)
                .withInputValue("v4", 7.0);
        Node net = Node.ROOT
                .withChildAdded(makeStrings1)
                .withChildAdded(makeStrings2)
                .withChildAdded(makeNumbers1)
                .withChildAdded(makeNumbers2)
                .withChildAdded(calculate)
                .withRenderedChildName("calculateMultiple")
                .connect("makeStrings1", "makeNumbers1", "string")
                .connect("makeStrings2", "makeNumbers2", "string")
                .connect("makeNumbers1", "calculateMultiple", "v1")
                .connect("makeNumbers2", "calculateMultiple", "v3");
        return net;
    }
}
