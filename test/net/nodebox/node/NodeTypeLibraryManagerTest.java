package net.nodebox.node;

import java.io.IOException;

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
        NodeTypeLibrary testlib = manager.loadLatestVersion("testlib");
        assertEquals("testlib", testlib.getName());
        assertEquals("0.0.0", testlib.getVersion().toString());
        assertFalse(testlib.isLoaded());
        // Retrieve a type from the library.
        NodeType negateType = testlib.getNodeType("negate");
        // Retrieving a type implicitly loads the library.
        assertTrue(testlib.isLoaded());
        assertEquals("negate", negateType.getIdentifier());
        // Try it out
        Node node = negateType.createNode();
        node.set("value", 42);
        assertTrue(node.update());
        assertEquals(-42, node.getOutputValue());
    }

    public void testPathToLibrary() {
        NodeTypeLibrary lib1 = NodeTypeLibraryManager.pathToLibrary("test", "testlib");
        assertEquals("testlib", lib1.getName());
        assertEquals("0.0.0", lib1.getVersion().toString());

        assertInvalidLibraryPath("", "empty paths should not be accepted");
        assertInvalidLibraryPath("bobby-1.2.3-alpha-beta", "there are too many components");
        assertInvalidLibraryPath("bobby-a.b.c", "version specifiers are not numbers");

        NodeTypeLibrary lib2 = NodeTypeLibraryManager.pathToLibrary("test", "vector-1.2.3");
        assertEquals("vector", lib2.getName());
        assertEquals("1.2.3", lib2.getVersion().toString());
    }

    private void assertInvalidLibraryPath(String path, String reason) {
        try {
            NodeTypeLibraryManager.pathToLibrary("test", path);
            fail("Path \"" + path + "\" should not have been accepted because " + reason + ".");
        } catch (RuntimeException e) {
        }
    }
}
