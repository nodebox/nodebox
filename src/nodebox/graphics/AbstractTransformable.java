package nodebox.graphics;

public abstract class AbstractTransformable implements Grob {

    private TransformDelegate transformDelegate = DefaultTransformDelegate.getDefaultDelegate();

    private Transform transform;
    
    //// Transformations ////

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

    public TransformDelegate getTransformDelegate() {
        return transformDelegate;
    }

    public void setTransformDelegate(TransformDelegate d) {
        transformDelegate = d;
    }
    
    public abstract Grob clone();
}
