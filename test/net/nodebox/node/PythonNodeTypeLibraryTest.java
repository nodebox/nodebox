package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.client.FileUtils;
import net.nodebox.client.PlatformUtils;
import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Rect;
import net.nodebox.util.PythonUtils;

import java.io.File;
import java.io.IOException;

public class PythonNodeTypeLibraryTest extends TestCase {

    private File librariesDirectory;
    private NodeTypeLibraryManager manager;
    private NetworkEventHandler networkEventHandler;

    @Override
    protected void setUp() throws Exception {
        PythonUtils.initializePython();
        // The librariesDirectory is used in the testCreatePythonLibrary method.
        librariesDirectory = FileUtils.createTemporaryDirectory("testlibs");
        // Create a new NTLM with a custom search path set to the temporary libraries directory.
        manager = new NodeTypeLibraryManager(librariesDirectory);
        networkEventHandler = new NetworkEventHandler();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(librariesDirectory);
    }

    public void testCreatePythonLibrary() throws IOException {
        // Create a new Python library in the test libraries folder.
        String libraryName = "tlib";
        NodeTypeLibrary library = manager.createPythonLibrary(libraryName);
        // Check if the library's path is correct.
        assertEquals(librariesDirectory.getCanonicalPath() + PlatformUtils.SEP + libraryName, library.getPath());
        // Check if the library loads correctly.
        assertTrue(library.hasNodeType("test"));
        NodeType testType = library.getNodeType("test");
        assertTrue(testType.hasParameterType("x"));
        assertTrue(testType.hasParameterType("y"));
        // Create a test node and examine the output
        Node testNode = testType.createNode();
        testNode.set("x", 50);
        testNode.set("y", 30);
        testNode.update();
        assertTrue(testNode.getOutputValue() instanceof BezierPath);
        BezierPath p = (BezierPath) testNode.getOutputValue();
        assertEquals(new Rect(25, 5, 50, 50), p.getBounds());
        FileUtils.deleteDirectory(librariesDirectory);
    }

    public void testReload() throws IOException {
        NodeType canvasnetType = manager.getNodeType("corecanvas.canvasnet");
        Network net = (Network) canvasnetType.createNode();
        net.addNetworkEventListener(networkEventHandler);
        // Create a new Python library in the test libraries folder.
        String libraryName = "tlib";
        NodeTypeLibrary library = manager.createPythonLibrary(libraryName);
        String libraryDirectory = library.getPath();
        NodeType testType = library.getNodeType("test");
        assertTrue(testType.hasParameterType("x"));
        assertTrue(testType.hasParameterType("y"));
        assertFalse(testType.hasParameterType("size"));
        Node testNode = net.create(testType);
        testNode.set("x", 50);
        testNode.set("y", 30);
        assertTrue(testNode.hasParameter("x"));
        assertTrue(testNode.hasParameter("y"));
        assertFalse(testNode.hasParameter("size"));

        // Change the type information and module contents.
        File ntlFile = new File(libraryDirectory + PlatformUtils.SEP + "types.ntl");
        String ntlContents = "<library name=\"" + libraryName + "\" formatVersion=\"0.8\" type=\"python\" module=\"" + libraryName + "\">\n" +
                "    <type name=\"test\" outputType=\"grob_path\" method=\"test\">\n" +
                "        <description>Test method.</description>\n" +
                "        <parameter type=\"float\" name=\"x\"/>\n" +
                "        <parameter type=\"float\" name=\"y\"/>\n" +
                "        <parameter type=\"float\" name=\"size\" defaultValue=\"100\"/>\n" +
                "    </type>\n" +
                "</library>";
        File moduleFile = new File(libraryDirectory + PlatformUtils.SEP + libraryName + ".py");
        String moduleContents = "from net.nodebox import graphics\n\n" +
                "def test(x, y, size):\n" +
                "    p = graphics.BezierPath()\n" +
                "    p.ellipse(x, y, size, size)\n" +
                "    return p\n";
        FileUtils.writeFile(ntlFile, ntlContents);
        FileUtils.writeFile(moduleFile, moduleContents);

        // Assert no node changed event have been fired yet.
        assertEquals(0, networkEventHandler.nodeChangedCounter);

        // Now reload the library
        library.reload();

        NodeType newType = testNode.getNodeType();
        assertTrue(newType.hasParameterType("x"));
        assertTrue(newType.hasParameterType("y"));
        assertTrue(newType.hasParameterType("size"));

        assertTrue(testNode.hasParameter("x"));
        assertTrue(testNode.hasParameter("y"));
        assertTrue(testNode.hasParameter("size"));

        assertEquals(50.0, testNode.asFloat("x"));
        assertEquals(30.0, testNode.asFloat("y"));
        assertEquals(100.0, testNode.asFloat("size"));

        // Check if the node changed event has fired.
        assertEquals(1, networkEventHandler.nodeChangedCounter);
        networkEventHandler.nodeChangedCounter = 0; // reset the counter by hand.

        // Create a second node and reload the library. The event should now be fired twice, once for each node.
        Node testNode2 = net.create(newType);
        library.reload();
        assertEquals(2, networkEventHandler.nodeChangedCounter);

    }

    private class NetworkEventHandler extends NetworkEventAdapter {
        public int nodeChangedCounter = 0;

        @Override
        public void nodeChanged(Network source, Node node) {
            nodeChangedCounter++;
        }
    }
}
