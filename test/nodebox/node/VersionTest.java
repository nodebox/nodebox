package nodebox.node;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testParsing() {
        assertEquals(new Version(0, 0, 0), new Version("0"));
        assertEquals(new Version(1, 0, 0), new Version("1"));
        assertEquals(new Version(1, 4, 0), new Version("1.4"));
        assertEquals(new Version(0, 4, 5), new Version("0.4.5"));
        assertEquals(new Version(124125, 421321, 5123213), new Version("124125.421321.5123213"));

        assertInvalidVersion("", "no empty strings");
        assertInvalidVersion("hello", "only numbers");
        assertInvalidVersion("1.2.3.4", "too many components");
        assertInvalidVersion("-12.2.3", "no negative numbers");
        assertInvalidVersion("1.2.a", "no letters");
    }

    public void testLargerThan() {
        assertTrue(new Version(1, 0, 0).largerThan(new Version(0, 0, 0)));
        assertTrue(new Version(0, 0, 1).largerThan(new Version(0, 0, 0)));
        assertTrue(new Version(0, 3, 1).largerThan(new Version(0, 2, 99)));
        assertTrue(new Version(4, 0, 0).largerThan(new Version(3, 9, 9)));
    }

    private void assertInvalidVersion(String verisonString, String reason) {
        try {
            Version.parseVersionString(verisonString);
            fail("Version " + verisonString + " should not have been accepted: " + reason);
        } catch (IllegalArgumentException e) {
            // Should fail!
        }
    }
}
