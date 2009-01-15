package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Rect;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;

public class RectNodeTest extends NodeTestCase {

    public void testRectNode() {
        Node r = manager.getNodeType("corevector.rect").createNode();
        r.update();
        Object outputValue = r.getOutputValue();
        if (!(outputValue instanceof BezierPath))
            fail("Output value is not a BezierPath, but " + outputValue);
        BezierPath p = (BezierPath) outputValue;
        assertEquals(new Rect(-50, -50, 100, 100), p.getBounds());
    }
}
