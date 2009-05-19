package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.node.polygraph.Polygon;
import net.nodebox.node.polygraph.PolygraphLibrary;
import net.nodebox.node.polygraph.Rectangle;
import net.nodebox.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class NodeManagerTest extends TestCase {

    private NodeLibraryManager manager;
    private NodeLibrary testLibrary;

    @Override
    protected void setUp() throws Exception {
        manager = new NodeLibraryManager();
        testLibrary = new NodeLibrary("test");
        // TODO: manager.add(testLibrary);
    }

    public void testLoad() {
        // The example.ndbx references polygraph nodes,
        // so make sure those are loaded.
        PolygraphLibrary polygraph = new PolygraphLibrary();
        manager.add(polygraph);
        manager.load(new File("test/demo.ndbx"));
        assertNotNull(manager.get("demo"));
        assertNotNull(manager.getNode("demo.root"));
        assertNotNull(manager.getNode("demo.rect1"));
        assertNotNull(manager.getNode("demo.move1"));
        Node rect1 = manager.getNode("demo.rect1");
        Node move1 = manager.getNode("demo.move1");
        assertEquals(polygraph.get("rect"), rect1.getPrototype());
        assertEquals(Node.ROOT_NODE, move1.getPrototype());
        assertTrue(rect1.hasParameter("x"));
        assertTrue(rect1.hasParameter("y"));
        assertFalse(rect1.hasParameter("tx"));
        assertTrue(move1.hasParameter("tx"));
        assertTrue(move1.hasParameter("ty"));
        assertFalse(move1.hasParameter("x"));
        // Test connections
        assertTrue(move1.getPort("polygon").isConnectedTo(rect1));
        assertTrue(rect1.isConnectedTo(move1));
        // Try executing
        rect1.update();
        Object obj = rect1.getOutputValue();
        assertEquals(Polygon.class, obj.getClass());
        assertEquals(new Rectangle(0, 0, 100, 100), ((Polygon) obj).getBounds());
        move1.update();
        obj = move1.getOutputValue();
        assertEquals(Polygon.class, obj.getClass());
        assertEquals(new Rectangle(15, -40, 100, 100), ((Polygon) obj).getBounds());
    }

    public void testStoreAndLoad() throws IOException {
        // Create a temporary file to store the nodes in.
        File f = temporaryLibraryFile();
        // The name of the library without extension is how the library
        // will be stored in the manager.
        // Since we generate a temporary file, we need to know its name
        // to retrieve it from the manager.
        String basename = FileUtils.stripExtension(f);

        NodeLibrary storedLibrary = new NodeLibrary(basename);
        Node dotNode = Node.ROOT_NODE.newInstance(storedLibrary, "dot");
        dotNode.addParameter("x", Parameter.Type.FLOAT);
        dotNode.addParameter("y", Parameter.Type.FLOAT);
        Node circleNode = dotNode.newInstance(storedLibrary, "circle");
        circleNode.addParameter("size", Parameter.Type.FLOAT, 50F);
        storedLibrary.add(dotNode);
        storedLibrary.add(circleNode);

        try {
            // Store the node library
            storedLibrary.store(f);
            // Add the new node library to the manager.
            manager.load(f);
            assertTrue(manager.hasNode(basename + ".dot"));
            assertTrue(manager.hasNode(basename + ".circle"));
            Node dot = manager.getNode(basename + ".dot");
            Node circle = manager.getNode(basename + ".circle");
            assertTrue(dot.hasParameter("x"));
            assertTrue(dot.hasParameter("y"));
            assertTrue(circle.hasParameter("size"));
            assertTrue(circle.hasParameter("x"));
            assertEquals(dot, circle.getPrototype());
        } finally {
            f.delete();
        }
    }

    /**
     * Test ordering of nodes and their prototypes.
     * <p/>
     * Since a ndbx file can potentially store a node and its prototype, make sure that the prototype gets
     * stored sequentially before its instance.
     *
     * @throws java.io.IOException when the temporary file could not be created.
     */
    public void testOrderedDependencies() throws IOException {
        // Create a temporary file to store the nodes in.
        File f = temporaryLibraryFile();
        // The library stores nodes in a Set so we can't know the exact ordering of the nodes.
        // Therefore, we test in all directions.
        testOrderedDependencies(f, new String[]{"alpha", "beta", "gamma"});
        testOrderedDependencies(f, new String[]{"alpha", "gamma", "beta"});
        testOrderedDependencies(f, new String[]{"beta", "alpha", "gamma"});
        testOrderedDependencies(f, new String[]{"beta", "gamma", "alpha"});
        testOrderedDependencies(f, new String[]{"gamma", "beta", "alpha"});
        testOrderedDependencies(f, new String[]{"gamma", "alpha", "beta"});
    }

    /**
     * Create a deep inheritance structure, save it and assert it loads correctly.
     * The first name will become the prototype of the second one, and so forth.
     *
     * @param f     the file to save to
     * @param names the names of the nodes to create.
     * @throws java.io.IOException when the temporary file could not be created.
     */
    private void testOrderedDependencies(File f, String[] names) throws IOException {
        resetManager();
        // The name of the library without extension is how the library
        // will be stored in the manager.
        // Since we generate a temporary file, we need to know its name
        // to retrieve it from the manager.
        String basename = FileUtils.stripExtension(f);
        NodeLibrary library = new NodeLibrary(basename);
        Node prototype = Node.ROOT_NODE;
        for (String name : names) {
            Node instance = prototype.newInstance(library, name);
            library.add(instance);
            prototype = instance;
        }
        library.store(f);
        manager.load(f);
        NodeLibrary newLib = manager.get(basename);
        Node newPrototype = Node.ROOT_NODE;
        for (String name : names) {
            Node newInstance = newLib.get(name);
            assertEquals(newPrototype, newInstance.getPrototype());
            newPrototype = newInstance;
        }
    }

    public void testChildNodes() {
        Node net = Node.ROOT_NODE.newInstance(testLibrary, "net");
        Node rect = Node.ROOT_NODE.newInstance(testLibrary, "rect");
        rect.setParent(net);
    }

    //// Helper methods ////

    /**
     * Creates a new manager instance.
     */
    private void resetManager() {
        manager = new NodeLibraryManager();
    }

    /**
     * Create a temporary file to store the nodes in.
     *
     * @return the temporary file
     */
    private File temporaryLibraryFile() {
        try {
            return File.createTempFile("lib", ".ndbx");
        } catch (IOException e) {
            throw new RuntimeException("IOException while creating temporary file.", e);
        }
    }


}
