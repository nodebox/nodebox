package nodebox.node;

import nodebox.graphics.Color;

/**
 * All tests that have to do with parent/child relationships between nodes.
 */
public class NetworkTest extends NodeTestCase {

    class TestDataListener implements DirtyListener {
        public int dirtyCounter = 0;
        public int updatedCounter = 0;

        public void nodeDirty(Node node) {
            ++dirtyCounter;
        }

        public void nodeUpdated(Node node, ProcessingContext context) {
            ++updatedCounter;
        }
    }

    class TestChildListener implements NodeChildListener {
        public int childAddedCounter = 0;
        public int childRemovedCounter = 0;
        public int connectionAddedCounter = 0;
        public int connectionRemovedCounter = 0;
        public int renderedChildChangedCounter = 0;
        public int childAttributeChangedCounter = 0;

        public void childAdded(Node source, Node child) {
            ++childAddedCounter;
        }

        public void childRemoved(Node source, Node child) {
            ++childRemovedCounter;
        }

        public void connectionAdded(Node source, Connection connection) {
            ++connectionAddedCounter;
        }

        public void connectionRemoved(Node source, Connection connection) {
            ++connectionRemovedCounter;
        }

        public void renderedChildChanged(Node source, Node child) {
            ++renderedChildChangedCounter;
        }

        public void childAttributeChanged(Node source, Node child, NodeAttributeListener.Attribute attribute) {
            ++childAttributeChangedCounter;
        }
    }

    public void testCreate() {
        Node grandParent = testNetworkNode.newInstance(testLibrary, "grandParent");
        Node parent = grandParent.create(testNetworkNode, "parent");
        Node child = parent.create(numberNode);
        assertTrue(grandParent.contains(parent));
        assertTrue(parent.contains(child));
        // Contains doesn't go into child networks.
        assertFalse(grandParent.contains(child));
        assertTrue(child.hasParent());
        assertTrue(grandParent.hasParent());
        assertTrue(child.hasParent());
        assertFalse(testLibrary.getRootNode().hasParent());
        assertEquals(testLibrary.getRootNode(), grandParent.getParent());
        assertEquals(grandParent, parent.getParent());
        assertEquals(parent, child.getParent());
    }

    public void testDataEvent() {
        TestDataListener l = new TestDataListener();
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        net.addDirtyListener(l);
        Node n1 = net.create(numberNode);
        Node n2 = net.create(numberNode);
        assertEquals(0, l.dirtyCounter);
        assertEquals(0, l.updatedCounter);
        n1.setRendered();
        // Network was already dirty from the start, counter is not updated.
        assertEquals(0, l.dirtyCounter);
        assertEquals(0, l.updatedCounter);
        net.update();
        assertEquals(0, l.dirtyCounter);
        assertEquals(1, l.updatedCounter);
        n2.setRendered();
        assertEquals(1, l.dirtyCounter);
        assertEquals(1, l.updatedCounter);
        net.update();
        assertEquals(1, l.dirtyCounter);
        assertEquals(2, l.updatedCounter);
    }

    /**
     * Test if changes to the child data marks the parent as dirty.
     */
    public void testChildDataPropagation() {
        Node net = testNetworkNode.newInstance(testLibrary, "net", Integer.class);
        Node n1 = net.create(numberNode);
        n1.setRendered();
        net.update();
        assertEquals(0, net.getOutputValue());
        // This should mark the net as dirty.
        n1.setValue("value", 42);
        assertTrue(net.isDirty());
        net.update();
        assertEquals(42, net.getOutputValue());
        n1.setExpression("value", "10 + 1");
        assertTrue(net.isDirty());
        net.update();
        assertEquals(11, net.getOutputValue());
        n1.clearExpression("value");
        n1.setValue("value", 33);
        assertTrue(net.isDirty());
        net.update();
        assertEquals(33, net.getOutputValue());
    }

