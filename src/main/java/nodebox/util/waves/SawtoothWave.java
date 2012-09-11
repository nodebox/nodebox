package nodebox.util.waves;

public class SawtoothWave extends AbstractWave {
    public SawtoothWave(float period, float amplitude) {
        super(period, 0f, amplitude);
    }

    public SawtoothWave(float period, float phase, float amplitude) {
        super(period, phase, amplitude);
    }

    public SawtoothWave(float period, float phase, float amplitude, float offset) {
        super(period, phase, amplitude, offset);
    }

    /**
     * Creates a suitable SawtoothWave object from other than the constructor arguments.
     * The wave oscillates between min and max values
     *
     * @param min    the minimum value
     * @param max    the maximum value
     * @param period the length (expressed in time) over which the wave makes a full sawtooth movement
     * @return a new SawtoothWave
     */
    public static SawtoothWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new SawtoothWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return ((phase / TWO_PI) * 2 - 1) * -amplitude;
    }

    protected float adjustedTime(float t) {
        return t + getPeriod() / 2;
    }
}
