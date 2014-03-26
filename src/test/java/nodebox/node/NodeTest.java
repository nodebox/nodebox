package nodebox.node;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NodeTest {

    @Test
    public void testIsCompatible() {
        assertTrue("Nodes with the same type are always compatible.", Node.isCompatible("foo", "foo"));

        assertTrue("Everything can be converted to a string.", Node.isCompatible("foo", Port.TYPE_STRING));
        assertFalse("But not every type can take in a string.", Node.isCompatible(Port.TYPE_STRING, "foo"));

        assertTrue("Floating-point numbers will be rounded to integers.", Node.isCompatible(Port.TYPE_FLOAT, Port.TYPE_INT));
        assertTrue("Integers will be converted to floating-point numbers.", Node.isCompatible(Port.TYPE_INT, Port.TYPE_FLOAT));

        assertTrue("Floating-point numbers can be converted to points.", Node.isCompatible(Port.TYPE_FLOAT, Port.TYPE_POINT));
        assertTrue("Integers can be converted to points.", Node.isCompatible(Port.TYPE_INT, Port.TYPE_POINT));
        assertFalse("Points can not be converted to numbers.", Node.isCompatible(Port.TYPE_POINT, Port.TYPE_FLOAT));
    }

    @Test
    public void testPath() {
        assertEquals("/child", Node.path("/", Node.ROOT.withName("child")));
        assertEquals("/parent/child", Node.path("/parent", Node.ROOT.withName("child")));
        assertEquals("/parent", Node.path("/parent", ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativePath() {
        Node.path("", Node.ROOT.withName("child"));
    }

    @Test
    public void testRootName() {
        assertEquals("node", Node.ROOT.getName());
        // The moment we extend from root, the name changes.
        assertEquals("node1", Node.ROOT.withFunction("test").getName());
    }

    @Test
    public void testNetworkName() {
        assertEquals("network", Node.NETWORK.getName());
        // The moment we extend from network, the name changes.
        assertEquals("network1", Node.NETWORK.withOutputType("int").getName());
    }

    @Test(expected = InvalidNameException.class)
    public void testReservedRootName() {
        Node.ROOT.withName("node").getName();
    }

    @Test(expected = InvalidNameException.class)
    public void testReservedNetworkName() {
        Node.NETWORK.withName("network").getName();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildWithInvalidName() {
        Node root = Node.ROOT.withName("root");
        assertEquals("root", root.getName());
        Node.NETWORK.withChildAdded(root);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChildInvalidRenaming() {
        Node.NETWORK
                .withChildAdded(Node.ROOT.withName("child"))
                .withChildRenamed("child", "root");
    }

    @Test
    public void testWithChildCommented() {
        Node net = Node.NETWORK.withName("net");
        Node alpha = Node.ROOT.withName("alpha");
        String testComment = "test";
        net = net.withChildAdded(alpha).withChildCommented("alpha", testComment);
        assertTrue(net.getChild("alpha").getComment().equals(testComment));
    }

    @Test
    public void testChangeFunction() {
        Node test = Node.ROOT.extend().withFunction("test/test");
        assertEquals("test/test", test.getFunction());
    }

    @Test
    public void testPrototype() {
        Node alpha = Node.ROOT.withName("alpha");
        assertSame("Using withXXX on the root sets the root automatically on the prototype.",
                alpha.getPrototype(), Node.ROOT);
        Node beta = alpha.withName("beta");
        assertSame("Using withXXX methods doesn't automatically change the prototype.",
                beta.getPrototype(), Node.ROOT);
        Node gamma = alpha.extend().withName("gamma");
        assertSame("Use extend() to change the prototype.",
                gamma.getPrototype(), alpha);
    }

    @Test
    public void testNodeNaming() {
        Node n = Node.ROOT;
        assertInvalidName(n, "1234", "names cannot start with a digit.");

        assertInvalidName(n, "__reserved", "names cannot start with double underscores");
        assertInvalidName(n, "what!", "Only lowercase, numbers and underscore are allowed");
        assertInvalidName(n, "$-#34", "Only lowercase, numbers and underscore are allowed");
        assertInvalidName(n, "", "names cannot be empty");
        assertInvalidName(n, "very_very_very_very_very_very_long_name", "names cannot be longer than 30 characters");

        assertValidName(n, "radius");
        assertValidName(n, "_test");
        assertValidName(n, "_");
        assertValidName(n, "_1234");
        assertValidName(n, "a1234");
        assertValidName(n, "node1");
        assertValidName(n, "UPPERCASE");
        assertValidName(n, "uPpercase");
    }

    @Test
    public void testPortOrder() {
        Port pAlpha = Port.intPort("alpha", 1);
        Port pBeta = Port.intPort("beta", 2);
        Node original = Node.ROOT.withInputAdded(pAlpha).withInputAdded(pBeta);
        ImmutableList<String> orderedPortNames = ImmutableList.of("alpha", "beta");
        assertEquals(orderedPortNames, portNames(original));

        Node alphaChanged = original.withInputValue("alpha", 11L);
        assertEquals(orderedPortNames, portNames(alphaChanged));
    }

    @Test
    public void testPorts() {
        Port pX = Port.floatPort("x", 0);
        Port pY = Port.floatPort("y", 0);
        Node rectNode1 = Node.ROOT.withName("rect1").withInputAdded(pX);
        assertNull(getNodePort(Node.ROOT, "x"));
        assertSame(pX, getNodePort(rectNode1, "x"));
        Node rectNode2 = newNodeWithPortAdded(rectNode1.withName("rect2"), pY);
        assertSame(pX, getNodePort(rectNode2, "x"));
        assertSame(pY, getNodePort(rectNode2, "y"));
        assertNull(getNodePort(rectNode1, "y"));
        assertNodePortsSizeEquals(0, Node.ROOT);
        assertNodePortsSizeEquals(1, rectNode1);
        assertNodePortsSizeEquals(2, rectNode2);
        Node rectNode3 = newNodeWithPortRemoved(rectNode2.withName("rect3"), "x");
        assertNodePortsSizeEquals(2, rectNode2);
        assertNodePortsSizeEquals(1, rectNode3);
        assertNull(getNodePort(rectNode3, "x"));
        assertSame(pY, getNodePort(rectNode3, "y"));
    }

    private Node newNodeWithPortAdded(Node node, Port port) {
        return node.withInputAdded(port);
    }

    private Node newNodeWithPortRemoved(Node node, String portName) {
        return node.withInputRemoved(portName);
    }

    private Port getNodePort(Node node, String portName) {
        return node.getInput(portName);
    }

    private void assertNodePortsSizeEquals(int expected, Node node) {
        assertEquals(expected, node.getInputs().size());
    }

    public List<String> portNames(Node n) {
        List<String> portNames = new LinkedList<String>();
        for (Port p : n.getInputs()) {
            portNames.add(p.getName());
        }
        return portNames;
    }

    //// Helper functions ////

    private void assertInvalidName(Node n, String newName, String reason) {
        try {
            n.withName(newName);
            fail("the following condition was not met: " + reason);
        } catch (InvalidNameException ignored) {
        }
    }

    private void assertValidName(Node n, String newName) {
        try {
            Node newNode = n.withName(newName);
            assertEquals(newName, newNode.getName());
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

}
