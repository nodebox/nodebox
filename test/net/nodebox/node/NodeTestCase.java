package net.nodebox.node;

import junit.framework.TestCase;

public class NodeTestCase extends TestCase {

    protected NodeTypeLibraryManager manager;
    protected NodeType numberType, negateType, addType, multiplyType, multiAddType, testNetworkType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new NodeTypeLibraryManager();
        TestLibrary testlib = new TestLibrary();
        manager.addLibrary(testlib);
        numberType = manager.getNodeType("test.number");
        negateType = manager.getNodeType("test.negate");
        addType = manager.getNodeType("test.add");
        multiplyType = manager.getNodeType("test.multiply");
        multiAddType = manager.getNodeType("test.multiAdd");
        testNetworkType = manager.getNodeType("test.testnet");
    }

    public void testDummy() {
        // This needs to be here, otherwise jUnit complains that there are no tests in this class.
    }
}
