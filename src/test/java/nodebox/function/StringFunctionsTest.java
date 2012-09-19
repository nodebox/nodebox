package nodebox.function;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
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
    public void testConcatenate(){
        assertEquals("a", StringFunctions.concatenate("a", null, null, null));
        assertEquals("ab", StringFunctions.concatenate("a", "b", null, null));
        assertEquals("ad", StringFunctions.concatenate("a", null, null, "d"));
        assertEquals("cd", StringFunctions.concatenate(null, null, "c", "d"));
        assertEquals("", StringFunctions.concatenate(null, null, null, null));
    }



}
