package nodebox.node;

import junit.framework.TestCase;
import nodebox.client.PlatformUtils;
import nodebox.graphics.Geometry;
import nodebox.graphics.Rect;
import nodebox.graphics.Transform;

/**
 * Tests the Core Vector nodes
 */
public class CoreVectorTest extends TestCase {

    private NodeLibraryManager manager;
    private NodeLibrary library;
    private Node rootNode;

    public void setUp() {
        manager = new NodeLibraryManager();
        manager.addSearchPath(PlatformUtils.getApplicationScriptsDirectory());
        manager.lookForLibraries();
        library = new NodeLibrary("test");
        rootNode = library.getRootNode();
    }

    private Node createNode(String name) {
        Node prototype = manager.getNode("corevector." + name);
        return rootNode.create(prototype);
    }

    private Geometry updateNode(Node n) {
        n.update();
        assertNotNull(n.getOutputValue());
        assertEquals(Geometry.class, n.getOutputValue().getClass());
        return (Geometry) n.getOutputValue();
    }

    private void assertUpdateNull(Node n) {
        n.update();
        assertNull(n.getOutputValue());
    }

    public void testGenerator() {
        Node generator = createNode("generator");
        Geometry geo = updateNode(generator);
        assertEquals(Rect.centeredRect(0, 0, 100, 100), geo.getBounds());
    }

    public void testFilter() {
        Node filter = createNode("filter");
        assertUpdateNull(filter);
        Node generator = createNode("generator");
        filter.getPort("shape").connect(generator);
        Geometry geo = updateNode(filter);
        Transform t = Transform.rotated(45f);
        Rect r = t.map(Rect.centeredRect(0, 0, 100, 100));
        // TODO: Check geometry on this.
        // assertEquals(r.getX(), geo.getBounds().getX(), 0.01f);
        // assertEquals(r.getY(), geo.getBounds().getY(), 0.01f);
        // assertEquals(r.getWidth(), geo.getBounds().getWidth(), 0.01f);
        // assertEquals(r.getHeight(), geo.getBounds().getHeight(), 0.01f);
    }

    public void testEllipse() {
        Node ellipse = createNode("ellipse");
        Geometry geo = updateNode(ellipse);
        assertEquals(Rect.centeredRect(0, 0, 100, 100), geo.getBounds());
    }

    // TODO: Test all core vector nodes.

}
