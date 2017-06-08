package nodebox.ui;

import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.*;

public class ExportFormatTest {

    @Test
    public void testEnsureFileFormat() {
        assertEquals("test.pdf", ExportFormat.PDF.ensureFileExtension("test"));
        assertEquals("test.pdf", ExportFormat.PDF.ensureFileExtension("test.pdf"));
        assertEquals("test..pdf", ExportFormat.PDF.ensureFileExtension("test."));
        assertEquals("test.png.pdf", ExportFormat.PDF.ensureFileExtension("test.png"));
        assertEquals("test.something.pdf", ExportFormat.PDF.ensureFileExtension("test.something"));
        assertEquals("a.b.c.pdf", ExportFormat.PDF.ensureFileExtension("a.b.c"));
        assertEquals("/a/b/c.pdf", ExportFormat.PDF.ensureFileExtension("/a/b/c"));
        assertEquals("/a/b/c.png.pdf", ExportFormat.PDF.ensureFileExtension("/a/b/c.png"));
        assertEquals(new File("/a/b.test/c.pdf").getAbsolutePath(), ExportFormat.PDF.ensureFileExtension(new File("/a/b.test/c")).getAbsolutePath());
    }

}
