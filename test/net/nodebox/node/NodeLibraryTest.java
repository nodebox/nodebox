package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.polygraph.Polygon;
import net.nodebox.node.polygraph.Rectangle;

import java.io.File;

public class NodeLibraryTest extends TestCase {

    /**
     * Test if changing the node name updates the correspondent mapping in the library.
     */
    public void testNodeNameChange() {
        NodeLibrary test = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(test, "alpha");
        test.add(alpha);
        assertEquals(alpha, test.get("alpha"));
        // now change the name
        alpha.setName("beta");
        assertEquals(alpha, test.get("beta"));
    }

    /**
     * Test if new instance creates it in the correct library.
     */
    public void testNewInstance() {
        NodeLibrary test = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(test, "alpha");
        assertTrue(test.contains("alpha"));

    }

    public void testLoading() {
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary library = manager.load(new File("test/polynodes.ndbx"));
        assertTrue(library.contains("rect"));
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node rect = manager.getNode("polynodes.rect");
        Parameter pX = rect.getParameter("x");
        assertEquals(Parameter.Type.FLOAT, pX.getType());
        Node rect1 = rect.newInstance(testLibrary, "rect1");
        rect1.setValue("x", 20);
        rect1.setValue("y", 30);
        rect1.setValue("width", 40);
        rect1.setValue("height", 50);
        rect1.update();
        Object value = rect1.getOutputValue();
        assertEquals(Polygon.class, value.getClass());
        Polygon polygon = (Polygon) value;
        assertEquals(new Rectangle(20, 30, 40, 50), polygon.getBounds());
    }


}
