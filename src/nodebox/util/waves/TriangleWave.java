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

    public static TriangleWave from(float min, float max, float period) {
        float amplitude = (max - min) / 2;
        float offset = min + amplitude;
        return new TriangleWave(period, 0f, amplitude, offset);
    }

    protected float computeValue(float phase) {
        return Math.abs((phase / TWO_PI) * 2 - 1) * amplitude * 2 - amplitude;
    }
}
