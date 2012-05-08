package nodebox.node;

import nodebox.function.FunctionRepository;
import nodebox.function.MathFunctions;
import org.junit.Before;
import org.junit.Test;

import static nodebox.util.Assertions.assertResultsEqual;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

    public static final Node number42Node = Node.ROOT
            .withName("number42")
            .withFunction("math/number")
            .withInputAdded(Port.floatPort("number", 42));

    public static final Node number5Node = Node.ROOT
            .withName("number5")
            .withFunction("math/number")
            .withInputAdded(Port.floatPort("number", 5));

    public static final Node addNode = Node.ROOT
            .withName("add")
            .withFunction("math/add")
            .withInputAdded(Port.floatPort("v1", 0))
            .withInputAdded(Port.floatPort("v2", 0));

    public static final Node net = Node.ROOT
            .withChildAdded(number42Node)
            .withChildAdded(number5Node)
            .withChildAdded(addNode)
            .withRenderedChildName("add");

    private FunctionRepository functions = FunctionRepository.of(MathFunctions.LIBRARY);
    private NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);
    private NodeContext context;

    @Before
    public void setUp() throws Exception {
        context = new NodeContext(testLibrary);
    }

    @Test
    public void testBasicConnection() {
        Node n = net;
        assertFalse(n.isConnected("number42"));
        assertFalse(n.isConnected("add"));
        n = n.connect("number42", "add", "v1");
        assertTrue(n.isConnected("number42"));
        assertTrue(n.isConnected("add"));
        n = n.connect("number5", "add", "v2");
        assertTrue(n.isConnected("number5"));
    }

    @Test
    public void testReplaceConnection() {
        Node n = net;
        n = n.connect("number42", "add", "v1");
        assertTrue(n.isConnected("number42"));
        n = n.connect("number5", "add", "v1");
        assertFalse(n.isConnected("number42"));
    }

    @Test
    public void testExecute() {
        Node n = net
                .connect("number42", "add", "v1")
                .connect("number5", "add", "v2");
        context.renderNetwork(n);
        assertResultsEqual(context.getResults(addNode), 47.0);
    }

    @Test
    public void testCycles() {
        // Create an infinite loop.
        Node n = net
                .connect("number42", "add", "v1")
                .connect("add", "number42", "number");
        // Infinite loops are allowed: each node is only executed once.
        context.renderNetwork(n);
        assertResultsEqual(context.getResults(addNode), 42.0);
    }

}
