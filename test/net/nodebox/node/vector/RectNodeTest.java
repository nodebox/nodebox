package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.graphics.Rect;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;

public class RectNodeTest extends NodeTestCase {

    public void testRectNode() {
        Node r = manager.getNodeType("builtin.rect").createNode();
        r.update();
        Object outputValue = r.getOutputValue();
        if (!(outputValue instanceof Group))
            fail("Output value is not a Group, but " + outputValue);
        Group group = (Group) outputValue;
        assertEquals(1, group.size());
        BezierPath p = (BezierPath) group.get(0);
        assertEquals(new Rect(0, 0, 100, 100), p.getBounds());
    }
}
