package net.nodebox.client;

import net.nodebox.node.Network;
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

    public void activeNetworkChanged(Network activeNetwork) {
    }

    public void activeNodeChanged(Node activeNode) {
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
