package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;

import java.util.EventListener;

public interface DocumentFocusListener extends EventListener {

    public void activeNetworkChanged(Network activeNetwork);

    public void activeNodeChanged(Node activeNode);
}
