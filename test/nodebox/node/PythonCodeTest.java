package nodebox.node;

import junit.framework.TestCase;
import nodebox.node.polygraph.Polygon;
import nodebox.node.polygraph.Rectangle;

public class PythonCodeTest extends TestCase {

    private NodeLibrary testLibrary;

    @Override
    protected void setUp() throws Exception {
        testLibrary = new NodeLibrary("test");
    }

    /**
     * Test if basic expressions return the correct type.
     */
    public void testBasic() {
        assertSnippetEquals("hello", "\"hello\"", null);
        assertSnippetEquals(5, "2+3", null);
        assertSnippetEquals(2.5, "5.0/2.0", null); // Python returns a Double.
    }

    /**
     * You can access parameter values on the node by using self.parametername.
     */
    public void testSelf() {
        Node node1 = Node.ROOT_NODE.newInstance(testLibrary, "node1", Polygon.class);
        node1.addParameter("alpha", Parameter.Type.INT, 42);
        assertSnippetEquals(42, "self.alpha", node1);
        node1.addPort("polygon");
        node1.setPortValue("polygon", Polygon.rect(20, 30, 40, 50));
        assertSnippetEquals(new Rectangle(20, 30, 40, 50), "self.polygon.bounds", node1);
    }

    /**
     * You can access attributes on the node object itself by using self.node.
     */
    public void testNodeAccess() {
        Node node1 = Node.ROOT_NODE.newInstance(testLibrary, "node1");
        node1.setPosition(35, 22);
        node1.addParameter("x", Parameter.Type.INT, 42);
        assertSnippetEquals(42, "self.x", node1);
        assertSnippetEquals(35.0, "self.node.x", node1);
    }

    /**
     * Test if the code can access context globals.
     */
    public void testGlobals() {
        assertSnippetEquals(11, "FRAME + 10", null, new ProcessingContext());
    }

    /**
     * Test for errors when cooking code.
     */
    public void testErrors() {
        Node node1 = Node.ROOT_NODE.newInstance(testLibrary, "node1");
        // Test for intialization errors.
        assertCodeFails("# No source code", null, "does not contain a function");
        assertSnippetFails("/////// hello?", null, "SyntaxError");
        // Change the cook function to a number.
        assertCodeFails("cook = 5", null, "not a function.");
        // Cook with too many parameters.
        // The error here is a bit backwards but correct.
        assertCodeFails("def cook(moe, curly, larry): pass", null, "takes exactly 3 arguments (1 given)");
        // Test for errors in the cook function.
        assertSnippetFails("1 / 0", null, "ZeroDivisionError");
        assertSnippetFails("self.x", null, "'NoneType' object has no attribute 'x'");
        assertSnippetFails("self.alpha", node1, "Node 'test.node1' has no parameter or port 'alpha'");
        node1.addParameter("alpha", Parameter.Type.INT, 42);
        assertSnippetEquals(42, "self.alpha", node1);
    }

    //// Custom assertions ////

    /**
     * Assert if the given code snippet returns the expected value.
     * <p/>
     * This wraps the snippet in a full "cook" method and checks the result.
     *
     * @param expected the expected value
     * @param snippet  the snippet of code, e.g. "12 + 3"
     * @param node     the node to operate on
     */
    private void assertSnippetEquals(Object expected, String snippet, Node node) {
        assertSnippetEquals(expected, snippet, node, new ProcessingContext());
    }

    /**
     * Assert if the given code snippet returns the expected value.
     * <p/>
     * This wraps the snippet in a full "cook" method and checks the result.
     *
     * @param expected the expected value
     * @param snippet  the snippet of code, e.g. "12 + 3"
     * @param node     the node to operate on
     * @param context  the context to operate in
     */
    private void assertSnippetEquals(Object expected, String snippet, Node node, ProcessingContext context) {
        String source = snippetToCode(snippet);
        PythonCode code = new PythonCode(source);
        Object obj = code.cook(node, context);
        assertEquals(expected, obj);
    }

    private void assertSnippetFails(String snippet, Node node, String expectedMessage) {
        assertSnippetFails(snippet, node, new ProcessingContext(), expectedMessage);
    }

    private void assertSnippetFails(String snippet, Node node, ProcessingContext context, String expectedMessage) {
        assertCodeFails(snippetToCode(snippet), node, context, expectedMessage);
    }

    private void assertCodeFails(String source, Node node, String expectedMessage) {
        assertCodeFails(source, node, new ProcessingContext(), expectedMessage);
    }

    private void assertCodeFails(String source, Node node, ProcessingContext context, String expectedMessage) {
        PythonCode code = new PythonCode(source);
        try {
            code.cook(node, context);
            fail("Should have failed with " + expectedMessage);
        } catch (Exception e) {
            assertTrue("Was expecting error " + expectedMessage + ", got " + e.toString(),
                    e.toString().toLowerCase().contains(expectedMessage.toLowerCase()));
        }
    }

    private String snippetToCode(String snippet) {
        return "def cook(self): return " + snippet;
    }

}
