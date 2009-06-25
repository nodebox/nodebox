package nodebox.graphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OldGroup implements Grob {

    private ArrayList<Grob> items = new ArrayList<Grob>();

    public OldGroup() {
    }

    public OldGroup(OldGroup other) {
        for (Grob g : other.items) {
            add(g.clone());
        }
    }

    //// Container operations ////

    public void add(Grob g) {
        items.add(g);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public List<Grob> getItems() {
        return items;
    }

    public Grob get(int index) {
        try {
            return items.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Create copies of all grobs of the given group and append them to myself.
     *
     * @param g the group whose elements are appended.
     */
    public void extend(OldGroup g) {
        for (Grob grob : g.getItems()) {
            add(grob.clone());
        }
    }

    public void transform(Transform t) {
        for (Grob g : items) {
            g.transform(t);
        }
    }

    //// Geometry ////

    /**
     * Returns the bounding box of all elements in the group.
     *
     * @return a bounding box that contains all elements in the group.
     */
    public Rect getBounds() {
        if (items.isEmpty()) return new Rect();
        Rect r = null;
        for (Grob g : items) {
            if (r == null) {
                r = g.getBounds();
            } else {
                r = r.united(g.getBounds());
            }
        }
        return r;
    }

    public void inheritFromContext(GraphicsContext ctx) {
        throw new UnsupportedOperationException();
    }

    public void draw(Graphics2D g) {
        for (Grob grob : items) {
            grob.draw(g);
        }
    }

    //// Copy ////

    public OldGroup clone() {
        return new OldGroup(this);
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ">";
    }

}
