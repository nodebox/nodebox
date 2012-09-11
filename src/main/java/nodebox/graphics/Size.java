package nodebox.graphics;

import java.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Size implements Iterable {

    private double width, height;

    public Size() {
        this(0, 0);
    }

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public Size(Size sz) {
        this.width = sz.width;
        this.height = sz.height;
    }

    public Size(Dimension2D d) {
        this.width = d.getWidth();
        this.height = d.getHeight();
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Dimension2D getDimension2D() {
        return new Dimension(width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Size)) return false;
        Size sz = (Size) o;
        return width == sz.width && height == sz.height;
    }

    @Override
    public String toString() {
        return "Size(" + width + ", " + height + ")";
    }

    public Iterator<Double> iterator() {
        List<Double> list = new ArrayList<Double>();
        list.add(width);
        list.add(height);
        return list.iterator();
    }

    @Override
    public Size clone() {
        return new Size(this);
    }

    private class Dimension extends Dimension2D {
        private double width;
        private double height;

        private Dimension(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public void setSize(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }


}
