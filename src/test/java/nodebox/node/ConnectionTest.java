package nodebox.node;

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

    public static final Node net = Node.NETWORK
            .withChildAdded(number42Node)
            .withChildAdded(number5Node)
            .withChildAdded(addNode)
            .withRenderedChildName("add");

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
        assertResultsEqual(n, 47.0);
    }

    public void disabledTestCycles() {
        // TODO Infinite loops are not supported anymore!
        // Create an infinite loop.
        Node n = net
                .connect("number42", "add", "v1")
                .connect("add", "number42", "number");
        assertResultsEqual(n, 42.0);
    }

}
