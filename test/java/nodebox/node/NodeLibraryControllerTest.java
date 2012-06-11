package nodebox.node;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import static nodebox.util.Assertions.assertResultsEqual;

import static org.junit.Assert.*;

public class NodeLibraryControllerTest {

    private NodeLibraryController controller;

    @Before
    public void setUp() throws Exception {
        controller = NodeLibraryController.create();

    }

    @Test
    public void testChangeRenderedNode() throws Exception {
        Node alpha = Node.ROOT.withName("alpha");
        Node beta = Node.ROOT.withName("beta");
        controller.addNode("/", alpha);
        controller.addNode("/", beta);
        assertNull(controller.getNodeLibrary().getRoot().getRenderedChild());

        controller.setRenderedChild("/", "alpha");
        assertEquals(alpha, controller.getNodeLibrary().getRoot().getRenderedChild());

        controller.setRenderedChild("/", "beta");
        assertEquals(beta, controller.getNodeLibrary().getRoot().getRenderedChild());

        controller.setRenderedChild("/", "");
        assertNull(controller.getNodeLibrary().getRoot().getRenderedChild());
    }

    @Test
    public void testAddPort() {
        Node gamma = Node.ROOT.withName("gamma");
        controller.addNode("/", gamma);
        assertFalse(controller.getNodeLibrary().getNodeForPath("/gamma").hasInput("p"));
        controller.addPort("/gamma", "p", Port.TYPE_INT);
        assertTrue(controller.getNodeLibrary().getNodeForPath("/gamma").hasInput("p"));
    }

    @Test
    public void testRemovePort() {
        Node gamma = Node.ROOT.withName("gamma").withInputAdded(Port.intPort("p", 0));
        controller.addNode("/", gamma);
        assertTrue(controller.getNodeLibrary().getNodeForPath("/gamma").hasInput("p"));
        controller.removePort("/", "gamma", "p");
        assertFalse(controller.getNodeLibrary().getNodeForPath("/gamma").hasInput("p"));
    }

    @Test
    public void testRemoveConnectedPort() {
        Node gamma = Node.ROOT.withName("gamma").withInputAdded(Port.intPort("p", 0));
        Node delta = Node.ROOT.withName("delta").withInputAdded(Port.intPort("q", 0));
        controller.addNode("/", gamma);
        controller.addNode("/", delta);
        controller.connect("/", gamma, delta, delta.getInput("q"));
        assertTrue(controller.getNodeLibrary().getNodeForPath("/delta").hasInput("q"));
        assertEquals(1, controller.getNodeLibrary().getRoot().getConnections().size());
        controller.removePort("/", "delta", "q");
        assertFalse(controller.getNodeLibrary().getNodeForPath("/delta").hasInput("q"));
        assertEquals(0, controller.getNodeLibrary().getRoot().getConnections().size());
    }

    @Test
    public void testAddNode() {
        NodeLibrary library;

        Node parent = Node.ROOT.withName("parent");
        controller.addNode("/", parent);
        library = controller.getNodeLibrary();
        assertTrue(library.getRoot().hasChild("parent"));
        assertSame(parent, library.getRoot().getChild("parent"));
        assertSame(parent, library.getNodeForPath("/parent"));

        Node child = Node.ROOT.withName("child");
        controller.addNode("/parent", child);
        library = controller.getNodeLibrary();
        assertTrue(library.getRoot().getChild("parent").hasChild("child"));
        assertSame(child, library.getNodeForPath("/parent/child"));
        assertNotSame("No longer the same since the new parent has an extra child.", parent, library.getNodeForPath("/parent"));
    }

    /**
     * Test that adding nodes gives them unique names.
     * 
     * addNode() is only used for pasting.
     */
    @Test
    public void testAddNodeUniqueName() {
        Node gamma = Node.ROOT.withName("gamma");
        controller.addNode("/", gamma);
        assertTrue(controller.getNodeLibrary().getRoot().hasChild("gamma"));
        controller.addNode("/", gamma);
        assertTrue(controller.getNodeLibrary().getRoot().hasChild("gamma1"));
    }

    /**
     * Test that pasting nodes with connections works.
     */
    @Test
    public void testPasteNodes() {
        createTestNetwork();
        // Now paste them
        controller.pasteNodes("/", ImmutableList.of(controller.getNode("/alpha"), controller.getNode("/beta")));
        Node root = controller.getNodeLibrary().getRoot();
        assertTrue(root.hasChild("alpha1"));
        assertTrue(root.hasChild("beta1"));
        assertTrue(root.isConnected("alpha1"));
        assertTrue(root.isConnected("beta1"));
    }


    /**
     * Test pasting a node with its output connected.
     * The output should not be replaced.
     */
    @Test
    public void testPasteOutputNode() {
        createTestNetwork();
        // Now paste them
        controller.pasteNodes("/", ImmutableList.of(controller.getNode("/alpha")));
        Node root = controller.getNodeLibrary().getRoot();
        assertTrue(root.hasChild("alpha1"));
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
        assertFalse(root.isConnected("alpha1"));
    }

    /**
     * Test pasting a node with its output connected.
     * A new connection is made.
     */
    @Test
    public void testPasteInputNode() {
        createTestNetwork();
        // Now paste them
        controller.pasteNodes("/", ImmutableList.of(controller.getNode("/beta")));
        Node root = controller.getNodeLibrary().getRoot();
        assertTrue(root.hasChild("beta1"));
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
        assertTrue(root.isConnected("beta1"));
    }

