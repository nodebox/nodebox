package nodebox.function;

import nodebox.node.Node;
import nodebox.node.NodeContext;
import nodebox.node.NodeLibrary;
import nodebox.node.Port;
import nodebox.util.LoadException;
import org.junit.Test;

import static junit.framework.Assert.*;
import static nodebox.util.Assertions.assertResultsEqual;

public class PythonLibraryTest {

    private final FunctionLibrary pyLibrary = PythonLibrary.loadScript("py-functions", "src/test/python/functions.py");
    private final FunctionRepository functions = FunctionRepository.of(pyLibrary);
    private final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);
    private final NodeContext context = new NodeContext(testLibrary);

    @Test
    public void testNamespaceForFile() {
        assertEquals("math", PythonLibrary.namespaceForFile("math.py"));
        assertNamespaceForFileFails("blob", "needs to end in .py");
        assertNamespaceForFileFails(".py", "can not be empty");
        assertNamespaceForFileFails("MyFileName.py", "can only contain");
        assertNamespaceForFileFails("my-file-name.py", "can only contain");
        assertNamespaceForFileFails("my file name.py", "can only contain");
    }

    private void assertNamespaceForFileFails(String fileName, String message) {
        try {
            PythonLibrary.namespaceForFile(fileName);
            fail("The namespaceForFile function should have failed with " + message);
        } catch (IllegalArgumentException ex) {
            assertTrue("Exception " + ex + " does not contain message " + message, ex.getMessage().contains(message));
        }
    }


    @Test
    public void testAdd() {
        Node addNode = Node.ROOT
                .withName("add")
                .withFunction("py-functions/add");
        Iterable<?> results = context.renderNode(addNode);
        assertResultsEqual(results, 0L);
    }

    @Test
    public void testAddWithArguments() {
        Node addNode = Node.ROOT
                .withName("add")
                .withFunction("py-functions/add")
                .withInputAdded(Port.intPort("v1", 1))
                .withInputAdded(Port.intPort("v2", 2))
                .withInputAdded(Port.intPort("v3", 3));
        Iterable<?> results = context.renderNode(addNode);
        assertResultsEqual(results, 6L);
    }

    @Test
    public void testMultiplyFloat() {
        Node multiplyNode = Node.ROOT
                .withName("multiply")
                .withFunction("py-functions/multiply")
                .withInputAdded(Port.floatPort("v1", 10))
                .withInputAdded(Port.floatPort("v2", 2));
        Iterable<?> results = context.renderNode(multiplyNode);
        assertResultsEqual(results, 20.0);
    }

    @Test
    public void testMultiplyString() {
        Node multiplyNode = Node.ROOT
                .withName("multiply")
                .withFunction("py-functions/multiply")
                .withInputAdded(Port.stringPort("v1", "spam"))
                .withInputAdded(Port.intPort("v2", 3));
        Iterable<?> results = context.renderNode(multiplyNode);
        assertResultsEqual(results, "spamspamspam");
    }

    @Test(expected = LoadException.class)
    public void testLoadError() {
        PythonLibrary.loadScript("py-error", "src/test/python/nonexisting.py");
    }

}
