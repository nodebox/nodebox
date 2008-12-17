package net.nodebox.node;

import junit.framework.TestCase;

public class NetworkTest extends TestCase {

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

    private NodeManager manager;
    private NodeType testNetworkType;
    private NodeType numberType;

    @Override
    protected void setUp() throws Exception {
        manager = new TestManager();
        testNetworkType = manager.getNodeType("net.nodebox.node.test.network");
        numberType = manager.getNodeType("net.nodebox.node.test.number");
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

    /*
    public void testMacro() {
        VectorNetworkType towerMacro = new VectorNetworkType();
        // myCopyMacro.setDescription("Gets an image and makes points out of it.");
        Parameter pFloorHeight = towerMacro.addParameter("floorHeight", Parameter.Type.FLOAT);
        pFloorHeight.setLabel("Height of Floor");
        Parameter pSize = towerMacro.addParameter("buildingHeight", Parameter.Type.INT);
        pSize.setLabel("Building Height (in floors)");
        // Inner nodes
        Node rect1 = towerMacro.create(RectType.class);
        Node copy1 = towerMacro.create(CopyType.class);
        rect1.getParameter("width").set(50.0);
        rect1.getParameter("height").setExpression("network.floorHeight");
        copy1.getParameter("shape").connect(rect1);
        copy1.getParameter("copies").setExpression("network.buildingHeight");
        copy1.getParameter("ty").setExpression("network.floorHeight");
        copy1.setRendered();
        // Execute the macro.
        towerMacro.setValue("floorHeight", 20.0);
        towerMacro.setValue("buildingHeight", 8);
        towerMacro.update();
        Grob g = (Grob) towerMacro.getOutputValue();
        assertEquals(new Rect(0, 0, 50.0, 160.0), g.getBounds());
    }
    */

    public void testPersistence() {
        NodeManager manager = new NodeManager();

        // Create network
        Network rootNetwork = (Network) manager.getNodeType("net.nodebox.node.canvas.network").createNode();
        Network vector1 = (Network) rootNetwork.create(manager.getNodeType("net.nodebox.node.vector.network"));
        vector1.setPosition(10, 10);
        vector1.setRendered();
        Node ellipse1 = vector1.create(manager.getNodeType("net.nodebox.node.vector.ellipse"));
        ellipse1.setRendered();
        ellipse1.setPosition(100, 30);
        Node transform1 = vector1.create(manager.getNodeType("net.nodebox.node.vector.transform"));
        transform1.setPosition(40, 80);
        transform1.setRendered();
        transform1.getParameter("shape").connect(ellipse1);

        // Write network
        String xmlString = rootNetwork.toXml();
        System.out.println("xmlString = " + xmlString);

        // Read network
        Network newNetwork = Network.load(manager, xmlString);

        // TODO: Equals tests for identity. We need to correctly test this.
        // assertEquals(rootNetwork, newNetwork);
    }

}
