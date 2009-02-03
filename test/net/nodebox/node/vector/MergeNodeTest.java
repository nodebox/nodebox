package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTestCase;

public class MergeNodeTest extends NodeTestCase {
    public void testBasicMerge() {
        Network net = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node rect1 = net.create(manager.getNodeType("corevector.rect"));
        rect1.set("x", 25.0);
        Node ellipse1 = net.create(manager.getNodeType("corevector.ellipse"));
        ellipse1.set("x", 199.0);
        Node merge = net.create(manager.getNodeType("corevector.merge"));
        merge.getParameter("shapes").connect(rect1);
        merge.getParameter("shapes").connect(ellipse1);
        merge.update();
        Group g = (Group) merge.getOutputValue();
        assertEquals(2, g.getGrobs().size());

        BezierPath rectPath = (BezierPath) g.get(0);
        // Compare the X value set on the rect1 node so we know it is the output of the rect1 node.
        assertEquals(25.0 - 50.0, rectPath.getBounds().getX());

        BezierPath ellipsePath = (BezierPath) g.get(1);
        // Compare the X value set on the ellipse1 node
        assertEquals(199.0 - 50.0, ellipsePath.getBounds().getX());
    }

    public void testPropagation() {
        // This weird network shape causes errors with the propagation.
        // The problem is the grid node that is connected to two separate copy nodes.
        Network net = (Network) manager.getNodeType("corecanvas.canvasnet").createNode();
        Node rect1 = net.create(manager.getNodeType("corevector.rect"));
        Node rect2 = net.create(manager.getNodeType("corevector.rect"));
        Node copy1 = net.create(manager.getNodeType("corevector.copy"));
        Node copy2 = net.create(manager.getNodeType("corevector.copy"));
        Node grid1 = net.create(manager.getNodeType("corevector.grid"));
        Node merge1 = net.create(manager.getNodeType("corevector.merge"));
        net.connect(rect1, copy1, "shape");
        net.connect(rect2, copy2, "shape");
        net.connect(grid1, copy1, "template");
        net.connect(grid1, copy2, "template");
        net.connect(copy1, merge1, "shapes");
        net.connect(copy2, merge1, "shapes");
        merge1.setRendered();
        net.update();
        assertFalse(rect1.isDirty());
        assertFalse(copy1.isDirty());
        assertFalse(rect2.isDirty());
        assertFalse(copy2.isDirty());
        assertFalse(grid1.isDirty());
        assertFalse(merge1.isDirty());
        rect1.set("x", 50);
        assertTrue(rect1.isDirty());
        assertTrue(copy1.isDirty());
        assertFalse(rect2.isDirty());
        assertFalse(copy2.isDirty());
        assertFalse(grid1.isDirty());
        assertTrue(merge1.isDirty());
        net.update();
        assertFalse(rect1.isDirty());
        assertFalse(copy1.isDirty());
        assertFalse(rect2.isDirty());
        assertFalse(copy2.isDirty());
        assertFalse(grid1.isDirty());
        assertFalse(merge1.isDirty());
        rect2.set("x", 50);
        assertFalse(rect1.isDirty());
        assertFalse(copy1.isDirty());
        assertTrue(rect2.isDirty());
        assertTrue(copy2.isDirty());
        assertFalse(grid1.isDirty());
        assertTrue(merge1.isDirty());
    }


}
