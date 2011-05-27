package nodebox.client;

import junit.framework.TestCase;

public class ImageFormatTest extends TestCase {

    public void testEnsureFileFormat() {
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test"));
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test."));
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test.png"));
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test.png"));
        assertEquals("test.pdf", ImageFormat.PDF.ensureFileExtension("test.something"));
        assertEquals("test.something.pdf", ImageFormat.PDF.ensureFileExtension("test.something.png"));
        assertEquals("/a/b/c.pdf", ImageFormat.PDF.ensureFileExtension("/a/b/c"));
        assertEquals("/a/b/c.pdf", ImageFormat.PDF.ensureFileExtension("/a/b/c.png"));
    }

}
