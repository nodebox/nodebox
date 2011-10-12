package nodebox.client;

import java.awt.*;

public class NetworkPane extends Pane {

    private final NodeBoxDocument document;
    private final PaneHeader paneHeader;
    private final NetworkView networkView;

    public NetworkPane(NodeBoxDocument document) {
        this.document = document;
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        NButton newNodeButton = new NButton("New Node", "res/network-new-node.png");
        newNodeButton.setToolTipText("New Node (TAB)");
        newNodeButton.setActionMethod(this, "createNewNode");
        paneHeader.add(newNodeButton);
        networkView = new NetworkView(document);
        add(paneHeader, BorderLayout.NORTH);
        add(networkView, BorderLayout.CENTER);
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

    public String getPaneName() {
        return "Network";
    }

    public PaneView getPaneView() {
        return networkView;
    }

    public void createNewNode() {
        networkView.showNodeSelectionDialog();
    }

//    public void propertyChange(PropertyChangeEvent evt) {
//        if (!evt.getPropertyName().equals(NetworkView.SELECT_PROPERTY)) return;
//        Set<NodeView> selection = (Set<NodeView>) evt.getNewValue();
//        // If there is no selection, set the active node to null.
//        if (selection == null || selection.isEmpty()) {
//            getDocument().setActiveNode((Node) null);
//        } else {
//            // If the active node is in the new selection leave the active node as is.
//            NodeView nv = networkView.getNodeView(getDocument().getActiveNode());
//            if (selection.contains(nv)) return;
//            // If there are multiple elements selected, the first one will be the active node.
//            NodeView firstElement = selection.iterator().next();
//            getDocument().setActiveNode(firstElement.getNode());
//        }
//    }

}
