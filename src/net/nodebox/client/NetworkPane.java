package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

public class NetworkPane extends Pane implements PropertyChangeListener {

    private PaneHeader paneHeader;
    private NetworkAddressBar networkAddressBar;
    private NetworkView networkView;
    private Network network;


    public NetworkPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public NetworkPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        networkAddressBar = new NetworkAddressBar(this);
        paneHeader.add(networkAddressBar);
        networkView = new NetworkView(this, null);
        networkView.addPropertyChangeListener(this);
        add(paneHeader, BorderLayout.NORTH);
        add(networkView, BorderLayout.CENTER);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNetwork(document.getActiveNetwork());
    }

    public Pane clone() {
        return new NetworkPane(getDocument());
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
        networkAddressBar.setNode(network);
        networkView.setNetwork(network);
        networkView.select(getDocument().getActiveNode());
    }

    @Override
    public void currentNodeChanged(Node activeNetwork) {
        setNetwork(activeNetwork);
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        // If the active node is already selected, don't change the selection.
        // This avoids nasty surprises when multiple nodes (including the active one)
        // are selected.
        if (networkView.isSelected(activeNode)) return;
        networkView.singleSelect(activeNode);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (!evt.getPropertyName().equals(NetworkView.SELECT_PROPERTY)) return;
        Set<NodeView> selection = (Set<NodeView>) evt.getNewValue();
        // If there is no selection, set the active node to null.
        if (selection == null || selection.isEmpty()) {
            getDocument().setActiveNode(null);
        } else if (selection.size() == 1) {
            // If there is one element selected, that will be the new active node.
            NodeView firstElement = selection.iterator().next();
            getDocument().setActiveNode(firstElement.getNode());
        }
    }
}
