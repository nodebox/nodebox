package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Point;

import java.io.File;
import java.util.List;

/**
 * A function library containing special functions for testing.
 */
public class TestFunctions extends AbstractTestFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("test", TestFunctions.class, "allTypes", "baseReverse", "makeNull", "fileExists", "makeNestedWords");
    }

    public static String allTypes(int i, float f, String s, Point pt) {
        StringBuilder b = new StringBuilder()
                .append(i).append(", ")
                .append(f).append(", ")
                .append(s).append(", ")
                .append(pt);
        return b.toString();
    }

    /**
     * Whatever the input, returns null.
     * @param ignored The input, which is ignored
     * @return null.
     */
    public static Double makeNull(Double ignored) {
        return null;
    }

    public static boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    public static List<List<String>> makeNestedWords() {
        List<String> aWords = ImmutableList.of("apple", "abstraction", "albatross");
        List<String> bWords = ImmutableList.of("banana", "bird", "boat");
        List<String> cWords = ImmutableList.of("clock", "creature", "coffee");
        return ImmutableList.of(aWords, bWords, cWords);
    }

}
