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

    public static SquareWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new SquareWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return (phase / TWO_PI < 0.5 ? 1 : -1) * amplitude;
    }
}
