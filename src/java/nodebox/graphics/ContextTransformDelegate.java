package nodebox.graphics;

public class ContextTransformDelegate implements TransformDelegate {

    private GraphicsContext context;
    private Transform transform;
    private Transform currentTransform;

    public ContextTransformDelegate(GraphicsContext context) {
        this.context = context;
        transform = new Transform();
        currentTransform = new Transform();
    }

    public void transform(Grob g, Transform t) {
        transform(g, t, false);
    }

    public void transform(Grob g, Transform t, boolean override) {
        if (override) {
            currentTransform = new Transform();
        }

        if (!transform.getAffineTransform().isIdentity()) {
            Transform revertedTransform = transform.clone();
            revertedTransform.invert();
            g.transform(revertedTransform);
        }

        currentTransform.append(t);

        Rect bounds = g.getBounds();
        double dx = bounds.getX() + bounds.getWidth() / 2;
        double dy = bounds.getY() + bounds.getHeight() / 2;

        transform = currentTransform.clone();
        if (context.transform() == Transform.Mode.CENTER) {
            Transform n = new Transform();
            n.translate(dx, dy);
            transform.prepend(n);
            n = new Transform();
            n.translate(-dx, -dy);
            transform.append(n);
        }
        g.transform(transform);
    }
}