package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Constructor;

public abstract class Pane extends JPanel implements FocusListener {

    private Component mainComponent;

    public Pane() {
        addFocusListener(this);
    }

    public abstract Pane duplicate();

    public abstract String getPaneName();

    public abstract PaneHeader getPaneHeader();

    public abstract PaneView getPaneView();

    /**
     * Splits the pane into two vertically aligned panes. This pane will be relocated as the top pane.
     * The bottom pane will be a duplicate of this pane.
     */
    public void splitTopBottom() {
        split(NSplitter.Orientation.VERTICAL);
    }

    /**
     * Splits the pane into two horizontally aligned panes. This pane will be relocated as the left pane.
     * The right pane will be a duplicate of this pane.
     */
    public void splitLeftRight() {
        split(NSplitter.Orientation.HORIZONTAL);
    }

    private void split(NSplitter.Orientation orientation) {
        Container parent = getParent();
        if (parent instanceof PaneSplitter) {
            PaneSplitter parentSplit = (PaneSplitter) parent;
            boolean first = parentSplit.getFirstComponent() == this;
            if (first) {
                parentSplit.setFirstComponent(null);
            } else {
                parentSplit.setSecondComponent(null);
            }
            PaneSplitter split = new PaneSplitter(orientation, this, this.duplicate());
            if (first) {
                parentSplit.setFirstComponent(split);
            } else {
                parentSplit.setSecondComponent(split);
            }
            parentSplit.validate();
        } else {
            parent.remove(this);
            PaneSplitter split = new PaneSplitter(orientation, this, this.duplicate());
            parent.add(split);
            parent.validate();
        }
    }

    public void close() {
        Container parent = getParent();
        if (!(parent instanceof PaneSplitter)) return;
        PaneSplitter split = (PaneSplitter) parent;
        JComponent firstComponent = split.getFirstComponent();
        JComponent secondComponent = split.getSecondComponent();
        JComponent remainingComponent = firstComponent == this ? secondComponent : firstComponent;
        split.setFirstComponent(null);
        split.setSecondComponent(null);
        Container grandParent = parent.getParent();
        if (grandParent instanceof PaneSplitter) {
            PaneSplitter grandSplit = (PaneSplitter) grandParent;
            // Remove the split pane.
            if (split == grandSplit.getFirstComponent()) {
                grandSplit.setFirstComponent(remainingComponent);
            } else {
                grandSplit.setSecondComponent(remainingComponent);
            }
        } else {
            grandParent.remove(parent);
            grandParent.add(remainingComponent);
        }
        grandParent.validate();
    }

    public void changePaneType(Class paneType) {
        if (!Pane.class.isAssignableFrom(paneType)) return;
        final Pane newPane;
        try {
            Constructor c = paneType.getConstructor(NodeBoxDocument.class);
            newPane = (Pane) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate new " + paneType, e);
        }
        Container parent = getParent();
        if (parent instanceof PaneSplitter) {
            PaneSplitter parentSplit = (PaneSplitter) parent;
            boolean first = parentSplit.getFirstComponent() == this;
            if (first) {
                parentSplit.setFirstComponent(newPane);
            } else {
                parentSplit.setSecondComponent(newPane);
            }
        } else {
            Dimension d = getSize();
            parent.remove(this);
            parent.add(newPane);
            newPane.setSize(d);
        }
        parent.validate();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                newPane.requestFocus();
            }
        });
    }

    public Component getMainComponent() {
        return mainComponent;
    }

    public void setMainComponent(Component c) {
        this.mainComponent = c;
    }

    public void focusGained(FocusEvent focusEvent) {
        if (mainComponent != null) {
            mainComponent.requestFocus();
        }
    }

    public void focusLost(FocusEvent focusEvent) {
    }
}
