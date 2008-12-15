package net.nodebox.node.vector;

import junit.framework.TestCase;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Rect;

public class CopyNodeTest extends TestCase {

    public void testBasicCopy() {
        RectNode rect = new RectNode();
        CopyNode copy = new CopyNode();
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
