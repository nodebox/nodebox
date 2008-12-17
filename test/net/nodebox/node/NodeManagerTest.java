package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.vector.RectType;

import java.util.ArrayList;
import java.util.List;

public class NodeManagerTest extends TestCase {

    public class TestNodeType extends NodeType {
        public TestNodeType(NodeManager manager) {
            super(manager, "net.nodebox.node.test.testNode", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            return false;
        }
    }


    public void testVersionedNodeList() {
        NodeManager m = new NodeManager();
        TestNodeType tn_0_8 = new TestNodeType(m);
        tn_0_8.setVersion(0, 8);
        TestNodeType tn_1_0 = new TestNodeType(m);
        TestNodeType tn_2_0 = new TestNodeType(m);
        tn_2_0.setVersion(2, 0);
        TestNodeType tn_2_1 = new TestNodeType(m);
        tn_2_1.setVersion(2, 1);
        // Setup the correct order (newest nodes come first)
        List<NodeType> orderedNodes = new ArrayList<NodeType>();
        orderedNodes.add(tn_2_1);
        orderedNodes.add(tn_2_0);
        orderedNodes.add(tn_1_0);
        orderedNodes.add(tn_0_8);
        // Add the nodes in semi-random order.
        NodeManager.VersionedNodeTypeList vnl = new NodeManager.VersionedNodeTypeList();
        vnl.addNodeType(tn_2_0);
        vnl.addNodeType(tn_0_8);
        vnl.addNodeType(tn_1_0);
        vnl.addNodeType(tn_2_1);
        assertEquals(orderedNodes, vnl.getNodeTypes());
        // Add the nodes in another order.
        vnl = new NodeManager.VersionedNodeTypeList();
        vnl.addNodeType(tn_0_8);
        vnl.addNodeType(tn_1_0);
        vnl.addNodeType(tn_2_0);
        vnl.addNodeType(tn_2_1);
        assertEquals(orderedNodes, vnl.getNodeTypes());
        assertEquals(tn_2_1, vnl.getLatestVersion());
    }

    public void testBasicLoading() {
        NodeManager m = new NodeManager();
        NodeType n = m.getNodeType("net.nodebox.node.vector.rect");
        assertNotNull(n);
        assertTrue(RectType.class.isAssignableFrom(n.getClass()));
    }

}
