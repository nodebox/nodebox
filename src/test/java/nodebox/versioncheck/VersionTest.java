package nodebox.versioncheck;

import org.junit.Test;

import static junit.framework.TestCase.*;

public class VersionTest {

    private static final int LARGER_THAN = 1;
    private static final int EQUAL = 0;
    private static final int SMALLER_THAN = -1;

    @Test
    public void testBasic() {
        Version v1 = new Version("2.0");
        assertEquals(LARGER_THAN, v1.compareTo(new Version("1.0")));
        assertEquals(SMALLER_THAN, v1.compareTo(new Version("3.0")));
        assertEquals(LARGER_THAN, v1.compareTo(new Version("1.9.9")));
        assertEquals(SMALLER_THAN, v1.compareTo(new Version("2.1")));
        assertEquals(SMALLER_THAN, v1.compareTo(new Version("2.0.0.1")));
        Version v2 = new Version("0.1.2.3");
        assertEquals(LARGER_THAN, v2.compareTo(new Version("0")));
        assertEquals(LARGER_THAN, v2.compareTo(new Version("0.0")));
        assertEquals(LARGER_THAN, v2.compareTo(new Version("0.0.0.1")));
        assertEquals(LARGER_THAN, v2.compareTo(new Version("0.1.2.1")));
        assertEquals(SMALLER_THAN, v2.compareTo(new Version("0.1.2.4")));
        assertEquals(SMALLER_THAN, v2.compareTo(new Version("0.2.1.1")));
        assertEquals(SMALLER_THAN, v2.compareTo(new Version("0.1.2.3.4")));
        assertEquals(EQUAL, v2.compareTo(new Version("0.1.2.3")));
        Version v3 = new Version("1.2.3.snapshot");
        assertEquals(LARGER_THAN, v3.compareTo(new Version("0")));
        assertEquals(LARGER_THAN, v3.compareTo(new Version("1.2.3")));
        assertEquals(LARGER_THAN, v3.compareTo(new Version("1.2.3.99")));
        assertEquals(SMALLER_THAN, v3.compareTo(new Version("1.2.3.snapshot.9")));
        assertEquals(SMALLER_THAN, v3.compareTo(new Version("hello")));
    }

}
