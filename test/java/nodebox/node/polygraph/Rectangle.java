package nodebox.node.polygraph;

public class Rectangle {

    public float x, y, width, height;

    public Rectangle() {
    }

    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rectangle rectangle = (Rectangle) o;

        if (Float.compare(rectangle.height, height) != 0) return false;
        if (Float.compare(rectangle.width, width) != 0) return false;
        if (Float.compare(rectangle.x, x) != 0) return false;
        if (Float.compare(rectangle.y, y) != 0) return false;

        return true;
    }

    @Override
    public String toString() {
        return "Rectangle{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}