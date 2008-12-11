package net.nodebox.node.vector;

import junit.framework.TestCase;
import net.nodebox.node.Node;

public class VectorNetworkTest extends TestCase {


    public void testVectorNetwork() {
        VectorNetwork net = new VectorNetwork();
        Node rect1 = net.create(RectNode.class);
        net.update();
    }
}
