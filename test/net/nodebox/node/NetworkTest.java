package net.nodebox.node;

public class NetworkTest extends NodeTestCase {

    class TestDataListener implements NetworkDataListener {
        public int dirtyCounter = 0;
        public int updatedCounter = 0;

        public void networkDirty(Network network) {
            ++dirtyCounter;
        }

        public void networkUpdated(Network network) {
            ++updatedCounter;
        }
    }

    public void testCreate() {
        Network net = (Network) testNetworkType.createNode();
        Node testNode = net.create(numberType);
        assertTrue(net.contains(testNode));
        assertTrue(testNode.inNetwork());
        assertEquals(net, testNode.getNetwork());
    }

    public void testDataEvent() {
        TestDataListener l = new TestDataListener();
        Network net = (Network) testNetworkType.createNode();
        net.addNetworkDataListener(l);
        Node n1 = net.create(numberType);
        Node n2 = net.create(numberType);
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
        Network net = (Network) testNetworkType.createNode();
        Node v1 = net.create(numberType);
        v1.setValue("value", 42);
        net.update();
        assertTrue(net.hasError());
        assertEquals(0, net.getOutputValue());
        v1.setRendered();
        net.update();
        assertFalse(net.hasError());
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
        Node rect1 = towerType.create(RectType.class);
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
        // Create network
        Network rootNetwork = (Network) manager.getNodeType("corecanvas.canvasnet").createNode();
        Network vecnet1 = (Network) rootNetwork.create(manager.getNodeType("corevector.vecnet"));
        vecnet1.setPosition(10, 10);
        assertEquals("vecnet1", vecnet1.getName());
        vecnet1.setRendered();
        Node ellipse1 = vecnet1.create(manager.getNodeType("corevector.ellipse"));
        assertEquals("ellipse1", ellipse1.getName());
        ellipse1.setRendered();
        ellipse1.setPosition(100, 30);
        Node transform1 = vecnet1.create(manager.getNodeType("corevector.transform"));
        assertEquals("transform1", transform1.getName());
        transform1.setPosition(40, 80);
        transform1.setRendered();
        transform1.getParameter("shape").connect(ellipse1);
        Node rect1 = vecnet1.create(manager.getNodeType("corevector.rect"));
        assertEquals("ellipse1", ellipse1.getName());
        rect1.setPosition(180, 30);
        Node merge1 = vecnet1.create(manager.getNodeType("corevector.merge"));
        assertEquals("merge1", merge1.getName());
        merge1.getParameter("shapes").connect(transform1);
        merge1.getParameter("shapes").connect(rect1);

        // Write network
        String xmlString = rootNetwork.toXml();

        // Read network
        Network newNetwork = Network.load(manager, xmlString);

        // Perform tests on the network
        assertEquals(rootNetwork.getName(), newNetwork.getName());
        assertTrue(newNetwork.contains("vecnet1"));
        Network nVector1 = (Network) newNetwork.getNode("vecnet1");
        assertTrue(nVector1.contains("ellipse1"));
        assertTrue(nVector1.contains("transform1"));
        Node nEllipse1 = nVector1.getNode("ellipse1");
        Node nTransform1 = nVector1.getNode("transform1");
        Node nRect1 = nVector1.getNode("rect1");
        Node nMerge1 = nVector1.getNode("merge1");
        assertEquals(ellipse1.getValue("x"), nEllipse1.getValue("x"));
        assertEquals(ellipse1.getValue("fill"), nEllipse1.getValue("fill"));
        assertEquals(ellipse1.getValue("stroke"), nEllipse1.getValue("stroke"));
        assertTrue(nEllipse1.isConnected());
        assertTrue(nTransform1.isConnected());
        assertTrue(nTransform1.getParameter("shape").isConnectedTo(nEllipse1));
        assertTrue(nMerge1.getParameter("shapes").isConnectedTo(nRect1));
        assertTrue(nMerge1.getParameter("shapes").isConnectedTo(nTransform1));
        // Check if this is the same connection
        Parameter nShapes = nMerge1.getParameter("shapes");
        Connection c1 = nVector1.getConnection(nTransform1, nShapes);
        Connection c2 = nVector1.getConnection(nRect1, nShapes);
        assertTrue(c1 == c2);
        assertTrue(c1 instanceof MultiConnection);
        // This tests for a bug where the connection would be created twice.
        nMerge1.getParameter("shapes").disconnect();
        assertFalse(nShapes.isConnectedTo(nRect1));
        assertFalse(nShapes.isConnectedTo(nTransform1));
    }

    /**
     * Tests whether the network does copy the output of the rendered node.
     */
    public void testCopy() {
        Network vector1 = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node ellipse1 = vector1.create(manager.getNodeType("corevector.ellipse"));
        ellipse1.setRendered();
        vector1.update();
        assertFalse(vector1.getOutputValue() == ellipse1.getOutputValue());
    }

    public void testCycles() {
        Network net = (Network) testNetworkType.createNode();
        Node n1 = net.create(numberType);
        Node n2 = net.create(numberType);
        Node n3 = net.create(numberType);
        assertFalse(n2.isConnected());
        assertValidConnect(n2, "value", n1);
        assertTrue(n2.isConnected());
        assertTrue(n2.isInputConnectedTo(n1));
        assertTrue(n1.isOutputConnectedTo(n2));
        assertValidConnect(n3, "value", n2);
        assertTrue(n3.isConnected());
        assertTrue(n3.isInputConnectedTo(n2));
        assertTrue(n2.isOutputConnectedTo(n3));
        // Try creating a 2-node cycle.
        assertInvalidConnect(n1, "value", n2);
        // The connection didn't go through, so n1's input is not connected to n2.
        assertFalse(n1.isInputConnectedTo(n2));
        // However the output of n2 is still connected to n1.
        assertTrue(n2.isInputConnectedTo(n1));
        assertTrue(n1.isConnected());
        assertTrue(n2.isConnected());
        // Try creating a 3-node cycle.
        assertInvalidConnect(n1, "value", n3);
        // Test multi-input connections.
        Node n4 = net.create(multiAddType);
        assertValidConnect(n4, "values", n1);
        assertValidConnect(n4, "values", n2);
        assertValidConnect(n4, "values", n3);
        assertInvalidConnect(n4, "values", n4);
        assertInvalidConnect(n1, "value", n4);
    }

    private void assertValidConnect(Node inputNode, String inputParameterName, Node outputNode) {
        try {
            inputNode.getParameter(inputParameterName).connect(outputNode);
        } catch (ConnectionError e) {
            fail("Should not have thrown ConnectionError: " + e);
        }
    }

    private void assertInvalidConnect(Node inputNode, String inputParameterName, Node outputNode) {
        try {
            inputNode.getParameter(inputParameterName).connect(outputNode);
            fail("Should have thrown ConnectionError.");
        } catch (ConnectionError e) {
        }
    }

}
