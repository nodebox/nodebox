package net.nodebox.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class Group extends Grob {

    private ArrayList<Grob> grobs = new ArrayList<Grob>();

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

    public Rect getBounds() {
        if (grobs.isEmpty()) {
            return new Rect();
        }
        Rect r = null;
        for (Grob g : grobs) {
            if (r == null) {
                r = g.getBounds();
            } else {
                r = r.united(g.getBounds());
            }
        }
        return r;
    }

    public void draw(Graphics2D g) {
        AffineTransform t = g.getTransform();
        getTransform().apply(g, getBounds());
        for (Grob grob : grobs) {
            grob.draw(g);
        }
        g.setTransform(t);
    }

    //// Copy ////

    public Grob clone() {
        Group newGroup = new Group();
        newGroup.setTransform(getTransform().clone());
        for (Grob g : grobs) {
            newGroup.add(g.clone());
        }
        return newGroup;
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ">";
    }


}
