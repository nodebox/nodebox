package nodebox.function;

import org.junit.Test;

import static junit.framework.TestCase.*;
import static nodebox.function.StringFunctions.makeStrings;
import static nodebox.util.Assertions.assertResultsEqual;

public class StringFunctionsTest {

    @Test
    public void testMakeStrings() {
        assertResultsEqual(makeStrings("a;b", ";"), "a", "b");
        assertResultsEqual(makeStrings("a;b", ""), "a", ";", "b");
        assertResultsEqual(makeStrings("hello", ""), "h", "e", "l", "l", "o");
        assertResultsEqual(makeStrings("a b c", " "), "a", "b", "c");
        assertResultsEqual(makeStrings("a; b; c", ";"), "a", " b", " c");
        assertResultsEqual(makeStrings(null, ";"));
        assertResultsEqual(makeStrings(null, null));
    }

    @Test
    public void testLength() {
        assertEquals(0, StringFunctions.length(null));
        assertEquals(0, StringFunctions.length(""));
        assertEquals(5, StringFunctions.length("bingo"));
    }

    @Test
    public void testWordCount() {
        assertEquals(0, StringFunctions.wordCount(null));
        assertEquals(0, StringFunctions.wordCount(""));

        assertEquals(1, StringFunctions.wordCount("a"));
        assertEquals(1, StringFunctions.wordCount("a_b"));

        assertEquals(2, StringFunctions.wordCount("a b"));
        assertEquals(2, StringFunctions.wordCount("a-b"));
        assertEquals(2, StringFunctions.wordCount("a,b"));
        assertEquals(2, StringFunctions.wordCount("a.b"));
    }

    @Test
    public void testConcatenate() {
        assertEquals("a", StringFunctions.concatenate("a", null, null, null, null, null, null));
        assertEquals("ab", StringFunctions.concatenate("a", "b", null, null, null, null, null));
        assertEquals("ad", StringFunctions.concatenate("a", null, null, "d", null, null, null));
        assertEquals("cd", StringFunctions.concatenate(null, null, "c", "d", null, null, null));
        assertEquals("", StringFunctions.concatenate(null, null, null, null, null, null, null));
    }

    @Test
    public void testFormatNumber() {
        assertEquals("16.13", StringFunctions.formatNumber(16.127, "%.2f"));
        assertEquals("12", StringFunctions.formatNumber(12, "%.0f"));
        assertEquals("012", StringFunctions.formatNumber(12, "%03.0f"));
        assertEquals("012", StringFunctions.formatNumber(12.25, "%03.0f"));
        assertEquals("012", StringFunctions.formatNumber(11.55, "%03.0f"));
        assertEquals("012.00", StringFunctions.formatNumber(12.0, "%06.2f"));
    }

}
