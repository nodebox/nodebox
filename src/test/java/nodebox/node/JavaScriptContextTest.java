package nodebox.node;

import com.google.common.collect.ImmutableList;
import nodebox.function.FunctionRepository;
import org.junit.Ignore;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class JavaScriptContextTest {

    @Ignore
    public void testSimple() {
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        Node negate1 = mathLibrary.getRoot().getChild("negate").withInputValue("value", 42.0);
        Node net = Node.NETWORK.withChildAdded(negate1).withRenderedChild(negate1);
        NodeLibrary myLibrary = NodeLibrary.create("test", net, NodeRepository.of(mathLibrary), FunctionRepository.of());
        JavaScriptContext context = new JavaScriptContext(myLibrary);
        assertEquals(ImmutableList.of(42.0), context.renderNetwork(net));
    }

}
