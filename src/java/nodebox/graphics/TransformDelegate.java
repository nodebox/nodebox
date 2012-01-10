package nodebox.graphics;

public interface TransformDelegate {
    public void transform(Grob g, Transform t);

    public void transform(Grob g, Transform t, boolean override);
}