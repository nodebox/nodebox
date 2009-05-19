package net.nodebox.node.polygraph;

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

    @Override
    public Point clone() {
        return new Point(x, y);
    }
}
