package nodebox.ui;

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

    public abstract PaneHeader getPaneHeader();

    public abstract PaneView getPaneView();

    /**
     * Splits the pane into two vertically aligned panes. This pane will be relocated as the top pane.
     * The bottom pane will be a duplicate of this pane.
     */
    public void splitTopBottom() {
        split(JSplitPane.VERTICAL_SPLIT);
    }

    /**
     * Splits the pane into two horizontally aligned panes. This pane will be relocated as the left pane.
     * The right pane will be a duplicate of this pane.
     */
    public void splitLeftRight() {
        split(JSplitPane.HORIZONTAL_SPLIT);
    }

    private void split(int orientation) {
        Container parent = getParent();
        if (parent instanceof JSplitPane) {
            JSplitPane parentSplit = (JSplitPane) parent;
            boolean first = parentSplit.getTopComponent() == this;
            if (first) {
                parentSplit.setTopComponent(null);
            } else {
                parentSplit.setBottomComponent(null);
            }
            CustomSplitPane split = new CustomSplitPane(orientation, this, this.duplicate());
            if (first) {
                parentSplit.setTopComponent(split);
            } else {
                parentSplit.setBottomComponent(split);
            }
            parentSplit.validate();
        } else {
            parent.remove(this);
            CustomSplitPane split = new CustomSplitPane(orientation, this, this.duplicate());
            parent.add(split);
            parent.validate();
        }
    }

    public void close() {
        Container parent = getParent();
        if (!(parent instanceof JSplitPane)) return;
        JSplitPane split = (JSplitPane) parent;
        Component firstComponent = split.getTopComponent();
        Component secondComponent = split.getBottomComponent();
        Component remainingComponent = firstComponent == this ? secondComponent : firstComponent;
        split.setTopComponent(null);
        split.setBottomComponent(null);
        Container grandParent = parent.getParent();
        if (grandParent instanceof JSplitPane) {
            JSplitPane grandSplit = (JSplitPane) grandParent;
            // Remove the split pane.
            if (split == grandSplit.getTopComponent()) {
                grandSplit.setTopComponent(remainingComponent);
            } else {
                grandSplit.setBottomComponent(remainingComponent);
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
            Constructor c = paneType.getConstructors()[0];
            newPane = (Pane) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate new " + paneType, e);
        }
        Container parent = getParent();
        if (parent instanceof JSplitPane) {
            JSplitPane parentSplit = (JSplitPane) parent;
            boolean first = parentSplit.getTopComponent() == this;
            if (first) {
                parentSplit.setTopComponent(newPane);
            } else {
                parentSplit.setBottomComponent(newPane);
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
