package nodebox.util;

import com.google.common.collect.ImmutableList;
import nodebox.function.*;
import nodebox.node.Node;
import nodebox.node.NodeContext;
import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;

import static org.junit.Assert.assertEquals;

public final class Assertions {

    static private final FunctionRepository functionRepository =
            FunctionRepository.of(TestFunctions.LIBRARY, MathFunctions.LIBRARY, ColorFunctions.LIBRARY, ListFunctions.LIBRARY, StringFunctions.LIBRARY, SideEffects.LIBRARY, CoreVectorFunctions.LIBRARY);
    static private final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, NodeRepository.of(), functionRepository);

    public static void assertResultsEqual(Iterable<?> result, Object... args) {
        assertEquals(ImmutableList.copyOf(args), ImmutableList.copyOf(result));
    }

    public static void assertResultsEqual(Node network, Node child, Object... args) {
        NodeContext context = new NodeContext(testLibrary.withRoot(network));
        Iterable<?> values = context.renderChild("/", child);
        assertResultsEqual(values, args);
    }

    public static void assertNoResults(Node network, Node child) {
        assertResultsEqual(network, child);
    }

    public static void assertResultsEqual(Node node, Object... args) {
        NodeContext context = new NodeContext(testLibrary.withRoot(node));
        Iterable<?> values = context.renderNode("/");
        assertResultsEqual(values, args);
    }


}
