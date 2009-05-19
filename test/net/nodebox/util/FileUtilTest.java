package net.nodebox.util;

import junit.framework.TestCase;

public class FileUtilTest extends TestCase {

    public void testStripExtension() {
        assertEquals("test", FileUtils.stripExtension("test.ndbx"));
        assertEquals("MixedCase", FileUtils.stripExtension("MixedCase.GIF")); // Retain case
        assertEquals("a.lot.of.dots", FileUtils.stripExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("noextension", FileUtils.stripExtension("noextension"));
        assertEquals("", FileUtils.stripExtension("")); // Retain case
    }

    public void testGetExtension() {
        assertEquals("png", FileUtils.getExtension("helloworld.png"));
        assertEquals("gif", FileUtils.getExtension("MixedCase.GIF")); // Always lower case
        assertEquals("dot", FileUtils.getExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("", FileUtils.getExtension("noextension"));
    }
}
