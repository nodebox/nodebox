package nodebox.util;

import nodebox.util.waves.*;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class WavesTest {

    @Test
    public void testConvertFromMinMax() {
        AbstractWave w = SineWave.from(-20, 20, 100);
        assertEquals(20f, w.getAmplitude());
        assertEquals(0f, w.getOffset());
        w = SineWave.from(-25, 35, 100);
        assertEquals(30f, w.getAmplitude());
        assertEquals(5f, w.getOffset());
        w = SineWave.from(-35, 25, 100);
        assertEquals(30f, w.getAmplitude());
        assertEquals(-5f, w.getOffset());
        assertEquals(100f, w.getPeriod());
        w = SawtoothWave.from(-20, 20, 100);
        assertEquals(20f, w.getAmplitude());
        assertEquals(0f, w.getOffset());
        w = SquareWave.from(-25, 35, 100);
        assertEquals(30f, w.getAmplitude());
        assertEquals(5f, w.getOffset());
        w = TriangleWave.from(-35, 25, 100);
        assertEquals(30f, w.getAmplitude());
        assertEquals(-5f, w.getOffset());
    }

    @Test
    public void testSineWave() {
        SineWave w = new SineWave(120, 20);
        assertEquals(0f, w.getValueAt(0), 0.0001);
        assertEquals(-20f, w.getValueAt(30), 0.0001);
        assertEquals(0f, w.getValueAt(60), 0.0001);
        assertEquals(20f, w.getValueAt(90), 0.0001);
        assertEquals(0f, w.getValueAt(120), 0.0001);
    }

}
