package nodebox.util.waves;

public class SineWave extends AbstractWave {
    public SineWave(float period, float amplitude) {
        super(period, 0f, amplitude);
    }

    public SineWave(float period, float phase, float amplitude) {
        super(period, phase, amplitude);
    }

    public SineWave(float period, float phase, float amplitude, float offset) {
        super(period, phase, amplitude, offset);
    }

    public static SineWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new SineWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return (float) Math.sin(phase) * amplitude;
    }
}
