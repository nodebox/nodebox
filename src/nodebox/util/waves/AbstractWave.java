package nodebox.util.waves;

public abstract class AbstractWave {

    public static final float PI = 3.14159265358979323846f;
    public static final float TWO_PI = 2 * PI;

    private float period;
    private float phase;
    private float originalPhase;
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

    public void setPhase(float phase) {
        phase %= TWO_PI;
        if (phase < 0) phase += TWO_PI;
        this.phase = this.originalPhase = phase;
    }

    public float getValueAt(float t) {
        float phase;
        if (t % period == 0)
            phase = originalPhase;
        else
            phase = (this.phase + t * frequency) % TWO_PI;
        if (phase < 0) phase += TWO_PI;
        return computeValue(phase) + offset;
    }

    protected abstract float computeValue(float phase);
}
