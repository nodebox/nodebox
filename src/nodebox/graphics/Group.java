package nodebox.graphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Group extends Grob {

    private ArrayList<Grob> grobs = new ArrayList<Grob>();

    public Group() {
    }

    public Group(Group other) {
        super(other);
        for (Grob g : other.grobs) {
            add(g.clone());
        }
    }

    //// Container operations ////

    public void add(Grob g) {
        grobs.add(g);
    }

    public int size() {
        return grobs.size();
    }

    public void clear() {
        grobs.clear();
    }

    public List<Grob> getGrobs() {
        return grobs;
    }

    public Grob get(int index) {
        try {
            return grobs.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Create copies of all grobs of the given group and append them to myself.
     *
     * @param g the group whose elements are appended.
     */
    public void extend(Group g) {
        for (Grob grob : g.getGrobs()) {
            add(grob.clone());
        }
    }

    //// Geometry ////

    /**
     * Returns the bounding box of all elements in the group.
     *
     * @return a bounding box that contains all elements in the group.
     */
    public Rect getBounds() {
        if (grobs.isEmpty()) {
            return new Rect();
        }
        Rect r = null;
        // Note that, to calculate the bounding box of a group, we use the frames of the inner elements.
        // This means we take the child grobs' transforms in account.
        for (Grob g : grobs) {
            if (r == null) {
                r = g.getFrame();
            } else {
                r = r.united(g.getFrame());
            }
        }
        return r;
    }

    public void draw(Graphics2D g) {
        setupTransform(g);
        for (Grob grob : grobs) {
            grob.draw(g);
        }
        restoreTransform(g);
    }

    //// Query methods ////

    @Override
    public java.util.List<Grob> getChildren(Class grobClass) {
        ArrayList<Grob> childGrobs = new ArrayList<Grob>();
        for (Grob grob : grobs) {
            if (grobClass.isAssignableFrom(grob.getClass())) {
                childGrobs.add(grob);
            }
            childGrobs.addAll(grob.getChildren(grobClass));
        }
        return childGrobs;
    }

    //// Copy ////

    public Group clone() {
        return new Group(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Group)) return false;
        Group other = (Group) obj;
        return grobs.equals(other.grobs)
                && super.equals(other);
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ">";
    }

}
