package nodebox.client;

import nodebox.node.Node;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ViewerPane extends Pane implements ChangeListener {

    private PaneHeader paneHeader;
    private Viewer viewer;
    private Node node;
    private NButton handlesCheck, pointsCheck, pointNumbersCheck;
    private JSlider zoomSlider;

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
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 1, 1000, 100);
        zoomSlider.setSize(new Dimension(12, 125));
        zoomSlider.putClientProperty("JComponent.sizeVariant", "small");
        zoomSlider.addChangeListener(this);
        paneHeader.add(handlesCheck);
        paneHeader.add(pointsCheck);
        paneHeader.add(pointNumbersCheck);
        paneHeader.add(zoomSlider);
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

    /**
     * User dragged the zoom slider.
     *
     * @param e the change event
     */
    public void stateChanged(ChangeEvent e) {
        float zoomFactor = zoomSlider.getValue() / 100f;
        viewer.setZoomFactor(zoomFactor);
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
