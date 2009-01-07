package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Rect;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;

public class CopyNodeTest extends NodeTestCase {

    public void testBasicCopy() {
        Node rect = manager.getNodeType("builtin.rect").createNode();
        Node copy = manager.getNodeType("builtin.copy").createNode();
        copy.getParameter("shape").connect(rect);
        copy.update();
        Grob g = (Grob) copy.getOutputValue();
        assertEquals(new Rect(0, 0, 100, 100), g.getBounds());
        copy.setValue("ty", 100.0);
        copy.setValue("copies", 5);
        copy.update();
        g = (Grob) copy.getOutputValue();
        assertEquals(new Rect(0, 0, 100, 500), g.getBounds());
    }
}
