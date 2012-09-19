package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.function.*;
import nodebox.graphics.Point;
import nodebox.util.SideEffects;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
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
        List<?> results = context.renderNode(valuesToPointNode);
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
        Node net = Node.NETWORK
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
        Node net = Node.NETWORK
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
        assertResultsEqual(context.renderChild(net, add1), 101.0, 102.0, 103.0);
    }

    @Test
    public void testLongestList() {
        Node net = Node.NETWORK
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
        Node net = Node.NETWORK
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
        assertResultsEqual(network, 0.0);
    }

    @Test
    public void testValidSubnetworkResults() {
        Node subnet1 = createAddNetwork("subnet1", 1.0, 2.0);
        Node subnet2 = createAddNetwork("subnet2", 3.0, 4.0);
        Node add1 = addNode.extend().withName("add1");
        Node net = Node.ROOT
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
        assertResultsEqual(frame, 1.0);
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
        Node repeat = Node.ROOT
                .withName("repeat")
                .withFunction("list/repeat")
                .withInputAdded(Port.customPort("value", "list").withRange(Port.Range.LIST))
                .withInputAdded(Port.intPort("amount", 3))
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

        Node makeStrings = Node.ROOT
                .withName("makeStrings")
                .withFunction("string/makeStrings")
                .withInputAdded(Port.stringPort("value", "A;B;C"))
                .withInputAdded(Port.stringPort("separator", ";"))
                .withOutputRange(Port.Range.LIST);

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
        Node makeNestedWords = Node.NETWORK
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

        Port publishedPort = lengthNet.getInput("text").withRange(Port.Range.VALUE);
        lengthNet = lengthNet.withInputChanged("text", publishedPort);

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

}
