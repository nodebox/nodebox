package nodebox.util.waves;

public abstract class AbstractWave {

    public static final float PI = 3.14159265358979323846f;
    public static final float TWO_PI = 2 * PI;

    public static enum Type {SINE, TRIANGLE, SQUARE, SAWTOOTH}

    ;

    private float period;
    private float phase;
    private float frequency;
    protected float amplitude;
    private float offset;

    public AbstractWave(float period, float amplitude) {
        this(period, 0f, amplitude, 0);
    }

    public AbstractWave(float period, float phase, float amplitude) {
        this(period, phase, amplitude, 0);
    }

    public AbstractWave(float period, float phase, float amplitude, float offset) {
        this.period = period;
        this.frequency = TWO_PI / period;
        this.amplitude = amplitude;
        this.offset = offset;
        setPhase(phase);
    }

    public float getPeriod() {
        return period;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public float getOffset() {
        return offset;
    }

    public float getPhase() {
        return phase;
    }

    /**
     * (Re)sets the starting position of the wave.
     *
     * @param phase the new starting phase
     */
    public void setPhase(float phase) {
        phase %= TWO_PI;
        if (phase < 0) phase += TWO_PI;
        this.phase = phase;
    }

    /**
     * Calculates and returns the value at time unit t for the wave.
     *
     * @param t time coordinate on the wave continuum
     * @return the new value
     */
    public float getValueAt(float t) {
        float time = adjustedTime(t);
        float phase;
        if (time % period == 0)
            phase = this.phase;
        else
            phase = (this.phase + time * frequency) % TWO_PI;
        if (phase < 0) phase += TWO_PI;
        return computeValue(phase) + offset;
    }


    protected abstract float adjustedTime(float t);

    /**
     * Calculates and returns the value at phase for the wave.
     *
     * @param phase
     * @return the new value
     */
    protected abstract float computeValue(float phase);
}
