package nodebox.graphics;

public class DefaultTransformDelegate implements TransformDelegate {
    private static DefaultTransformDelegate delegate;

    private DefaultTransformDelegate() {

    }

    static DefaultTransformDelegate getDefaultDelegate() {
        if (delegate == null)
            delegate = new DefaultTransformDelegate();
        return delegate;
    }

    public void transform(Grob g, Transform t) {
        g.transform(t);
    }

    public void transform(Grob g, Transform t, boolean override) {
        g.transform(t);
    }
}