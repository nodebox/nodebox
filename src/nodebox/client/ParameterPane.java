package nodebox.client;

import nodebox.node.Node;

import javax.swing.*;
import java.awt.*;

public class ParameterPane extends Pane {

    private PaneHeader paneHeader;
    private ParameterView parameterView;
    private Node node;

    public ParameterPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public ParameterPane() {
        setLayout(new BorderLayout());
        paneHeader = new PaneHeader(this);
        NButton metadataButton = new NButton("Metadata", "res/parameter-metadata.png");
        metadataButton.setActionMethod(this, "editMetadata");
        paneHeader.add(metadataButton);
        parameterView = new ParameterView();
        add(paneHeader, BorderLayout.NORTH);
        add(parameterView, BorderLayout.CENTER);
    }

    public Pane clone() {
        return new ParameterPane(getDocument());
    }

    public String getPaneName() {
        return "Parameters";
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return parameterView;
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNode(document.getActiveNode());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        this.node = node;
        parameterView.setNode(node);
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        setNode(activeNode);
    }

    public void editMetadata() {
        if (node == null) return;
        NodeAttributesEditor editor = new NodeAttributesEditor(node);
        JFrame editorFrame = new JFrame(node.getName() + " Metadata");
        editorFrame.getContentPane().add(editor);
        editorFrame.setSize(580, 710);
        editorFrame.setResizable(false);
        // Center the frame based on the current window.
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null) {
            // If the current window could not be found, set to the default location.
            editorFrame.setLocationByPlatform(true);
        } else {
            int ancestorCenterX = w.getX() + w.getWidth() / 2;
            int ancestorCenterY = w.getY() + w.getHeight() / 2;
            int x = ancestorCenterX - editorFrame.getWidth() / 2;
            int y = ancestorCenterY - editorFrame.getHeight() / 2;
            editorFrame.setLocation(x, y);
        }
        editorFrame.setVisible(true);
    }
}