    public void testChildEvent() {
        TestChildListener l1 = new TestChildListener();
        TestChildListener l2 = new TestChildListener();
        Node parent1 = Node.ROOT_NODE.newInstance(testLibrary, "parent1");
        Node parent2 = Node.ROOT_NODE.newInstance(testLibrary, "parent2");
        parent1.addNodeChildListener(l1);
        parent2.addNodeChildListener(l2);
        Node n1 = parent1.create(numberNode);
        assertEquals(1, l1.childAddedCounter);
        n1.setParent(parent2);
        assertEquals(1, l1.childAddedCounter);
        assertEquals(1, l2.childAddedCounter);
        assertEquals(1, l1.childRemovedCounter);
    }

    public void testBasicProcessing() {
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        Node v1 = net.create(numberNode);
        v1.setValue("value", 42);
        assertProcessingError(net, "no child node to render");
        assertEquals(null, net.getOutputValue());
        v1.setRendered();
        net.update();
        assertEquals(42, net.getOutputValue());
    }


    public void testMacro() {
        /*
        NodeType vectorNetworkType = manager.getNodeType("nodebox.node.vector.network");
        NodeType towerType = vectorNetworkType.clone();
        towerType.setDescription("Gets an image and makes points out of it.");
        Parameter pFloorHeight = towerType.addParameter("floorHeight", Parameter.Type.FLOAT);
        pFloorHeight.setLabel("Height of Floor");
        Parameter pSize = towerType.addParameter("buildingHeight", Parameter.Type.INT);
        pSize.setLabel("Building Height (in floors)");
        // Inner nodes
        Node rect1 = towerType.create(RectNode.class);
        Node copy1 = towerType.create(CopyType.class);
        rect1.getParameter("width").set(50.0);
        rect1.getParameter("height").setExpression("network.floorHeight");
        copy1.getParameter("shape").connect(rect1);
        copy1.getParameter("copies").setExpression("network.buildingHeight");
        copy1.getParameter("ty").setExpression("network.floorHeight");
        copy1.setRendered();
        // Execute the macro.
        towerType.setValue("floorHeight", 20.0);
        towerType.setValue("buildingHeight", 8);
        towerType.update();
        Grob g = (Grob) towerType.getOutputValue();
        assertEquals(new Rect(0, 0, 50.0, 160.0), g.getBounds());
        */
    }

    public void testPersistence() {
        Node polynet1 = manager.getNode("polygraph.network").newInstance(testLibrary, "polynet1");
        //Node polynet1 = testLibrary.getRootNode().create(manager.getNode("polygraph.network"), "polynet1");
        polynet1.setPosition(10, 10);
        assertEquals("polynet1", polynet1.getName());
        polynet1.setRendered();
        Node polygon1 = polynet1.create(manager.getNode("polygraph.polygon"));
        assertEquals("polygon1", polygon1.getName());
        polygon1.setRendered();
        polygon1.setPosition(100, 30);
        Node translate1 = polynet1.create(manager.getNode("polygraph.translate"));
        assertEquals("translate1", translate1.getName());
        translate1.setPosition(40, 80);
        translate1.setRendered();
        translate1.getPort("polygon").connect(polygon1);
        Node rect1 = polynet1.create(manager.getNode("polygraph.rect"));
        assertEquals("rect1", rect1.getName());
        rect1.setPosition(180, 30);
        Node merge1 = polynet1.create(manager.getNode("polygraph.merge"));
        assertEquals("merge1", merge1.getName());
        merge1.getPort("polygons").connect(translate1);
        merge1.getPort("polygons").connect(rect1);

        NodeLibrary newLibrary = storeAndLoad(testLibrary);
        Node newRoot = newLibrary.getRootNode();

        assertEquals("root", newRoot.getName());
        assertTrue(newRoot.contains("polynet1"));
        Node nPolynet1 = newRoot.getChild("polynet1");
        assertTrue(nPolynet1.contains("polygon1"));
        assertTrue(nPolynet1.contains("translate1"));
        Node nPolygon1 = nPolynet1.getChild("polygon1");
        Node nTranslate1 = nPolynet1.getChild("translate1");
        Node nRect1 = nPolynet1.getChild("rect1");
        Node nMerge1 = nPolynet1.getChild("merge1");
        assertEquals(polygon1.getValue("x"), nPolygon1.getValue("x"));
        assertEquals(polygon1.getValue("fill"), nPolygon1.getValue("fill"));
        assertEquals(polygon1.getValue("stroke"), nPolygon1.getValue("stroke"));
        assertTrue(nPolygon1.isConnected());
        assertTrue(nTranslate1.isConnected());
        assertTrue(nTranslate1.getPort("polygon").isConnectedTo(nPolygon1));
        assertTrue(nMerge1.getPort("polygons").isConnectedTo(nRect1));
        assertTrue(nMerge1.getPort("polygons").isConnectedTo(nTranslate1));
        // Check if this is the same connection
        Port nPolygons = nMerge1.getPort("polygons");
        assertEquals(1, nTranslate1.getDownstreamConnections().size());
        assertEquals(1, nRect1.getDownstreamConnections().size());
        Connection c1 = nTranslate1.getDownstreamConnections().iterator().next();
        Connection c2 = nRect1.getDownstreamConnections().iterator().next();
        assertTrue(c1 == c2);
        // This tests for a bug where the connection would be created twice.
        nMerge1.getPort("polygons").disconnect();
        assertFalse(nPolygons.isConnectedTo(nRect1));
        assertFalse(nPolygons.isConnectedTo(nTranslate1));
    }

