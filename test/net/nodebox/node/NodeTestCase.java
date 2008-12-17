package net.nodebox.node;

import junit.framework.TestCase;

public class NodeTestCase extends TestCase {

    protected NodeManager manager;
    protected NodeType numberType, negateType, addType, multiplyType, testNetworkType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new TestManager();
        numberType = manager.getNodeType("net.nodebox.node.test.number");
        negateType = manager.getNodeType("net.nodebox.node.test.negate");
        addType = manager.getNodeType("net.nodebox.node.test.add");
        multiplyType = manager.getNodeType("net.nodebox.node.test.multiply");
        testNetworkType = manager.getNodeType("net.nodebox.node.test.network");
    }

    public void testDummy() {
        // This needs to be here, otherwise jUnit complains that there are no tests in this class.
    }
}
