package nodebox.util.waves;

public abstract class AbstractWave {

    public static final float PI = 3.14159265358979323846f;
    public static final float TWO_PI = 2 * PI;

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
     * @param t  time coordinate on the wave continuum
     * @return   the new value
     */
    public float getValueAt(float t) {
        float phase;
        if (t % period == 0)
            phase = this.phase;
        else
            phase = (this.phase + t * frequency) % TWO_PI;
        if (phase < 0) phase += TWO_PI;
        return computeValue(phase) + offset;
    }

    /**
     * Calculates and returns the value at phase for the wave.
     *
     * @param  phase
     * @return the new value
     */
    protected abstract float computeValue(float phase);
}
