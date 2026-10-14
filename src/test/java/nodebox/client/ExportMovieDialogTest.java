package nodebox.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExportMovieDialogTest {

    @Test
    public void testParseFrameRate() {
        assertEquals(30, ExportMovieDialog.parseFrameRate("30"), 0);
        assertEquals(29.97, ExportMovieDialog.parseFrameRate(" 29.97 "), 0);
        assertEquals(29.97, ExportMovieDialog.parseFrameRate("29,97"), 0);
        // Anything unusable falls back to the default of 25.
        assertEquals(25, ExportMovieDialog.parseFrameRate(""), 0);
        assertEquals(25, ExportMovieDialog.parseFrameRate("fast"), 0);
        assertEquals(25, ExportMovieDialog.parseFrameRate("-5"), 0);
    }
}