    private void createTestNetwork() {
        Node alpha = Node.ROOT.withName("alpha");
        Node beta = Node.ROOT.withName("beta").withInputAdded(Port.floatPort("number", 0.0));
        controller.addNode("/", alpha);
        controller.addNode("/", beta);
        controller.connect("/", alpha, beta, beta.getInput("number"));
        Node root = controller.getNodeLibrary().getRoot();
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
    }
    
    @Test
    public void testCreateNode() {
        Node proto = Node.ROOT.withName("protoNode");
        controller.createNode("/", proto);
        assertTrue(controller.getNodeLibrary().getRoot().hasChild("protoNode1"));
        assertSame(proto, controller.getNodeLibrary().getNodeForPath("/protoNode1").getPrototype());
    }
    
    @Test
    public void testRemoveNode() {
        Node child = Node.ROOT.withName("child");
        controller.addNode("/", child);
        assertTrue(controller.getNodeLibrary().getRoot().hasChild("child"));
        controller.removeNode("/", "child");
        assertFalse(controller.getNodeLibrary().getRoot().hasChild("child"));
        assertNull(controller.getNodeLibrary().getNodeForPath("/child"));
    }

    @Test
    public void testSetPortValue() {
        Node numberNode = Node.ROOT.withName("number").withInputAdded(Port.intPort("value", 10));
        controller.addNode("/", numberNode);
        assertEquals(10, controller.getNode("/number").getInput("value").intValue());
        controller.setPortValue("/number", "value", 42);
        assertEquals(42, controller.getNode("/number").getInput("value").intValue());
    }

    @Test
    public void testSetPortValueInSubnet() {
        Node numberNode = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 10.0));
        Node subnet = Node.ROOT.withName("subnet").withChildAdded(numberNode).withRenderedChildName("number");
        controller.addNode("/", subnet);
        assertResultsEqual(controller.getNodeLibrary().getRoot(), controller.getNode("/subnet"), 10.0);
        controller.setPortValue("/subnet/number", "value", 42.0);
        assertResultsEqual(controller.getNodeLibrary().getRoot(), controller.getNode("/subnet"), 42.0);
    }

    @Test
    public void testUniqueNodeName() {
        Node proto = Node.ROOT.withName("protoNode");
        controller.createNode("/", proto);
        controller.createNode("/", proto);
        controller.createNode("/", proto);
        Node rootNode = controller.getNodeLibrary().getRoot();
        assertFalse(rootNode.hasChild("protoNode"));
        assertTrue(rootNode.hasChild("protoNode1"));
        assertTrue(rootNode.hasChild("protoNode2"));
        assertTrue(rootNode.hasChild("protoNode3"));

        controller.removeNode("/","protoNode2");
        rootNode = controller.getNodeLibrary().getRoot();
        assertFalse(rootNode.hasChild("protoNode2"));

        controller.createNode("/", proto);
        rootNode = controller.getNodeLibrary().getRoot();
        assertTrue(rootNode.hasChild("protoNode2"));
        assertFalse(rootNode.hasChild("protoNode4"));
    }
    
    @Test
    public void testSimpleRename() {
        Node child = Node.ROOT.withName("child");
        controller.addNode("/", child);
        controller.renameNode("/", "/child", "n");
        assertFalse(controller.getNodeLibrary().getRoot().hasChild("child"));
        assertTrue(controller.getNodeLibrary().getRoot().hasChild("n"));
    }

    @Test
    public void testSimpleConnection() {
        assertEquals(0, controller.getNodeLibrary().getRoot().getConnections().size());
        createSimpleConnection();
        assertEquals(1, controller.getNodeLibrary().getRoot().getConnections().size());
        Connection c = controller.getNodeLibrary().getRoot().getConnections().get(0);
        assertEquals("negate", c.getInputNode());
        assertEquals("value", c.getInputPort());
        assertEquals("number", c.getOutputNode());
        assertResultsEqual(controller.getNodeLibrary().getRoot(), controller.getNode("/negate"), -20.0);
    }

    @Test
    public void testSimpleDisconnect() {
        createSimpleConnection();
        Connection c = controller.getNodeLibrary().getRoot().getConnections().get(0);
        controller.disconnect("/", c);
        assertEquals(0, controller.getNodeLibrary().getRoot().getConnections().size());
    }
    
    @Test
    public void testRemoveNodeWithConnections() {
        createSimpleConnection();
        Node invert2Node = Node.ROOT.withName("invert2").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", invert2Node);
        controller.connect("/", controller.getNode("/number"), invert2Node, invert2Node.getInput("value"));
        assertEquals(2, controller.getNodeLibrary().getRoot().getConnections().size());
        controller.removeNode("/", "number");
        assertEquals(0, controller.getNodeLibrary().getRoot().getConnections().size());
    }
    
    @Test
    public void testRemoveNodeWithRendered() {
        Node alpha = Node.ROOT.withName("alpha");
        controller.addNode("/", alpha);
        controller.setRenderedChild("/", "alpha");
        Node beta = Node.ROOT.withName("beta");
        controller.addNode("/", beta);
        assertEquals("alpha", controller.getNodeLibrary().getRoot().getRenderedChildName());
        controller.removeNode("/", "beta");
        assertEquals("alpha", controller.getNodeLibrary().getRoot().getRenderedChildName());
        controller.removeNode("/", "alpha");
        assertEquals("", controller.getNodeLibrary().getRoot().getRenderedChildName());
        assertNull(controller.getNodeLibrary().getRoot().getRenderedChild());
    }

    private void createSimpleConnection() {
        Node numberNode = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 20));
        Node invertNode = Node.ROOT.withName("negate").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", numberNode);
        controller.addNode("/", invertNode);
        controller.connect("/", numberNode, invertNode, invertNode.getInput("value"));
    }
}
