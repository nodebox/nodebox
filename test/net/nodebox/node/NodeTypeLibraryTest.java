package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.client.PlatformUtils;
import org.python.core.PySystemState;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class NodeTypeLibraryTest extends TestCase {

    private void initJython() {
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});

    }

    public void testLoading() {
        // TODO: Python initialization should happen lazily somewhere while loading the library.
        initJython();
        // Find the "<projectDirectory>/libraries/testlib" library.
        String projectDirectory = System.getProperty("user.dir");
        File testlibPath = new File(projectDirectory + PlatformUtils.SEP + "libraries" + PlatformUtils.SEP + "testlib");
        // Create a library object. This is normally something handled by the NodeTypeLibraryManager.
        NodeTypeLibrary library;
        try {
            library = new NodeTypeLibrary("testlib", 1, 0, 0, testlibPath);
        } catch (IOException e) {
            fail("An exception occurred while loading library:" + e);
            return;
        }
        // Load the library. This can throw a multitude of exceptions.
        library.load();
        // Check if everything's there.
        // Number node type.
        NodeType numberType = library.getNodeType("number");
        ParameterType ptValue = numberType.getParameterType("value");
        assertEquals(ParameterType.Type.INT, ptValue.getType());
        // Check if the node executes.
        NodeType negateType = library.getNodeType("negate");
        Node negateNode = negateType.createNode();
        negateNode.set("value", 42);
        negateNode.update();
        assertEquals(-42, negateNode.getOutputValue());
        // Check node with multiple parameters.
        NodeType addType = library.getNodeType("add");
        Node addNode = addType.createNode();
        addNode.set("v1", 50);
        addNode.set("v2", 3);
        addNode.update();
        assertEquals(53, addNode.getOutputValue());
    }

}
