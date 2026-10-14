package nodebox.movie;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovieTest {

    @Test
    public void testFormatFrameRate() {
        assertEquals("25", Movie.formatFrameRate(25));
        assertEquals("60", Movie.formatFrameRate(60.0));
        assertEquals("29.97", Movie.formatFrameRate(29.97));
    }

    @Test
    public void testDefaultFrameRate() {
        Movie movie = new Movie("test.mp4", MP4VideoFormat.HighFormat, 10, 10);
        assertEquals(25, movie.getFrameRate(), 0);
        movie.setFrameRate(60);
        assertEquals(60, movie.getFrameRate(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsZeroFrameRate() {
        new Movie("test.mp4", MP4VideoFormat.HighFormat, 10, 10).setFrameRate(0);
    }
}
