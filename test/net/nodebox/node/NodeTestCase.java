package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.polygraph.PolygraphLibrary;

public class NodeTestCase extends TestCase {

    protected NodeLibraryManager manager;
    protected NodeLibrary testNodes, polygraphLibrary, testLibrary;
    protected Node numberNode, negateNode, addNode, addDirectNode, addConstantNode, multiplyNode, multiAddNode,
            floatNegateNode, convertToUppercaseNode, testNetworkNode,
            polygonNode, rectNode, translateNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new NodeLibraryManager();
        testNodes = new TestNodes();
        testLibrary = new NodeLibrary("test");
        polygraphLibrary = new PolygraphLibrary();
        manager.add(testNodes);
        manager.add(polygraphLibrary);
        numberNode = manager.getNode("testlib.number");
        negateNode = manager.getNode("testlib.negate");
        addNode = manager.getNode("testlib.add");
        addDirectNode = manager.getNode("testlib.addDirect");
        addConstantNode = manager.getNode("testlib.addConstant");
        multiplyNode = manager.getNode("testlib.multiply");
        multiAddNode = manager.getNode("testlib.multiAdd");
        floatNegateNode = manager.getNode("testlib.floatNegate");
        convertToUppercaseNode = manager.getNode("testlib.convertToUppercase");
        testNetworkNode = manager.getNode("testlib.testnet");
        polygonNode = manager.getNode("polygraph.polygon");
        rectNode = manager.getNode("polygraph.rect");
        translateNode = manager.getNode("polygraph.translate");
    }

    public void testDummy() {
        // This needs to be here, otherwise jUnit complains that there are no tests in this class.
    }

    //// Custom assertions ////

    public void assertConnectionError(Node inputNode, String inputPort, Node outputNode, String message) {
        try {
            inputNode.getPort(inputPort).connect(outputNode);
            fail(message);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void assertProcessingError(Node node, Class expectedErrorClass) {
        try {
            node.update();
            fail("The node " + node + " should have failed processing.");
        } catch (ProcessingError e) {
            // ProcessingErrors are not wrapped, so check if the expected error is a ProcessingError.
            if (expectedErrorClass == ProcessingError.class) return;
            assertEquals(expectedErrorClass, e.getCause().getClass());
        }
    }

    public void assertProcessingError(Node node, String expectedErrorMessage) {
        try {
            node.update();
            fail("The node " + node + " should have failed processing.");
        } catch (ProcessingError e) {
            assertTrue("Was expecting error " + expectedErrorMessage + ", got " + e.toString(),
                    e.toString().toLowerCase().contains(expectedErrorMessage.toLowerCase()));
        }
    }

}
