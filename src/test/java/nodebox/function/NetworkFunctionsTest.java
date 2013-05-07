package nodebox.function;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkFunctionsTest {

    @Test
    public void testEncodeURL() {
        assertEquals("foo", NetworkFunctions.encodeURL("foo"));
        assertEquals("foo+bar", NetworkFunctions.encodeURL("foo bar"));
        assertEquals("foo+%26+bar", NetworkFunctions.encodeURL("foo & bar"));
    }

}
