package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;

import java.awt.*;

public class ViewerPane extends Pane {

    private PaneHeader paneHeader;
    private NetworkAddressBar networkAddressBar;
    private Viewer viewer;
    private Network network;


    public ViewerPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public ViewerPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        networkAddressBar = new NetworkAddressBar(this);
        paneHeader.add(networkAddressBar);
        viewer = new Viewer(this, null);
        add(paneHeader, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNetwork(document.getActiveNetwork());
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
        networkAddressBar.setNode(network);
        viewer.setNetwork(network);
    }

    public void setActiveNode(Node node) {
        viewer.setActiveNode(node);
    }

    @Override
    public void activeNetworkChanged(Network activeNetwork) {
        setNetwork(activeNetwork);
    }

    @Override
    public void activeNodeChanged(Node activeNode) {
        setActiveNode(activeNode);
    }

    public Pane clone() {
        return new ViewerPane(getDocument());
    }
}
