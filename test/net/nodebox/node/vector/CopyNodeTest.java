package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Rect;
import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;

public class CopyNodeTest extends NodeTestCase {

    public void testBasicCopy() {
        Network net = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node rect = net.create(manager.getNodeType("corevector.rect"));
        Node copy = net.create(manager.getNodeType("corevector.copy"));
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

    public void testCopyStamping() {
        Network net = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node rect = net.create(manager.getNodeType("corevector.rect"));
        Node copy = net.create(manager.getNodeType("corevector.copy"));
        copy.getParameter("shape").connect(rect);
        copy.set("copies", 10);
        copy.set("expression", "rect1.x = COPY");
        copy.update();
        Grob g = (Grob) copy.getOutputValue();
        assertEquals(new Rect(0, 0, 109, 100), g.getBounds());
    }
}
