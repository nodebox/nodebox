package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;
import net.nodebox.node.NodeType;
import net.nodebox.node.ParameterType;

public class VectorNetworkTest extends NodeTestCase {

    public void testType() {
        NodeType vectorNetworkType = manager.getNodeType("builtin.vecnet");
        Node n = vectorNetworkType.createNode();
        assertEquals(ParameterType.Type.GROB_VECTOR, n.getOutputParameter().getType());
    }

}
