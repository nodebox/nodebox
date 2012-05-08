package nodebox.node.polygraph;

/**
 * A simple 2-dimensional point with floating point values.
 */
public class Point {

    public float x, y;

    public Point() {
    }

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public Point clone() {
        return new Point(x, y);
    }
}
