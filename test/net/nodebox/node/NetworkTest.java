package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Rect;
import net.nodebox.node.vector.CopyNode;
import net.nodebox.node.vector.RectNode;
import net.nodebox.node.vector.VectorNetwork;

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

    public void testCreate() {
        TestNetwork net = new TestNetwork();
        Node testNode = net.create(TestNode.class);
        assertTrue(net.contains(testNode));
        assertTrue(testNode.inNetwork());
        assertEquals(net, testNode.getNetwork());
    }

    public void testDataEvent() {
        TestDataListener l = new TestDataListener();
        TestNetwork net = new TestNetwork();
        net.addNetworkDataListener(l);
        Node n1 = net.create(TestNode.class);
        Node n2 = net.create(TestNode.class);
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
        TestNetwork net = new TestNetwork();
        Node v1 = net.create(TestNode.class);
        net.update();
        assertTrue(net.hasError());
        assertEquals(0, net.getOutputValue());
        v1.setRendered();
        net.update();
        assertFalse(net.hasError());
        assertEquals(42, net.getOutputValue());
    }

    public void testMacro() {
        VectorNetwork towerMacro = new VectorNetwork();
        // myCopyMacro.setDescription("Gets an image and makes points out of it.");
        Parameter pFloorHeight = towerMacro.addParameter("floorHeight", Parameter.Type.FLOAT);
        pFloorHeight.setLabel("Height of Floor");
        Parameter pSize = towerMacro.addParameter("buildingHeight", Parameter.Type.INT);
        pSize.setLabel("Building Height (in floors)");
        // Inner nodes
        Node rect1 = towerMacro.create(RectNode.class);
        Node copy1 = towerMacro.create(CopyNode.class);
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

    public class ValueNode extends Node {

        public ValueNode() {
            super(Parameter.Type.INT);
        }

        protected boolean process(ProcessingContext ctx) {
            setOutputValue(42);
            return true;
        }
    }
}
