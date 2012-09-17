package nodebox.function;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;

/**
 * Library with functions for String manipulation.
 */
public class StringFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("string", StringFunctions.class, "string", "makeStrings", "length", "wordCount", "concatenate");
    }

    /**
     * Return the given string, as-is.
     * <p/>
     * This function is used for using strings as variables in NodeBox.
     */
    public static String string(String value) {
        return value;
    }

    /**
     * Make a list of strings from a big string with separators.
     * Whitespace is not stripped.
     *
     * @param s         The input string, e.g. "a;b;c"
     * @param separator The separator, e.g. ";". If the separator is empty, return each character separately.
     * @return A list of strings.
     */
    public static List<String> makeStrings(String s, String separator) {
        if (s == null) {
            return ImmutableList.of();
        }
        if (separator == null || separator.isEmpty()) {
            return ImmutableList.copyOf(Splitter.fixedLength(1).split(s));
        }
        return ImmutableList.copyOf(Splitter.on(separator).split(s));
    }

    public static int length(String s) {
        if (s == null) return 0;
        return s.length();
    }

    public static int wordCount(String s) {
        if (s == null) return 0;
        Iterable<String> split = Splitter.onPattern("\\w+").split(s);
        return Iterables.size(split) - 1;
    }

    public static String concatenate(String s1, String s2, String s3, String s4) {
        s1 = s1 != null ? s1 : "";
        s2 = s2 != null ? s2 : "";
        s3 = s3 != null ? s3 : "";
        s4 = s4 != null ? s4 : "";
        return s1 + s2 + s3 + s4;
    }

}
