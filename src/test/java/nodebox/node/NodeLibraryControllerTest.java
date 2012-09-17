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
        assertNull(controller.getRootNode().getRenderedChild());

        controller.setRenderedChild("/", "alpha");
        assertEquals(alpha, controller.getRootNode().getRenderedChild());

        controller.setRenderedChild("/", "beta");
        assertEquals(beta, controller.getRootNode().getRenderedChild());

        controller.setRenderedChild("/", "");
        assertNull(controller.getRootNode().getRenderedChild());
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
        assertEquals(1, controller.getRootNode().getConnections().size());
        controller.removePort("/", "delta", "q");
        assertFalse(controller.getNodeLibrary().getNodeForPath("/delta").hasInput("q"));
        assertEquals(0, controller.getRootNode().getConnections().size());
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
        assertTrue(controller.getRootNode().hasChild("gamma"));
        controller.addNode("/", gamma);
        assertTrue(controller.getRootNode().hasChild("gamma1"));
    }

    /**
     * Test that copying/pasting nodes with connections works.
     */
    @Test
    public void testCopyPasteNodes() {
        createTestNetwork();
        // Now paste them
        controller.pasteNodes("/", controller.getRootNode(), ImmutableList.of(controller.getNode("/alpha"), controller.getNode("/beta")));
        Node root = controller.getRootNode();
        assertTrue(root.hasChild("alpha1"));
        assertTrue(root.hasChild("beta1"));
        assertTrue(root.isConnected("alpha1"));
        assertTrue(root.isConnected("beta1"));
    }

    /**
     * Test that cutting/pasting nodes with connections works.
     */
    @Test
    public void testCutPasteNodes() {
        createTestNetwork();
        Node root = controller.getRootNode();
        Node alpha = controller.getNode("/alpha");
        Node beta = controller.getNode("/beta");
        controller.removeNode("/", "alpha");
        controller.removeNode("/", "beta");
        controller.pasteNodes("/", root, ImmutableList.of(alpha, beta));
        assertTrue(root.hasChild("alpha"));
        assertTrue(root.hasChild("beta"));
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
    }

    /**
     * Test that copying/pasting nodes with connections into subnetworks works.
     */
    @Test
    public void testPasteIntoSubnetwork() {
        createTestNetwork();
        controller.addNode("/", Node.ROOT.withName("subnet"));
        controller.pasteNodes("/subnet", controller.getRootNode(), ImmutableList.of(controller.getNode("/alpha"), controller.getNode("/beta")));
        Node subnet = controller.getNode("/subnet");
        assertTrue(subnet.hasChild("alpha"));
        assertTrue(subnet.hasChild("beta"));
        assertTrue(subnet.isConnected("alpha"));
        assertTrue(subnet.isConnected("beta"));
    }

    /**
     * Test pasting a node with its output connected.
     * The output should not be replaced.
     */
    @Test
    public void testPasteOutputNode() {
        createTestNetwork();
        // Now paste them
        controller.pasteNodes("/", controller.getRootNode(), ImmutableList.of(controller.getNode("/alpha")));
        Node root = controller.getRootNode();
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
        controller.pasteNodes("/", controller.getRootNode(), ImmutableList.of(controller.getNode("/beta")));
        Node root = controller.getRootNode();
        assertTrue(root.hasChild("beta1"));
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
        assertTrue(root.isConnected("beta1"));
    }

    /**
     * Test pasting a node with its output connected but with the input removed between copying/pasting.
     * No new connection is made.
     */
    @Test
    public void testPasteInputNodePartial() {
        createTestNetwork();
        Node root = controller.getRootNode();
        controller.removeNode("/", "alpha");
        // Now paste them
        controller.pasteNodes("/", root, ImmutableList.of(controller.getNode("/beta")));
        root = controller.getRootNode();
        assertTrue(root.hasChild("beta1"));
        assertFalse(root.hasChild("alpha"));
        assertFalse(root.isConnected("beta"));
        assertFalse(root.isConnected("beta1"));
    }

    private void createTestNetwork() {
        Node alpha = Node.ROOT.withName("alpha");
        Node beta = Node.ROOT.withName("beta").withInputAdded(Port.floatPort("number", 0.0));
        controller.addNode("/", alpha);
        controller.addNode("/", beta);
        controller.connect("/", alpha, beta, beta.getInput("number"));
        Node root = controller.getRootNode();
        assertTrue(root.isConnected("alpha"));
        assertTrue(root.isConnected("beta"));
    }

    @Test
    public void testCreateNode() {
        Node proto = Node.ROOT.withName("protoNode");
        controller.createNode("/", proto);
        assertTrue(controller.getRootNode().hasChild("protoNode1"));
        assertSame(proto, controller.getNodeLibrary().getNodeForPath("/protoNode1").getPrototype());
    }
    
    @Test
    public void testRemoveNode() {
        Node child = Node.ROOT.withName("child");
        controller.addNode("/", child);
        assertTrue(controller.getRootNode().hasChild("child"));
        controller.removeNode("/", "child");
        assertFalse(controller.getRootNode().hasChild("child"));
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
    public void testSetPortValueInSubNet() {
        Node numberNode = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 10.0));
        Node subNet = Node.NETWORK.withName("subNet").withChildAdded(numberNode).withRenderedChildName("number");
        controller.addNode("/", subNet);
        assertResultsEqual(controller.getRootNode(), controller.getNode("/subNet"), 10.0);
        controller.setPortValue("/subNet/number", "value", 42.0);
        assertResultsEqual(controller.getRootNode(), controller.getNode("/subNet"), 42.0);
    }

    @Test
    public void testUniqueNodeName() {
        Node proto = Node.ROOT.withName("protoNode");
        controller.createNode("/", proto);
        controller.createNode("/", proto);
        controller.createNode("/", proto);
        Node rootNode = controller.getRootNode();
        assertFalse(rootNode.hasChild("protoNode"));
        assertTrue(rootNode.hasChild("protoNode1"));
        assertTrue(rootNode.hasChild("protoNode2"));
        assertTrue(rootNode.hasChild("protoNode3"));

        controller.removeNode("/","protoNode2");
        rootNode = controller.getRootNode();
        assertFalse(rootNode.hasChild("protoNode2"));

        controller.createNode("/", proto);
        rootNode = controller.getRootNode();
        assertTrue(rootNode.hasChild("protoNode2"));
        assertFalse(rootNode.hasChild("protoNode4"));
    }
    
    @Test
    public void testSimpleRename() {
        Node child = Node.ROOT.withName("child");
        controller.addNode("/", child);
        controller.renameNode("/", "child", "n");
        assertFalse(controller.getRootNode().hasChild("child"));
        assertTrue(controller.getRootNode().hasChild("n"));
    }

    @Test
    public void testRenderedNodeRenaming() {
        Node child = Node.ROOT.withName("child");
        controller.addNode("/", child);
        controller.setRenderedChild("/", "child");
        assertTrue(controller.getRootNode().getRenderedChildName().equals("child"));
        controller.renameNode("/", "child", "n");
        assertTrue(controller.getRootNode().getRenderedChildName().equals("n"));
    }

    @Test
    public void testSimpleConnection() {
        assertEquals(0, controller.getRootNode().getConnections().size());
        createSimpleConnection();
        assertEquals(1, controller.getRootNode().getConnections().size());
        Connection c = controller.getRootNode().getConnections().get(0);
        assertEquals("negate", c.getInputNode());
        assertEquals("value", c.getInputPort());
        assertEquals("number", c.getOutputNode());
        assertResultsEqual(controller.getRootNode(), controller.getNode("/negate"), -20.0);
    }

    @Test
    public void testSimpleDisconnect() {
        createSimpleConnection();
        Connection c = controller.getRootNode().getConnections().get(0);
        controller.disconnect("/", c);
        assertEquals(0, controller.getRootNode().getConnections().size());
    }
    
    @Test
    public void testRemoveNodeWithConnections() {
        createSimpleConnection();
        Node invert2Node = Node.ROOT.withName("invert2").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", invert2Node);
        controller.connect("/", controller.getNode("/number"), invert2Node, invert2Node.getInput("value"));
        assertEquals(2, controller.getRootNode().getConnections().size());
        controller.removeNode("/", "number");
        assertEquals(0, controller.getRootNode().getConnections().size());
    }

    @Test
    public void testConnectedNodeRenaming() {
        createSimpleConnection();
        controller.renameNode("/", "negate", "invert");
        assertEquals(1, controller.getRootNode().getConnections().size());
        Connection c = controller.getRootNode().getConnections().get(0);
        assertEquals("invert", c.getInputNode());
        assertEquals("value", c.getInputPort());
        assertEquals("number", c.getOutputNode());
    }

    @Test
    public void testRemoveNodeWithRendered() {
        Node alpha = Node.ROOT.withName("alpha");
        controller.addNode("/", alpha);
        controller.setRenderedChild("/", "alpha");
        Node beta = Node.ROOT.withName("beta");
        controller.addNode("/", beta);
        assertEquals("alpha", controller.getRootNode().getRenderedChildName());
        controller.removeNode("/", "beta");
        assertEquals("alpha", controller.getRootNode().getRenderedChildName());
        controller.removeNode("/", "alpha");
        assertEquals("", controller.getRootNode().getRenderedChildName());
        assertNull(controller.getRootNode().getRenderedChild());
    }

    @Test
    public void testPublish() {
        Node invertNode = Node.ROOT.withName("negate").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", Node.NETWORK.withName("subnet"));
        controller.addNode("/subnet", invertNode);
        controller.setRenderedChild("/subnet", "negate");
        controller.publish("/subnet", "negate", "value", "n");
        assertTrue(controller.getNode("/subnet").hasPublishedInput("n"));
        controller.setPortValue("/subnet", "n", 42.0);
        assertEquals(42, controller.getNode("/subnet/negate").getInput("value").intValue());
        Node numberNode = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 20));
        controller.addNode("/", numberNode);
        controller.connect("/", "number", "subnet", "n");
        assertResultsEqual(controller.getRootNode(), controller.getNode("/subnet"), -20.0);
        controller.setPortValue("/number", "value", 55.0);
        assertResultsEqual(controller.getRootNode(), controller.getNode("/subnet"), -55.0);
    }

    @Test
    public void testSimpleUnpublish() {
        Node invertNode = Node.ROOT.withName("negate").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", Node.ROOT.withName("subnet"));
        controller.addNode("/subnet", invertNode);
        controller.publish("/subnet", "negate", "value", "n");
        controller.unpublish("/subnet", "n");
        assertFalse(controller.getNode("/subnet").hasPublishedInput("n"));
    }

    private void createSimpleConnection() {
        Node numberNode = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 20));
        Node invertNode = Node.ROOT.withName("negate").withFunction("math/negate").withInputAdded(Port.floatPort("value", 0));
        controller.addNode("/", numberNode);
        controller.addNode("/", invertNode);
        controller.connect("/", numberNode, invertNode, invertNode.getInput("value"));
    }
}
