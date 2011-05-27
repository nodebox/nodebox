package nodebox.util;

import junit.framework.TestCase;

import java.io.File;

public class FileUtilTest extends TestCase {

    public void testStripExtension() {
        assertEquals("test", FileUtils.stripExtension("test.ndbx"));
        assertEquals("MixedCase", FileUtils.stripExtension("MixedCase.GIF")); // Retain case
        assertEquals("a.lot.of.dots", FileUtils.stripExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("noextension", FileUtils.stripExtension("noextension"));
        assertEquals("/a/b/c.d/some", FileUtils.stripExtension("/a/b/c.d/some.file")); // Dots in path name
        assertEquals("", FileUtils.stripExtension("")); // Retain case
    }

    public void testGetExtension() {
        assertEquals("png", FileUtils.getExtension("helloworld.png"));
        assertEquals("gif", FileUtils.getExtension("MixedCase.GIF")); // Always lower case
        assertEquals("dot", FileUtils.getExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("", FileUtils.getExtension("noextension"));
    }

    public void testGetRelativePaths() {
        assertEquals("stuff/xyz.dat", FileUtils.getRelativePath(
                new File("/var/data/stuff/xyz.dat"), new File("/var/data/")));
        assertEquals("../../b/c", FileUtils.getRelativePath(
                new File("/a/b/c"), new File("/a/x/y/")));
        assertEquals("../../b/c", FileUtils.getRelativePath(
                new File("/m/n/o/a/b/c"), new File("/m/n/o/a/x/y/")));
    }
}
