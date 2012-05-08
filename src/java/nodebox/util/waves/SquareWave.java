package nodebox.util.waves;

public class SquareWave extends AbstractWave {
    public SquareWave(float period, float amplitude) {
        super(period, 0f, amplitude);
    }

    public SquareWave(float period, float phase, float amplitude) {
        super(period, phase, amplitude);
    }

    public SquareWave(float period, float phase, float amplitude, float offset) {
        super(period, phase, amplitude, offset);
    }

    /**
     * Creates a suitable SquareWave object from other than the constructor arguments.
     * The wave oscillates between min and max values
     *
     * @param min    the minimum value
     * @param max    the maximum value
     * @param period the length (expressed in time) over which the wave makes a full block movement
     * @return a new SquareWave
     */
    public static SquareWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new SquareWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return (phase / TWO_PI < 0.5 ? 1 : -1) * amplitude;
    }

    protected float adjustedTime(float t) {
        return t + getPeriod() / 2;
    }
}
