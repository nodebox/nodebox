package net.nodebox.node.canvas;

import junit.framework.Assert;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;
import net.nodebox.node.NodeType;
import net.nodebox.node.ParameterType;

public class CanvasNetworkTest extends NodeTestCase {

    public void testType() {
        NodeType canvasNetworkType = manager.getNodeType("builtin.canvasnet");
        Node n = canvasNetworkType.createNode();
        Assert.assertEquals(ParameterType.Type.GROB_CANVAS, n.getOutputParameter().getType());
    }
}
