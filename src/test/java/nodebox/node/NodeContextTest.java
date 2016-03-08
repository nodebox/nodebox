package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.function.*;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.util.SideEffects;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.*;
import static nodebox.util.Assertions.assertNoResults;
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

    public static final Node makeStringsNode = Node.ROOT
            .withName("makeStrings")
            .withFunction("string/makeStrings")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.stringPort("string", "Alpha;Beta;Gamma"))
            .withInputAdded(Port.stringPort("separator", ";"));

    public static final Node stringNode = Node.ROOT
            .withName("string")
            .withFunction("string/string")
            .withInputAdded(Port.stringPort("value", ""));

    public static final FunctionRepository functions = FunctionRepository.of(CoreVectorFunctions.LIBRARY, MathFunctions.LIBRARY, ListFunctions.LIBRARY, StringFunctions.LIBRARY, SideEffects.LIBRARY, TestFunctions.LIBRARY);
    public static final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);

    private List<?> renderNode(Node node) {
        return new NodeContext(testLibrary.withRoot(node)).renderNode("/");
    }

    private List<?> renderChild(Node network, Node child) {
        return new NodeContext(testLibrary.withRoot(network)).renderChild("/", child);
    }

    @Before
    public void setUp() throws Exception {
        SideEffects.reset();
    }

    @Test
    public void testSingleOutput() {
        List<?> results = renderNode(valuesToPointNode);
        assertEquals(1, results.size());
        assertResultsEqual(results, Point.ZERO);
    }

    @Test
    public void testListRange() {
        Node node = Node.ROOT
                .withFunction("math/average")
                .withInputAdded(Port.floatPort("values", 42).withRange(Port.Range.LIST));
        assertResultsEqual(node, 0.0);
    }

    @Test
    public void testSameOutputPort() {
        Node invert1 = invertNode.extend().withName("invert1").withInputValue("value", 1.0);
        Node invert2 = invertNode.extend().withName("invert2").withInputValue("value", 10.0);
        assertResultsEqual(renderNode(invert1), -1.0);
        assertResultsEqual(renderNode(invert2), -10.0);
    }

    @Test
    public void testListAwareProcessing() {
        Node makeNumbers1 = makeNumbersNode.extend().withInputValue("string", "1 2 3 4");
        assertResultsEqual(renderNode(makeNumbers1), 1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void testListUnawareProcessing() {
        Node invert1 = invertNode.extend().withName("invert1").withInputValue("value", 42.0);
        assertResultsEqual(renderNode(invert1), -42.0);
    }

    @Test
    public void testConnectedListProcessing() {
        Node makeNumbers1 = makeNumbersNode.extend().withName("makeNumbers1").withInputValue("string", "1 2 3 4");
        Node invert1 = invertNode.extend().withName("invert1");
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers1)
                .withChildAdded(invert1)
                .connect("makeNumbers1", "invert1", "value")
                .withRenderedChildName("invert1");
        assertResultsEqual(renderChild(net, invert1), -1.0, -2.0, -3.0, -4.0);
    }

    @Test
    public void testEmptyListProcessing() {
        Node noNumbers = makeNumbersNode.extend().withName("noNumbers").withInputValue("string", "");
        Node add1 = addNode.extend().withName("add1");
        Node net = Node.NETWORK
                .withChildAdded(noNumbers)
                .withChildAdded(add1)
                .connect("noNumbers", "add1", "v1");
        assertNoResults(net, add1);
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
        assertResultsEqual(renderNode(getNumberNode), 42L);
    }

    @Test
    public void testOutputSideEffect() {
        Node setNumberNode = Node.ROOT
                .withFunction("side-effects/setNumber")
                .withInputAdded(Port.intPort("number", 42));
        renderNode(setNumberNode);
        assertEquals(SideEffects.theOutput, 42L);
    }

    @Test
    public void testSamePrototypeTwice() {
        Node invert1Node = invertNode.withName("invert1").withInputValue("value", 42.0);
        Node invert2Node = invertNode.withName("invert2");
        Node net = Node.NETWORK
                .withChildAdded(invert1Node)
                .withChildAdded(invert2Node)
                .connect("invert1", "invert2", "value");
        assertResultsEqual(renderChild(net, invert2Node), 42.0);
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
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers1)
                .withChildAdded(incNode)
                .connect("makeNumbers1", "inc", "number");
        assertResultsEqual(net, incNode, 2.0, 3.0, 4.0);
        assertEquals(3, SideEffects.theCounter);
    }

    /**
     * Test the combination of a list input and port value.
     */
    @Test
    public void testListWithValue() {
        Node makeNumbers1 = makeNumbersNode.withName("makeNumbers1").withInputValue("string", "1 2 3");
        Node add1 = addNode.extend().withName("add1").withInputValue("v2", 100.0);
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers1)
                .withChildAdded(add1)
                .connect("makeNumbers1", "add1", "v1");
        assertResultsEqual(renderChild(net, add1), 101.0, 102.0, 103.0);
    }

    @Test
    public void testLongestList() {
        Node net = Node.NETWORK
                .withChildAdded(threeNumbers)
                .withChildAdded(fiveNumbers)
                .withChildAdded(addNode)
                .connect("threeNumbers", addNode.getName(), "v1")
                .connect("fiveNumbers", addNode.getName(), "v2");
        assertResultsEqual(renderChild(net, addNode), 101.0, 202.0, 303.0, 401.0, 502.0);
    }

    @Test
    public void testPortRangeMatching() {
        Node sum = Node.ROOT
                .withName("sum")
                .withFunction("math/sum")
                .withInputAdded(Port.floatPort("numbers", 0))
                .withInputRange("numbers", Port.Range.LIST);
        Node net = Node.NETWORK
                .withChildAdded(sum)
                .withChildAdded(threeNumbers)
                .connect("threeNumbers", sum.getName(), "numbers");
        assertResultsEqual(renderChild(net, sum), 6.0);
    }

    @Test
    public void testTypeConversion() {
        assertConversion(Port.TYPE_INT, Port.TYPE_INT, 42L, 42L);
        assertConversion(Port.TYPE_INT, Port.TYPE_FLOAT, 42L, 42.0);
        assertConversion(Port.TYPE_INT, Port.TYPE_STRING, 42L, "42");
        assertConversion(Port.TYPE_INT, Port.TYPE_BOOLEAN, 42L, true);
        assertConversion(Port.TYPE_INT, Port.TYPE_COLOR, 255L, Color.WHITE);
        assertConversion(Port.TYPE_INT, Port.TYPE_POINT, 42L, new Point(42, 42));

        assertConversion(Port.TYPE_FLOAT, Port.TYPE_INT, 42.0, 42L);
        assertConversion(Port.TYPE_FLOAT, Port.TYPE_FLOAT, 42.0, 42.0);
        assertConversion(Port.TYPE_FLOAT, Port.TYPE_STRING, 42.0, "42.0");
        assertConversion(Port.TYPE_FLOAT, Port.TYPE_BOOLEAN, 0.0, false);
        assertConversion(Port.TYPE_FLOAT, Port.TYPE_COLOR, 0.0, Color.BLACK);
        assertConversion(Port.TYPE_FLOAT, Port.TYPE_POINT, 42.0, new Point(42, 42));

        assertConversion(Port.TYPE_STRING, Port.TYPE_INT, "42", 42L);
        assertConversion(Port.TYPE_STRING, Port.TYPE_FLOAT, "42", 42.0);
        assertConversion(Port.TYPE_STRING, Port.TYPE_STRING, "hello", "hello");
        assertConversion(Port.TYPE_STRING, Port.TYPE_BOOLEAN, "true", true);
        assertConversion(Port.TYPE_STRING, Port.TYPE_BOOLEAN, "not-a-boolean", false);
        assertConversion(Port.TYPE_STRING, Port.TYPE_COLOR, "#ff0000ff", new Color(1, 0, 0));
        assertConversion(Port.TYPE_STRING, Port.TYPE_POINT, "4,2", new Point(4, 2));

        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_INT, true, 1L);
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_INT, false, 0L);
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_FLOAT, true, 1.0);
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_STRING, true, "true");
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_STRING, false, "false");
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_BOOLEAN, false, false);
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_COLOR, true, Color.WHITE);
        assertConversion(Port.TYPE_BOOLEAN, Port.TYPE_COLOR, false, Color.BLACK);

        assertConversion(Port.TYPE_COLOR, Port.TYPE_STRING, new Color(0, 1, 0), "#00ff00ff");
        assertConversion(Port.TYPE_COLOR, Port.TYPE_COLOR, Color.WHITE, Color.WHITE);

        assertConversion(Port.TYPE_POINT, Port.TYPE_STRING, new Point(4, 2), "4.00,2.00");
        assertConversion(Port.TYPE_POINT, Port.TYPE_POINT, new Point(4, 2), new Point(4, 2));
    }

    @Test
    public void testGeometryToPointsConversion() {
        Node line = Node.ROOT
                .withName("line")
                .withFunction("corevector/line")
                .withInputAdded(Port.pointPort("point1", new Point(10, 20)))
                .withInputAdded(Port.pointPort("point2", new Point(30, 40)))
                .withInputAdded(Port.intPort("points", 2));
        Node point = Node.ROOT
                .withName("point")
                .withFunction("corevector/point")
                .withInputAdded(Port.pointPort("value", Point.ZERO));
        Node net = Node.NETWORK
                .withChildAdded(line)
                .withChildAdded(point)
                .withRenderedChild(point)
                .connect("line", "point", "value");
        assertResultsEqual(net, new Point(10, 20), new Point(30, 40));
    }

    private void assertConversion(String sourceType, String targetType, Object sourceValue, Object targetValue) {
        String generateFunction = identityFunction(sourceType);
        Port generatePort = Port.parsedPort("value", sourceType, sourceValue.toString());
        String convertFunction = identityFunction(targetType);
        Port convertPort = Port.portForType("value", targetType);
        Node net = buildTypeConversionNetwork(generateFunction, generatePort, convertFunction, convertPort);
        assertResultsEqual(net, targetValue);
    }

    private String identityFunction(String portType) {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        b.put(Port.TYPE_INT, "math/integer");
        b.put(Port.TYPE_FLOAT, "math/number");
        b.put(Port.TYPE_STRING, "string/string");
        b.put(Port.TYPE_BOOLEAN, "math/makeBoolean");
        b.put(Port.TYPE_COLOR, "color/color");
        b.put(Port.TYPE_POINT, "corevector/point");
        ImmutableMap<String, String> functions = b.build();
        return functions.get(portType);
    }

    private Node buildTypeConversionNetwork(String generateFunction, Port generatePort, String convertFunction, Port convertPort) {
        Node generator = Node.ROOT.withName("generate").withFunction(generateFunction).withInputAdded(generatePort);
        Node converter = Node.ROOT.withName("convert").withFunction(convertFunction).withInputAdded(convertPort);
        return Node.NETWORK
                .withChildAdded(generator)
                .withChildAdded(converter)
                .withRenderedChild(converter)
                .connect("generate", "convert", "value");
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
        Node net = Node.NETWORK
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
        Node net = Node.NETWORK
                .withChildAdded(makeStrings)
                .withChildAdded(makeNumbers)
                .withRenderedChildName("makeNumbers")
                .connect("makeStrings", "makeNumbers", "string");
        assertResultsEqual(net, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
    }

    @Test
    public void testRenderSubnetwork() {
        Node subnet = createAddNetwork("subnet1", 1.0, 2.0);
        Node net = Node.NETWORK
                .withChildAdded(subnet)
                .withRenderedChildName("subnet1");
        assertResultsEqual(net, subnet, 3.0);
    }

    @Test
    public void testRenderEmptyNetwork() {
        Node network = Node.NETWORK;
        assertResultsEqual(network);
    }

    @Test
    public void testValidSubnetworkResults() {
        Node subnet1 = createAddNetwork("subnet1", 1.0, 2.0);
        Node subnet2 = createAddNetwork("subnet2", 3.0, 4.0);
        Node add1 = addNode.extend().withName("add1");
        Node net = Node.NETWORK
                .withChildAdded(subnet1)
                .withChildAdded(subnet2)
                .withChildAdded(add1)
                .withRenderedChildName("add1")
                .connect("subnet1", "add1", "v1")
                .connect("subnet2", "add1", "v2");
        assertResultsEqual(net, add1, 10.0);
    }

    @Test
    public void testRenderNetworkWithPublishedPorts() {
        Node subNet = createAddNetwork("subnet1", 0.0, 0.0)
                .publish("number1", "number", "value1")
                .publish("number2", "number", "value2")
                .withInputValue("value1", 2.0)
                .withInputValue("value2", 3.0);
        Node net = Node.NETWORK
                .withChildAdded(subNet)
                .withRenderedChildName("subnet1");
        assertResultsEqual(net, subNet, 5.0);
    }

    @Test
    public void testRenderNetworkWithConnectedPublishedPorts() {
        Node addNet = createAddNetwork("addNet", 0, 0)
                .publish("number1", "number", "value1")
                .publish("number2", "number", "value2");
        Node number1 = numberNode
                .withName("number1")
                .withInputValue("number", 5.0);
        Node number2 = numberNode
                .withName("number2")
                .withInputValue("number", 3.0);
        Node net = Node.NETWORK
                .withChildAdded(number1)
                .withChildAdded(number2)
                .withChildAdded(addNet)
                .withRenderedChild(addNet)
                .connect("number1", "addNet", "value1")
                .connect("number2", "addNet", "value2");
        assertResultsEqual(net, addNet, 8.0);
    }

    @Test
    public void testRenderNestedNetworkWithConnectedPublishedPorts() {
        Node subnet1 = createAddNetwork("subnet1", 0.0, 0.0)
                .publish("number1", "number", "n1")
                .publish("number2", "number", "n2");
        Node subnet2 = createAddNetwork("subnet2", 0.0, 0.0)
                .publish("number1", "number", "n1")
                .publish("number2", "number", "n2");
        Node add1 = addNode.extend().withName("add1");
        Node subnet = Node.NETWORK
                .withName("subnet")
                .withChildAdded(subnet1)
                .withChildAdded(subnet2)
                .withChildAdded(add1)
                .withRenderedChildName("add1")
                .connect("subnet1", "add1", "v1")
                .connect("subnet2", "add1", "v2")
                .publish("subnet1", "n1", "value1")
                .publish("subnet1", "n2", "value2")
                .publish("subnet2", "n1", "value3")
                .publish("subnet2", "n2", "value4");
        Node number1 = numberNode.extend()
                .withName("number1")
                .withInputValue("number", 11.0);
        Node number2 = numberNode.extend()
                .withName("number2")
                .withInputValue("number", 22.0);
        Node number3 = numberNode.extend()
                .withName("number3")
                .withInputValue("number", 33.0);
        Node number4 = numberNode.extend()
                .withName("number4")
                .withInputValue("number", 44.0);
        Node net = Node.NETWORK
                .withChildAdded(number1)
                .withChildAdded(number2)
                .withChildAdded(number3)
                .withChildAdded(number4)
                .withChildAdded(subnet)
                .withRenderedChildName("subnet")
                .connect("number1", "subnet", "value1")
                .connect("number2", "subnet", "value2")
                .connect("number3", "subnet", "value3")
                .connect("number4", "subnet", "value4");
        assertResultsEqual(net, subnet, 110.0);
    }

    @Test
    public void testRenderUnpublishAndDisconnect() {
        Node subNet = createAddNetwork("subNet", 2.0, 3.0)
                .publish("number1", "number", "n1")
                .publish("number2", "number", "n2");
        Node number = numberNode.extend()
                .withName("number")
                .withInputValue("number", 11.0);
        Node net = Node.NETWORK
                .withChildAdded(number)
                .withChildAdded(subNet)
                .withRenderedChildName("subNet")
                .connect("number", "subNet", "n1");
        assertResultsEqual(net, subNet, 14.0);
        subNet = subNet.unpublish("n1");
        net = net.withChildReplaced("subNet", subNet);
        assertResultsEqual(net, subNet, 5.0);
    }

    @Test
    public void testFrame() {
        Node frame = Node.ROOT
                .withName("frame")
                .withFunction("core/frame")
                .withInputAdded(Port.customPort("context", "context"));
        Node frameNet = Node.NETWORK.withChildAdded(frame).withRenderedChild(frame);
        NodeContext c = new NodeContext(testLibrary.withRoot(frameNet), FunctionRepository.of(), ImmutableMap.of("frame", 42.0));
        List<?> results = c.renderNode("/");
        assertResultsEqual(results, 42.0);
    }

    // TODO Check list-aware node with no inputs.
    // TODO Check list-aware node with no outputs.
    // TODO Check list-aware node with single output.
    // TODO Check list-aware node with multiple outputs.

    // TODO Check list-unaware node with single output.
    // TODO Check list-unaware node with multiple outputs.
    // TODO Check list-unaware node with multiple inputs, single output.

    private Node createAddNetwork(String name, double v1, double v2) {
        Node number1 = numberNode.extend()
                .withName("number1")
                .withInputValue("number", v1);
        Node number2 = numberNode.extend()
                .withName("number2")
                .withInputValue("number", v2);
        return Node.NETWORK
                .withName(name)
                .withChildAdded(number1)
                .withChildAdded(number2)
                .withChildAdded(addNode)
                .withRenderedChild(addNode)
                .connect("number1", "add", "v1")
                .connect("number2", "add", "v2");
    }

    @Test
    public void testListOutputRange() {
        Node slice = Node.ROOT
                .withName("slice")
                .withFunction("list/slice")
                .withInputAdded(Port.stringPort("list", "").withRange(Port.Range.LIST))
                .withInputAdded(Port.intPort("start", 0))
                .withInputAdded(Port.intPort("size", 1000))
                .withInputAdded(Port.booleanPort("invert", false))
                .withOutputRange(Port.Range.LIST);
        Node makeStrings = Node.ROOT
                .withName("makeStrings")
                .withFunction("string/makeStrings")
                .withInputAdded(Port.stringPort("text", "A;B;C"))
                .withInputAdded(Port.stringPort("separator", ";"))
                .withOutputRange(Port.Range.LIST);
        Node makeNumbers = Node.ROOT
                .withName("makeNumbers")
                .withFunction("math/makeNumbers")
                .withInputAdded(Port.stringPort("text", "0;1;2"))
                .withInputAdded(Port.stringPort("separator", ";"))
                .withOutputRange(Port.Range.LIST);
        Node net = Node.NETWORK
                .withChildAdded(makeStrings)
                .withChildAdded(makeNumbers)
                .withChildAdded(slice)
                .connect("makeStrings", "slice", "list")
                .connect("makeNumbers", "slice", "start");

        assertResultsEqual(net, slice, "A", "B", "C", "B", "C", "C");
    }

    @Test
    public void testNestedGenerator() {
        Node makeStrings = Node.ROOT
                .withName("makeStrings")
                .withFunction("string/makeStrings")
                .withInputAdded(Port.stringPort("value", "A;B;C"))
                .withInputAdded(Port.stringPort("separator", ";"))
                .withOutputRange(Port.Range.LIST);

        Node repeat = Node.ROOT
                .withName("repeat")
                .withFunction("list/repeat")
                .withInputAdded(Port.customPort("value", "list").withRange(Port.Range.LIST))
                .withInputAdded(Port.intPort("amount", 3))
                .withInputAdded(Port.booleanPort("per_item", false))
                .withOutputRange(Port.Range.LIST);

        Node repeatNet = Node.NETWORK
                .withName("repeatNet")
                .withChildAdded(repeat)
                .withRenderedChild(repeat)
                .publish("repeat", "value", "strings")
                .withOutputRange(Port.Range.VALUE);
        Port publishedPort = repeatNet.getInput("strings").withRange(Port.Range.VALUE);
        repeatNet = repeatNet.withInputChanged("strings", publishedPort);

        assertResultsEqual(repeatNet);

        Node net = Node.NETWORK
                .withChildAdded(makeStrings)
                .withChildAdded(repeatNet)
                .withRenderedChild(repeatNet)
                .connect("makeStrings", "repeatNet", "strings");

        assertResultsEqual(net, ImmutableList.of("A", "A", "A"), ImmutableList.of("B", "B", "B"), ImmutableList.of("C", "C", "C"));
    }

    @Test
    public void testSimpleNestedFilter() {
        Node makeStrings = Node.ROOT
                .withName("makeStrings")
                .withFunction("string/makeStrings")
                .withInputAdded(Port.stringPort("value", "alpha;beta;gamma"))
                .withInputAdded(Port.stringPort("separator", ";"))
                .withOutputRange(Port.Range.LIST);

        Node caseNode = Node.ROOT
                .withName("changeCase")
                .withFunction("string/changeCase")
                .withInputAdded(Port.stringPort("value", ""))
                .withInputAdded(Port.stringPort("method", "uppercase"));

        Node caseNet = Node.NETWORK
                .withName("caseNet")
                .withChildAdded(caseNode)
                .withRenderedChild(caseNode)
                .publish("changeCase", "value", "value");

        Node net = Node.NETWORK
                .withChildAdded(makeStrings)
                .withChildAdded(caseNet)
                .connect("makeStrings", "caseNet", "value")
                .withRenderedChild(caseNet);

        assertResultsEqual(net, "ALPHA", "BETA", "GAMMA");
    }

    @Test
    public void testNestedFilter() {
        Node makeNestedWords = Node.ROOT
                .withName("makeNestedWords")
                .withFunction("test/makeNestedWords")
                .withOutputType("string")
                .withOutputRange(Port.Range.LIST);

        Node length = Node.ROOT
                .withName("length")
                .withFunction("string/length")
                .withOutputType("string")
                .withInputAdded(Port.stringPort("text", ""));

        Node lengthNet = Node.NETWORK
                .withName("lengthNet")
                .withChildAdded(length)
                .publish("length", "text", "text")
                .withRenderedChild(length)
                .withOutputRange(Port.Range.VALUE);

        Port textPort = lengthNet.getInput("text").withRange(Port.Range.VALUE);
        lengthNet = lengthNet.withInputChanged("text", textPort);

        Node mainNetwork = Node.NETWORK
                .withChildAdded(makeNestedWords)
                .withChildAdded(lengthNet)
                .connect("makeNestedWords", "lengthNet", "text")
                .withRenderedChild(lengthNet);

        List<Integer> aCounts = ImmutableList.of(5, 11, 9);
        List<Integer> bCounts = ImmutableList.of(6, 4, 4);
        List<Integer> cCounts = ImmutableList.of(5, 8, 6);
        assertResultsEqual(mainNetwork, aCounts, bCounts, cCounts);
    }

    @Test
    public void testClamping() {
        Port value = invertNode.getInput("value");
        value = value.withMaximumValue(10.0);
        Node clampedInvertNode = invertNode.withInputChanged("value", value);
        clampedInvertNode = clampedInvertNode.withInputValue("value", 25.0);
        assertResultsEqual(clampedInvertNode, -10.0);

        Node number1 = numberNode.withInputValue("number", 25.0);
        Node net = Node.NETWORK
                .withChildAdded(clampedInvertNode)
                .withChildAdded(number1)
                .connect("number", "negate", "value");
        assertResultsEqual(net, clampedInvertNode, -10.0);
    }

    @Test
    public void testStoreIntermediateResults() {
        Node increase = Node.ROOT
                .withName("increase")
                .withFunction("side-effects/increaseAndCount")
                .withInputAdded(Port.floatPort("counter", 42.0));

        Node network = Node.NETWORK
                .withChildAdded(addNode)
                .withChildAdded(increase)
                .connect("increase", "add", "v1")
                .connect("increase", "add", "v2");

        SideEffects.reset();

        assertResultsEqual(network, addNode, 86.0);
        assertEquals(1L, SideEffects.theCounter);
    }

    @Test
    public void testPortOverrides() {
        Node number3 = numberNode.withName("number3").withInputValue("number", 3.0);
        Node number5 = numberNode.withName("number5").withInputValue("number", 5.0);
        Node net = Node.NETWORK
                .withChildAdded(number3)
                .withChildAdded(number5)
                .withChildAdded(addNode)
                .connect("number3", "add", "v1")
                .connect("number5", "add", "v2")
                .withRenderedChildName("add");
        // With no overrides, the add node returns 8.0
        assertResultsEqual(net, addNode, 8.0);
        ImmutableMap<String, ?> overrides = ImmutableMap.of("number3.number", 10.0);
        NodeContext ctx = new NodeContext(testLibrary.withRoot(net), null, ImmutableMap.<String, Object>of(), ImmutableMap.<String, List<?>>of(), overrides);
        Iterable<?> values = ctx.renderChild("/", addNode);
        assertResultsEqual(values, 15.0);
    }

}