    /**
     * Test if code can be persisted correctly.
     */
    public void testCodeLoading() {
        Node hello = Node.ROOT_NODE.newInstance(testLibrary, "hello");
        String code = "def cook(self):\n  return 'hello'";
        hello.setValue("_code", new PythonCode(code));
        hello.update();
        assertEquals("hello", hello.getOutputValue());
        // Store/load library
        NodeLibrary newLibrary = storeAndLoad(testLibrary);
        Node newHello = newLibrary.getRootNode().getChild("hello");
        newHello.update();
        assertEquals("hello", newHello.getOutputValue());
    }

    /**
     * Test if all types load correctly.
     */
    public void testTypeLoading() {
        Node allTypes = Node.ROOT_NODE.newInstance(testLibrary, "allTypes");
        allTypes.addParameter("i", Parameter.Type.INT, 42);
        allTypes.addParameter("f", Parameter.Type.FLOAT, 42F);
        allTypes.addParameter("s", Parameter.Type.STRING, "42");
        allTypes.addParameter("c", Parameter.Type.COLOR, new Color(0.4, 0.2, 0.1, 0.9));
        NodeLibrary newLibrary = storeAndLoad(testLibrary);
        Node newAllTypes = newLibrary.getRootNode().getChild("allTypes");
        Parameter pI = newAllTypes.getParameter("i");
        Parameter pF = newAllTypes.getParameter("f");
        Parameter pS = newAllTypes.getParameter("s");
        Parameter pC = newAllTypes.getParameter("c");
        assertEquals(Parameter.Type.INT, pI.getType());
        assertEquals(Parameter.Type.FLOAT, pF.getType());
        assertEquals(Parameter.Type.STRING, pS.getType());
        assertEquals(Parameter.Type.COLOR, pC.getType());
        assertEquals("i", pI.getName());
        assertEquals(42, pI.getValue());
        assertEquals(42F, pF.getValue());
        assertEquals("42", pS.getValue());
        assertEquals(new Color(0.4, 0.2, 0.1, 0.9), pC.getValue());
    }

    /**
     * Tests whether the network does copy the output of the rendered node.
     * <p/>
     * Output values are not copied, since we have no reliable way to clone them.
     */
    public void testCopy() {
        Node net1 = Node.ROOT_NODE.newInstance(testLibrary, "net");
        Node rect1 = net1.create(rectNode);
        rect1.setRendered();
        net1.update();
        assertTrue(net1.getOutputValue() == rect1.getOutputValue());
    }

