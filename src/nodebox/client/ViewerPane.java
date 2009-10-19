package nodebox.client;

import nodebox.node.Node;

import java.awt.*;

public class ViewerPane extends Pane {

    private PaneHeader paneHeader;
    private Viewer viewer;
    private Node node;
    private NButton handlesCheck, pointsCheck, pointNumbersCheck;


    public ViewerPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public ViewerPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        handlesCheck = new NButton(NButton.Mode.CHECK, "Handles");
        handlesCheck.setChecked(true);
        handlesCheck.setActionMethod(this, "toggleHandles");
        pointsCheck = new NButton(NButton.Mode.CHECK, "Points");
        pointsCheck.setActionMethod(this, "togglePoints");
        pointNumbersCheck = new NButton(NButton.Mode.CHECK, "Point Numbers");
        pointNumbersCheck.setActionMethod(this, "togglePointNumbers");
        paneHeader.add(handlesCheck);
        paneHeader.add(pointsCheck);
        paneHeader.add(pointNumbersCheck);
        viewer = new Viewer(this, null);
        add(paneHeader, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    public void toggleHandles() {
        viewer.setShowHandle(handlesCheck.isChecked());
    }

    public void togglePoints() {
        viewer.setShowPoints(pointsCheck.isChecked());
    }

    public void togglePointNumbers() {
        viewer.setShowPointNumbers(pointNumbersCheck.isChecked());
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
