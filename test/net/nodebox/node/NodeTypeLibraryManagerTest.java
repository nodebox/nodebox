package net.nodebox.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeTypeLibraryManagerTest extends NodeTypeTestCase {


    public void testLoad() {
        initJython();
        NodeTypeLibraryManager manager = new NodeTypeLibraryManager();
        try {
            manager.addSearchPath(getLibrariesDirectory());
        } catch (IOException e) {
            fail("Failed with IOException: " + e);
        }
        // Load the test library
        NodeTypeLibrary testlib = manager.getLibrary("testlib");
        assertEquals("testlib", testlib.getName());
        assertEquals("0.0.0", testlib.getVersion().toString());
        assertFalse(testlib.isLoaded());
        // Retrieve a type from the library.
        NodeType negateType = testlib.getNodeType("negate");
        // Retrieving a type implicitly loads the library.
        assertTrue(testlib.isLoaded());
        assertEquals("negate", negateType.getName());
        // Try it out
        Node node = negateType.createNode();
        node.set("value", 42);
        assertTrue(node.update());
        assertEquals(-42, node.getOutputValue());
    }

    public void testPathToLibrary() {
        NodeTypeLibrary lib1 = NodeTypeLibraryManager.pathToLibrary(getLibrariesDirectory(), "testlib");
        assertEquals("testlib", lib1.getName());
        assertEquals("0.0.0", lib1.getVersion().toString());

        assertInvalidLibraryPath("", "empty paths should not be accepted");
        assertInvalidLibraryPath("bobby-1.2.3-alpha-beta", "there are too many components");
        assertInvalidLibraryPath("bobby-a.b.c", "version specifiers are not numbers");

        // TODO: Add more test libraries
//        NodeTypeLibrary lib2 = NodeTypeLibraryManager.pathToLibrary(getLibrariesDirectory(), "vector-1.2.3");
//        assertEquals("vector", lib2.getName());
//        assertEquals("1.2.3", lib2.getVersion().toString());
    }

    public void testVersionedLibraries() {
        NodeTypeLibraryManager m = new NodeTypeLibraryManager();
        NodeTypeLibrary tn_0_8 = new CoreNodeTypeLibrary("test", new Version(0, 8, 0));
        NodeTypeLibrary tn_1_0 = new CoreNodeTypeLibrary("test", new Version(1, 0, 0));
        NodeTypeLibrary tn_2_0 = new CoreNodeTypeLibrary("test", new Version(2, 0, 0));
        NodeTypeLibrary tn_2_1 = new CoreNodeTypeLibrary("test", new Version(2, 1, 0));
        // Setup the correct order (newest nodes come first)
        List<NodeTypeLibrary> orderedLibraries = new ArrayList<NodeTypeLibrary>();
        orderedLibraries.add(tn_2_1);
        orderedLibraries.add(tn_2_0);
        orderedLibraries.add(tn_1_0);
        orderedLibraries.add(tn_0_8);
        // Add the nodes in semi-random order.
        NodeTypeLibraryManager.VersionedLibraryList vll = new NodeTypeLibraryManager.VersionedLibraryList();
        vll.addLibrary(tn_2_0);
        vll.addLibrary(tn_0_8);
        vll.addLibrary(tn_1_0);
        vll.addLibrary(tn_2_1);
        assertEquals(orderedLibraries, vll.getLibraries());
        // Add the nodes in another order.
        vll = new NodeTypeLibraryManager.VersionedLibraryList();
        vll.addLibrary(tn_0_8);
        vll.addLibrary(tn_1_0);
        vll.addLibrary(tn_2_0);
        vll.addLibrary(tn_2_1);
        assertEquals(orderedLibraries, vll.getLibraries());
        assertEquals(tn_2_1, vll.getLatestVersion());
    }

    private void assertInvalidLibraryPath(String path, String reason) {
        try {
            NodeTypeLibraryManager.pathToLibrary("test", path);
            fail("Path \"" + path + "\" should not have been accepted because " + reason + ".");
        } catch (RuntimeException e) {
        }
    }
}
