package nodebox.client;

import junit.framework.TestCase;

import java.io.File;

public class ImageFormatTest extends TestCase {

    public void testEnsureFileFormat() {
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test"));
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test.pdf"));
        assertEquals("test..pdf", ImageFormat.PDF.ensureFileExtension("test."));
        assertEquals("test.png.pdf", ImageFormat.PDF.ensureFileExtension("test.png"));
        assertEquals("test.something.pdf", ImageFormat.PDF.ensureFileExtension("test.something"));
        assertEquals("a.b.c.pdf", ImageFormat.PDF.ensureFileExtension("a.b.c"));
        assertEquals("/a/b/c.pdf", ImageFormat.PDF.ensureFileExtension("/a/b/c"));
        assertEquals("/a/b/c.png.pdf", ImageFormat.PDF.ensureFileExtension("/a/b/c.png"));
        assertEquals(new File("/a/b.test/c.pdf").getAbsolutePath(), ImageFormat.PDF.ensureFileExtension(new File("/a/b.test/c")).getAbsolutePath());
    }

}
