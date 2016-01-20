package nodebox.function;

import nodebox.node.Node;
import nodebox.node.NodeContext;
import nodebox.node.NodeLibrary;
import nodebox.node.Port;
import nodebox.util.LoadException;
import org.junit.Test;

import java.util.List;

import static nodebox.util.Assertions.assertResultsEqual;

public class ClojureLibraryTest {

    private final FunctionLibrary mathLibrary = ClojureLibrary.loadScript("src/test/clojure/math.clj");
    private final FunctionRepository functions = FunctionRepository.of(mathLibrary);
    private final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);

    private List<?> renderNode(Node node) {
        return new NodeContext(testLibrary.withRoot(node)).renderNode("/");
    }

    @Test
    public void testAdd() {
        Node addNode = Node.ROOT
                .withName("add")
                .withOutputType("int")
                .withFunction("clojure-math/add");
        Iterable<?> results = renderNode(addNode);
        assertResultsEqual(results, 0L);
    }

    @Test
    public void testAddWithArguments() {
        Node addNode = Node.ROOT
                .withName("add")
                .withFunction("clojure-math/add")
                .withOutputType("int")
                .withInputAdded(Port.intPort("v1", 1))
                .withInputAdded(Port.intPort("v2", 2))
                .withInputAdded(Port.intPort("v3", 3));
        Iterable<?> results = renderNode(addNode);
        assertResultsEqual(results, 6L);
    }

    @Test(expected = LoadException.class)
    public void testNoVarAtEnd() {
        ClojureLibrary.loadScript("src/test/clojure/no-var-at-end.clj");
    }
}