    public void testCycles() {
        Node net = testNetworkNode.newInstance(testLibrary, "net1");
        Node n1 = net.create(numberNode);
        Node n2 = net.create(numberNode);
        Node n3 = net.create(numberNode);
        assertFalse(n2.isConnected());
        assertValidConnect(n2, "valuePort", n1);
        assertTrue(n2.isConnected());
        assertTrue(n2.isInputConnectedTo(n1));
        assertTrue(n1.isOutputConnectedTo(n2));
        assertValidConnect(n3, "valuePort", n2);
        assertTrue(n3.isConnected());
        assertTrue(n3.isInputConnectedTo(n2));
        assertTrue(n2.isOutputConnectedTo(n3));
        // Try creating a 2-node cycle.
        assertInvalidConnect(n1, "valuePort", n2);
        // The connection didn't go through, so n1's input is not connected to n2.
        assertFalse(n1.isInputConnectedTo(n2));
        // However the output of n2 is still connected to n1.
        assertTrue(n2.isInputConnectedTo(n1));
        assertTrue(n1.isConnected());
        assertTrue(n2.isConnected());
        // Try creating a 3-node cycle.
        assertInvalidConnect(n1, "valuePort", n3);
        // Test multi-input connections.
        Node n4 = net.create(multiAddNode);
        assertValidConnect(n4, "values", n1);
        assertValidConnect(n4, "values", n2);
        assertValidConnect(n4, "values", n3);
        assertInvalidConnect(n4, "values", n4);
        assertInvalidConnect(n1, "valuePort", n4);
    }

    public void testReparenting() {
        Node net1 = Node.ROOT_NODE.newInstance(testLibrary, "net1");
        Node net2 = Node.ROOT_NODE.newInstance(testLibrary, "net2");
        Node number1 = net1.create(numberNode);
        Node negate1 = net1.create(negateNode);
        Node addConstant1 = net1.create(addConstantNode);
        number1.setValue("value", 5);
        negate1.getPort("value").connect(number1);
        addConstant1.getPort("value").connect(negate1);
        addConstant1.setValue("constant", -3);
        addConstant1.setRendered();
        assertTrue(negate1.isConnected());
        assertTrue(number1.isConnected());
        assertTrue(addConstant1.isConnected());
        net1.update();
        assertEquals(-8, net1.getOutputValue());
        negate1.setParent(net2);
        assertFalse(negate1.isConnected());
        assertFalse(number1.isConnected());
        assertFalse(addConstant1.isConnected());
    }

    /**
     * Test if errors occur in the right level, for the parent or children.
     */
    public void testErrorPropagation() {
        Node net = Node.ROOT_NODE.newInstance(testLibrary, "net", Integer.class);
        Node number1 = net.create(numberNode);
        // Set an invalid expression. This error is caused by a parameter on the number1 node,
        // therefore the number1 node should have its error flag set.
        number1.setExpression("value", "***");
        number1.setRendered();
        try {
            net.update();
            fail("Update should have thrown an error.");
        } catch (ProcessingError e) {
            // The network also has its error flag set since errors propagate.
            assertTrue(net.hasError());
            assertTrue(number1.hasError());
            assertFalse(net.isDirty());
            assertFalse(number1.isDirty());
        }
        // Fix the error by clearing the expression and setting a regular value.
        number1.clearExpression("value");
        number1.setValue("value", 42);
        assertTrue(net.isDirty());
        net.update();
        assertFalse(net.hasError());
        assertFalse(number1.hasError());
        assertEquals(42, number1.getOutputValue());
        assertEquals(42, net.getOutputValue());
    }

    /**
     * Store the library in XML, then load it under the name "newLibrary".
     *
     * @param lib the library to store.
     * @return the new library object.
     */
    private NodeLibrary storeAndLoad(NodeLibrary lib) {
        String xml = testLibrary.toXml();
        return manager.load("newLibrary", xml);
    }

    private void assertValidConnect(Node inputNode, String inputPortName, Node outputNode) {
        assert inputNode != null;
        assert inputPortName != null;
        assert outputNode != null;
        try {
            inputNode.getPort(inputPortName).connect(outputNode);
        } catch (IllegalArgumentException e) {
            fail("Should not have thrown IllegalArgumentException: " + e);
        }
    }

    private void assertInvalidConnect(Node inputNode, String inputPortName, Node outputNode) {
        try {
            inputNode.getPort(inputPortName).connect(outputNode);
            fail("Should have thrown IllegalArgumentException.");
        } catch (IllegalArgumentException ignored) {
        }
    }

}
