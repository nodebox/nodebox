package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.canvas.CanvasNetwork;

public class CanvasNetworkTest extends TestCase {

    public void testType() {
        CanvasNetwork n = new CanvasNetwork();
        assertEquals(Parameter.Type.GROB_CANVAS, n.getOutputParameter().getType());
    }
}
