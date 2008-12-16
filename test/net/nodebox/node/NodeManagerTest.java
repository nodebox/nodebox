package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.vector.RectNode;

import java.util.ArrayList;
import java.util.List;

public class NodeManagerTest extends TestCase {

    public void testVersionedNodeList() {
        TestNode tn_0_8 = new TestNode();
        tn_0_8.setVersion(0, 8);
        TestNode tn_1_0 = new TestNode();
        TestNode tn_2_0 = new TestNode();
        tn_2_0.setVersion(2, 0);
        TestNode tn_2_1 = new TestNode();
        tn_2_1.setVersion(2, 1);
        // Setup the correct order (newest nodes come first)
        List<Node> orderedNodes = new ArrayList<Node>();
        orderedNodes.add(tn_2_1);
        orderedNodes.add(tn_2_0);
        orderedNodes.add(tn_1_0);
        orderedNodes.add(tn_0_8);
        // Add the nodes in semi-random order.
        NodeManager.VersionedNodeList vnl = new NodeManager.VersionedNodeList();
        vnl.addNode(tn_2_0);
        vnl.addNode(tn_0_8);
        vnl.addNode(tn_1_0);
        vnl.addNode(tn_2_1);
        assertEquals(orderedNodes, vnl.getNodes());
        // Add the nodes in another order.
        vnl = new NodeManager.VersionedNodeList();
        vnl.addNode(tn_0_8);
        vnl.addNode(tn_1_0);
        vnl.addNode(tn_2_0);
        vnl.addNode(tn_2_1);
        assertEquals(orderedNodes, vnl.getNodes());
        assertEquals(tn_2_1, vnl.getLatestVersion());
    }

    public void testBasicLoading() {
        NodeManager m = new NodeManager();
        Node n = m.getNode("net.nodebox.node.vector.RectNode");
        assertNotNull(n);
        assertTrue(RectNode.class.isAssignableFrom(n.getClass()));
    }

}
