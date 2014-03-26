package nodebox.function;

import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;

public class FunctionRepositoryTest {

    private static final NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
    private static final NodeLibrary stringLibrary = NodeLibrary.load(new File("libraries/string/string.ndbx"), NodeRepository.of());

    @Test
    public void testGetFunctionRepository() {
        NodeRepository nodeRepository = NodeRepository.of(mathLibrary, stringLibrary);
        FunctionRepository functionRepository = nodeRepository.getFunctionRepository();
        assertTrue(functionRepository.hasLibrary("math"));
        assertTrue(functionRepository.hasLibrary("string"));
    }

}
