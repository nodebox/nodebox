package net.nodebox.client;

import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ViewerPane extends Pane {

    private PaneHeader paneHeader;
    private Viewer viewer;
    private Node node;


    public ViewerPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public ViewerPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        JCheckBox showHandleBox = new JCheckBox("Handles");
        showHandleBox.setOpaque(false);
        showHandleBox.setSelected(true);
        showHandleBox.putClientProperty("JComponent.sizeVariant", "small");
        showHandleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewer.setShowHandle(((JCheckBox) e.getSource()).isSelected());
            }
        });
        paneHeader.add(showHandleBox);
        viewer = new Viewer(this, null);
        add(paneHeader, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNode(document.getActiveNetwork());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
        viewer.setNode(node);
    }

    public void setActiveNode(Node node) {
        viewer.setActiveNode(node);
    }

    @Override
    public void currentNodeChanged(Node activeNetwork) {
        setNode(activeNetwork);
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        setActiveNode(activeNode);
    }

    public Pane clone() {
        return new ViewerPane(getDocument());
    }
}
