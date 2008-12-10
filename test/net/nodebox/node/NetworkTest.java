package net.nodebox.node;

import junit.framework.TestCase;

public class NetworkTest extends TestCase {

    public void testCreate() {
        TestNetwork net = new TestNetwork();
        Node testNode = net.create(TestNode.class);
        assertTrue(net.contains(testNode));
        assertTrue(testNode.inNetwork());
        assertEquals(net, testNode.getNetwork());
    }
}
