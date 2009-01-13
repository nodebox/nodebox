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

        Group rectGroup = (Group) g.get(0);
        assertEquals(1, rectGroup.size());
        BezierPath rectPath = (BezierPath) rectGroup.get(0);
        // Compare the X value set on the rect1 node so we know it is the output of the rect1 node.
        assertEquals(25.0, rectPath.getBounds().getX());

        Group ellipseGroup = (Group) g.get(1);
        assertEquals(1, ellipseGroup.size());
        BezierPath ellipsePath = (BezierPath) ellipseGroup.get(0);
        // Compare the X value set on the ellipse1 node
        assertEquals(199.0, ellipsePath.getBounds().getX());

    }
}
