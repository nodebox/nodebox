package nodebox.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;

public abstract class AbstractGrob implements Grob {

    private TransformDelegate transformDelegate = DefaultTransformDelegate.getDefaultDelegate();
    private Transform transform;
    private AffineTransform savedTransform;

    //// Constructors ////

    protected AbstractGrob() {
        transform = new Transform();
    }

    protected AbstractGrob(AbstractGrob g) {
        this.transform = g.transform == null ? new Transform() : g.transform.clone();
    }

    //// Transformations ////

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public void transform(Transform t) {
        this.transform.append(t);
    }

    public void appendTransform(Transform transform) {
        this.transform.append(transform);
    }

    public void prependTransform(Transform transform) {
        this.transform.prepend(transform);
    }

    public void translate(float tx, float ty) {
        Transform t = Transform.translated(tx, ty);
        transformDelegate.transform(this, t);
    }

    public void rotate(float degrees) {
        Transform t = Transform.rotated(degrees);
        transformDelegate.transform(this, t);
    }

    public void rotateRadians(float radians) {
        Transform t = Transform.rotatedRadians(radians);
        transformDelegate.transform(this, t);
    }

    public void scale(float scale) {
        Transform t = Transform.scaled(scale);
        transformDelegate.transform(this, t);
    }

    public void scale(float sx, float sy) {
        Transform t = Transform.scaled(sx, sy);
        transformDelegate.transform(this, t);
    }

    public void skew(float skew) {
        Transform t = Transform.skewed(skew);
        transformDelegate.transform(this, t);
    }

    public void skew(float kx, float ky) {
        Transform t = Transform.skewed(kx, ky);
        transformDelegate.transform(this, t);
    }

    //// Graphics context ////

    protected void saveTransform(Graphics2D g) {
        assert (savedTransform == null);
        savedTransform = new AffineTransform(g.getTransform());
    }

    protected void restoreTransform(Graphics2D g) {
        assert (savedTransform != null);
        g.setTransform(savedTransform);
        savedTransform = null;
    }

    public TransformDelegate getTransformDelegate() {
        return transformDelegate;
    }

    public void setTransformDelegate(TransformDelegate d) {
        transformDelegate = d;
    }

    //// Object methods ////

    public abstract Grob clone();
}
