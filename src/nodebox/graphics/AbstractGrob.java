package nodebox.graphics;

import java.awt.geom.AffineTransform;
import java.awt.*;

public abstract class AbstractGrob implements Grob {

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
        transform.translate(tx, ty);
    }

    public void rotate(float degrees) {
        transform.rotate(degrees);
    }

    public void scale(float scale) {
        transform.scale(scale);
    }

    public void scale(float sx, float sy) {
        transform.scale(sx, sy);
    }

    public void skew(float skew) {
        transform.skew(skew);
    }

    public void skew(float kx, float ky) {
        transform.skew(kx, ky);
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

    public void inheritFromContext(GraphicsContext ctx) {
    }    
    
    //// Object methods ////

    public abstract Grob clone();
}
