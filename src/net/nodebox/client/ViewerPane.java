package net.nodebox.client;

import net.nodebox.node.Node;

import java.awt.*;

public class ViewerPane extends Pane {

    private PaneHeader paneHeader;
    private Viewer viewer;
    private Node node;
    private NButton handlesCheck, pointsCheck;


    public ViewerPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public ViewerPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        handlesCheck = new NButton(NButton.Mode.CHECK, "Handles");
        handlesCheck.setActionMethod(this, "toggleHandles");
        pointsCheck = new NButton(NButton.Mode.CHECK, "Points");
        pointsCheck.setActionMethod(this, "togglePoints");
        paneHeader.add(handlesCheck);
        // TODO: Add the following lines when viewer implements showPoints.
        //paneHeader.add(new Divider());
        //paneHeader.add(pointsCheck);
        viewer = new Viewer(this, null);
        add(paneHeader, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    public void toggleHandles() {
        viewer.setShowHandle(handlesCheck.isChecked());
    }

    public void togglePoints() {
        // TODO: setShowPoints not implemented in viewer yet
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

    public String getPaneName() {
        return "Viewer";
    }
}
