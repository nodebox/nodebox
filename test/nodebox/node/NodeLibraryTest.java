package nodebox.node;

import junit.framework.TestCase;
import nodebox.node.polygraph.Polygon;
import nodebox.node.polygraph.Rectangle;

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
        assertTrue(test.getRootNode().contains("alpha"));
        assertTrue(test.getRootNode().contains(alpha));
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

    /**
     * There is a difference between loading a library using a static method on NodeLibrary
     * and using the NodeManager.load(). NodeManager.load() automatically adds the library
     * to the manager, whereas NodeLibrary only uses the given manager to look up prototypes.
     * <p/>
     * This method tests the differences.
     */
    public void testStoreInLibrary() {
        NodeLibraryManager manager;
        NodeLibrary library;
        // First try loading from within the manager
        manager = new NodeLibraryManager();
        library = manager.load(new File("test/polynodes.ndbx"));
        assertTrue(manager.contains("polynodes"));
        assertTrue(library.contains("rect"));
        // Now try loading using the NodeLibrary.load static method.
        manager = new NodeLibraryManager();
        // We pass in the manager to figure out the prototypes.
        library = NodeLibrary.load(new File("test/polynodes.ndbx"), manager);
        assertFalse(manager.contains("polynodes"));
        // You can add the library yourself.
        manager.add(library);
        assertTrue(manager.contains("polynodes"));
    }

    /**
     * Test if connections are persisted.
     */
    public void testStoreConnections() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha", Polygon.class);
        Node beta = Node.ROOT_NODE.newInstance(library, "beta", Polygon.class);
        beta.addPort("polygon", Polygon.class);
        beta.getPort("polygon").connect(alpha);
        assertTrue(alpha.isConnectedTo(beta));
        assertTrue(beta.isConnectedTo(alpha));
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", library.toXml(), manager);
        Node newAlpha = newLibrary.get("alpha");
        Node newBeta = newLibrary.get("beta");
        assertTrue(newAlpha.isConnectedTo(newBeta));
        assertTrue(newBeta.isConnectedTo(newAlpha));
    }


}
