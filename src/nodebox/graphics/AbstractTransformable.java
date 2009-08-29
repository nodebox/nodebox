package nodebox.graphics;

public abstract class AbstractTransformable implements Grob {

    //// Transformations ////

    public void translate(float tx, float ty) {
        Transform t = Transform.translated(tx, ty);
        transform(t);
    }

    public void rotate(float degrees) {
        Transform t = Transform.rotated(degrees);
        transform(t);
    }

    public void rotateRadians(float radians) {
        Transform t = Transform.rotatedRadians(radians);
        transform(t);
    }

    public void scale(float scale) {
        Transform t = Transform.scaled(scale);
        transform(t);
    }

    public void scale(float sx, float sy) {
        Transform t = Transform.scaled(sx, sy);
        transform(t);
    }

    public void skew(float skew) {
        Transform t = Transform.skewed(skew);
        transform(t);
    }

    public void skew(float kx, float ky) {
        Transform t = Transform.skewed(kx, ky);
        transform(t);
    }

    public abstract Grob clone();
}
