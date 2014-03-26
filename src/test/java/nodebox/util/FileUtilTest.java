package nodebox.util;

import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;
import static nodebox.client.FileUtils.getBaseName;

public class FileUtilTest {

    @Test
    public void testStripExtension() {
        assertEquals("test", FileUtils.stripExtension("test.ndbx"));
        assertEquals("MixedCase", FileUtils.stripExtension("MixedCase.GIF")); // Retain case
        assertEquals("a.lot.of.dots", FileUtils.stripExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("noextension", FileUtils.stripExtension("noextension"));
        assertEquals("/a/b/c.d/some", FileUtils.stripExtension("/a/b/c.d/some.file")); // Dots in path name
        assertEquals("", FileUtils.stripExtension("")); // Retain case
    }

    @Test
    public void testGetExtension() {
        assertEquals("png", FileUtils.getExtension("helloworld.png"));
        assertEquals("gif", FileUtils.getExtension("MixedCase.GIF")); // Always lower case
        assertEquals("dot", FileUtils.getExtension("a.lot.of.dots.dot")); // Extension = last dot
        assertEquals("", FileUtils.getExtension("noextension"));
    }

    @Test
    public void testGetBaseName() {
        assertEquals("helloworld", getBaseName("helloworld.png"));
        assertEquals("a.lot.of.dots", getBaseName("a.lot.of.dots.dot"));
        assertEquals("noextension", getBaseName("noextension"));
    }

    @Test
    public void testGetRelativePaths() {
        String sep = "/";
        assertEquals("stuff" + sep + "xyz.dat", FileUtils.getRelativePath(
                new File("/var/data/stuff/xyz.dat"), new File("/var/data/")));
        assertEquals(".." + sep + ".." + sep + "b" + sep + "c", FileUtils.getRelativePath(
                new File("/a/b/c"), new File("/a/x/y/")));
        assertEquals(".." + sep + ".." + sep + "b" + sep + "c", FileUtils.getRelativePath(
                new File("/m/n/o/a/b/c"), new File("/m/n/o/a/x/y/")));
    }

}