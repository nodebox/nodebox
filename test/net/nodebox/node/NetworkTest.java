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
