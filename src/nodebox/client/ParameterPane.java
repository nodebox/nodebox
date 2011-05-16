package nodebox.client;

import nodebox.node.Node;

import javax.swing.*;
import java.awt.*;

public class ParameterPane extends Pane {

    private PaneHeader paneHeader;
    private ParameterView parameterView;
    private Node node;

    public ParameterPane(NodeBoxDocument document) {
        super(document);
        setLayout(new BorderLayout());
        paneHeader = new PaneHeader(this);
        NButton metadataButton = new NButton("Metadata", "res/parameter-metadata.png");
        metadataButton.setActionMethod(this, "editMetadata");
        paneHeader.add(metadataButton);
        parameterView = new ParameterView(this);
        add(paneHeader, BorderLayout.NORTH);
        add(parameterView, BorderLayout.CENTER);
        setNode(document.getActiveNode());
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

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (node == null) {
            Node rootNode = getDocument().getNodeLibrary().getRootNode();
            if (parameterView.getNode() == rootNode) return;
            parameterView.setNode(rootNode);
        }
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
        getDocument().addEdit("Node Metadata");
        NodeAttributesEditor editor = new NodeAttributesEditor(node);
        Frame frame = (Frame) SwingUtilities.getRoot(this);
        JDialog editorDialog = new JDialog(frame, node.getName() + " Metadata");
        editorDialog.getContentPane().add(editor);
        editorDialog.setSize(580, 751);
        editorDialog.setResizable(false);
        editorDialog.setModal(true);
        editorDialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        // Center the frame based on the current window.
        if (frame == null) {
            // If the current window could not be found, set to the default location.
            editorDialog.setLocationByPlatform(true);
        } else {
            int ancestorCenterX = frame.getX() + frame.getWidth() / 2;
            int ancestorCenterY = frame.getY() + frame.getHeight() / 2;
            int x = ancestorCenterX - editorDialog.getWidth() / 2;
            int y = ancestorCenterY - editorDialog.getHeight() / 2;
            editorDialog.setLocation(x, y);
        }
        editorDialog.setVisible(true);
    }
}
