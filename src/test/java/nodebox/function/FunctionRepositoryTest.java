package nodebox.function;

import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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

    @Test
    public void testMultipleLibrariesForNamespace() {
        FunctionRepository repository = mathLibrary.getFunctionRepository();
        assertTrue(repository.hasLibrary("math"));
        // getLibrary method will return the first library, for compatibility.
        assertEquals("java", repository.getLibrary("math").getLanguage());
        // getLibraries#String will return all implementing libraries.
        assertFunctionLinks(mathLibrary, repository.getLibraries("math"),
                "java:nodebox.function.MathFunctions",
                "javascript:math.js");
        assertFunctionLinks(mathLibrary, repository.getLibraries(),
                "java:nodebox.function.CoreFunctions",
                "java:nodebox.function.MathFunctions",
                "javascript:math.js");
    }

    @Test
    public void testCombineMultiples() {
        FunctionRepository combined = FunctionRepository.combine(mathLibrary.getFunctionRepository(), stringLibrary.getFunctionRepository());
        assertFunctionLinks(mathLibrary, combined.getLibraries("math"),
                "java:nodebox.function.MathFunctions",
                "javascript:math.js");
        assertFunctionLinks(mathLibrary, combined.getLibraries(),
                "java:nodebox.function.CoreFunctions",
                "java:nodebox.function.MathFunctions",
                "javascript:math.js",
                "java:nodebox.function.StringFunctions",
                "javascript:../string/string.js");
    }

    private void assertFunctionLinks(NodeLibrary library, Collection<FunctionLibrary> libraries, String... links) {
        assertEquals(links.length, libraries.size());
        int i = 0;
        for (FunctionLibrary fl : libraries) {
            assertEquals(fl.getLink(library.getFile()), links[i]);
            i++;
        }
    }

}
