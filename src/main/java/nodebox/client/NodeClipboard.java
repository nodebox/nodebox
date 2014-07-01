package nodebox.client;

import java.io.Serializable;

import nodebox.node.Node;

import com.google.common.collect.ImmutableList;

class NodeClipboard implements Serializable {
	private static final long serialVersionUID = 1L;
	final Node network;
    final ImmutableList<Node> nodes;

    NodeClipboard(Node network, Iterable<Node> nodes) {
        this.network = network;
        this.nodes = ImmutableList.copyOf(nodes);
    }
}