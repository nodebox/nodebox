package net.nodebox.node;

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

        public void nodeUpdated(Node node) {
            ++updatedCounter;
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
        NodeType vectorNetworkType = manager.getNodeType("net.nodebox.node.vector.network");
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

        // Store/load library
        String xml = testLibrary.toXml();
        NodeLibrary newLibrary = manager.load("newLibrary", xml);
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
        String xml = testLibrary.toXml();
        NodeLibrary newLibrary = manager.load("newLibrary", xml);
        Node newHello = newLibrary.get("hello");
        newHello.update();
        assertEquals("hello", newHello.getOutputValue());
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
