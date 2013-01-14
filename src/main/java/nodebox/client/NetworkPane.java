package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeRenderException;
import nodebox.ui.*;
import org.python.core.PyException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static nodebox.ui.ExceptionDialog.getRootCause;

public class NetworkPane extends Pane {

    private final NodeBoxDocument document;
    private final PaneHeader paneHeader;
    private final JLabel errorLabel;
    private final NetworkView networkView;
    private Throwable nodeRenderException;

    public NetworkPane(NodeBoxDocument document) {
        this.document = document;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        paneHeader = new PaneHeader("Network");
        paneHeader.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        NButton newNodeButton = new NButton("New Node", getClass().getResourceAsStream("/network-new-node.png"));
        newNodeButton.setToolTipText("New Node (TAB)");
        newNodeButton.setActionMethod(this, "showNodeSelectionDialog");
        paneHeader.add(newNodeButton);
        add(paneHeader);

        errorLabel = new MessageBar("Error");
        errorLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ExceptionDialog ed = new ExceptionDialog(null, nodeRenderException, "", false);
                ed.setVisible(true);
            }
        });
        errorLabel.setVisible(false);
        add(errorLabel);

        networkView = new NetworkView(document);
        document.addZoomListener(networkView);
        networkView.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        add(networkView);
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public Pane duplicate() {
        return new NetworkPane(document);
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return networkView;
    }

    public void showNodeSelectionDialog() {
        document.showNodeSelectionDialog();
    }

    public void setError(Throwable e) {
        StringBuilder sb = new StringBuilder("<html>&nbsp;&nbsp;&nbsp;");
        if (e instanceof NodeRenderException) {
            Node node = ((NodeRenderException) e).getNode();
            sb.append("<b>");
            sb.append(node.getName());
            sb.append(":</b> ");
        }
        sb.append("<u>");
        nodeRenderException = getRootCause(e);
        if (nodeRenderException instanceof PyException) {
            PyException ex = (PyException) nodeRenderException;
            if (ex.value != null) {
                sb.append(ex.value.toString());
            } else {
                sb.append(ex.toString());
            }
        } else if (nodeRenderException instanceof OutOfMemoryError) {
            sb.append("Out of memory. Are you trying to process an infinite list?");
        } else {
            sb.append(nodeRenderException.getMessage());
        }
        sb.append("</u></html>");
        errorLabel.setText(sb.toString());
        errorLabel.setVisible(true);
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

}
