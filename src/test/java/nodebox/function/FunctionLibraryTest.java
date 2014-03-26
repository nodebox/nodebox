package nodebox.function;

import nodebox.util.LoadException;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;

public class FunctionLibraryTest {

    private final File userDir = new File(System.getProperty("user.dir"));

    @Test
    public void testLoadClojure() {
        String href = "clojure:src/test/clojure/math.clj";
        FunctionLibrary clojureLibrary = FunctionLibrary.load(href);
        assertEquals("clojure-math", clojureLibrary.getNamespace());
        assertTrue(clojureLibrary.hasFunction("add"));
        assertEquals(href, clojureLibrary.getLink(new File(userDir, "test.ndbx")));
    }

    @Test
    public void testLoadPython() {
        String href = "python:src/test/python/functions.py";
        FunctionLibrary pythonLibrary = FunctionLibrary.load(href);
        assertEquals("functions", pythonLibrary.getNamespace());
        assertTrue(pythonLibrary.hasFunction("add"));
        assertEquals(href, pythonLibrary.getLink(new File(userDir, "test.ndbx")));
    }

    @Test
    public void testLoadJava() {
        String href = "java:nodebox.function.MathFunctions";
        FunctionLibrary javaLibrary = FunctionLibrary.load(href);
        assertEquals("math", javaLibrary.getNamespace());
        assertTrue(javaLibrary.hasFunction("add"));
        assertEquals(href, javaLibrary.getLink());
    }

    @Test(expected = LoadException.class)
    public void testMethodsWithSameName() {
        String href = "java:nodebox.function.NonUniqueFunctions";
        FunctionLibrary javaLibrary = FunctionLibrary.load(href);
    }
}
