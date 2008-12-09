package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.canvas.CanvasNode;

public class CanvasNodeTest extends TestCase {

    public void testType() {
        CanvasNode n = new CanvasNode();
        assertEquals(Parameter.Type.CANVAS, n.getOutputParameter().getType());
    }
}
