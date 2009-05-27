package net.nodebox.client;

import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;

public abstract class Pane extends JPanel implements DocumentFocusListener {

    private NodeBoxDocument document;

    public Pane() {
    }

    public Pane(NodeBoxDocument document) {
        setDocument(document);
    }

    public void setDocument(NodeBoxDocument document) {
        NodeBoxDocument oldDocument = this.document;
        if (oldDocument != null)
            oldDocument.removeDocumentFocusListener(this);
        this.document = document;
        if (document != null)
            document.addDocumentFocusListener(this);
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public abstract Pane clone();

    public abstract String getPaneName();

    public void currentNodeChanged(Node activeNetwork) {
    }

    public void focusedNodeChanged(Node activeNode) {
    }

    /**
     * Splits the pane into two vertically aligned panes. This pane will be relocated as the top pane.
     * The bottom pane will be a clone of this pane.
     */
    public void splitTopBottom() {
        split(JSplitPane.VERTICAL_SPLIT);
    }

    /**
     * Splits the pane into two horizontally aligned panes. This pane will be relocated as the left pane.
     * The right pane will be a clone of this pane.
     */
    public void splitLeftRight() {
        split(JSplitPane.HORIZONTAL_SPLIT);
    }

    private void split(int orientation) {
        Dimension d = getSize();
        Container parent = getParent();
        parent.remove(this);
        PaneSplitter split = new PaneSplitter(orientation, this, this.clone());
        split.setSize(d);
        parent.add(split);
        parent.validate();
    }

    public void close() {
        Container parent = getParent();
        if (!(parent instanceof PaneSplitter)) return;
        PaneSplitter split = (PaneSplitter) parent;
        Component left = split.getLeftComponent();
        Component right = split.getRightComponent();
        Component remainingComponent = left == this ? right : left;
        split.remove(left);
        split.remove(right);
        Container grandParent = parent.getParent();
        if (!(grandParent instanceof PaneSplitter)) return;
        PaneSplitter grandSplit = (PaneSplitter) grandParent;
        Component grandLeft = grandSplit.getLeftComponent();
        Component grandRight = grandSplit.getRightComponent();
        String constraint = split == grandLeft ? JSplitPane.LEFT : JSplitPane.RIGHT;
        // Remove the split pane.
        grandSplit.remove(split);
        grandSplit.add(remainingComponent, constraint);

    }

    public void changePaneType(Class paneType) {
        if (!Pane.class.isAssignableFrom(paneType)) return;
        Pane newPane;
        try {
            newPane = (Pane) paneType.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        newPane.setDocument(getDocument());
        Container parent = getParent();
        Dimension d = getSize();

        parent.remove(this);
        parent.add(newPane);
        newPane.setSize(d);
        parent.validate();
    }

}
