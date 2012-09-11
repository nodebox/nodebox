package nodebox.util.waves;

public class TriangleWave extends AbstractWave {
    public TriangleWave(float period, float amplitude) {
        super(period, 0f, amplitude);
    }

    public TriangleWave(float period, float phase, float amplitude) {
        super(period, phase, amplitude);
    }

    public TriangleWave(float period, float phase, float amplitude, float offset) {
        super(period, phase, amplitude, offset);
    }

    /**
     * Creates a suitable TriangleWave object from other than the constructor arguments.
     * The wave oscillates between min and max values
     *
     * @param min    the minimum value
     * @param max    the maximum value
     * @param period the length (expressed in time) over which the wave makes a full triangular movement
     * @return a new TriangleWave
     */
    public static TriangleWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new TriangleWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return Math.abs((phase / TWO_PI) * 2 - 1) * amplitude * 2 - amplitude;
    }

    protected float adjustedTime(float t) {
        return t + getPeriod() / 4;
    }
}
