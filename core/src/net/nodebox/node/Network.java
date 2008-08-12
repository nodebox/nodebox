/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nodebox.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Network extends Node {

    private HashMap<String, Node> nodes = new HashMap<String, Node>();
    private Node renderedNode = null;

    public static class NodeNotInNetwork extends RuntimeException {

        private Network network;
        private Node node;

        public NodeNotInNetwork(Network network, Node node) {
            this.network = network;
            this.node = node;
        }

        public Network getNetwork() {
            return network;
        }

        public Node getNode() {
            return node;
        }
    }

    public Network(String outputType) {
        super(outputType);
    }
    //// Container operations ////
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public int size() {
        return nodes.size();
    }

    public void add(Node node) {
        assert (node != null);
        if (contains(node)) {
            return;
        }
        if (contains(node.getName())) {
            throw new Node.InvalidName(node, node.getName(), "There is already a node named \"" + node.getName() + "\" in this network.");
        }
        node._setNetwork(this);
    // TODO: notify
    }

    public boolean remove(Node node) {
        assert (node != null);
        if (!contains(node)) {
            return false;
        }
        node.markDirty();
        // TODO: disconnect node from the old network.
        node.disconnect();
        nodes.remove(node.getName());
        if (node == renderedNode) {
            setRenderedNode(null);
        }
        // TODO: notify
        return true;
    }

    public boolean contains(Node node) {
        return nodes.containsValue(node);
    }

    public boolean contains(String nodeName) {
        return nodes.containsKey(nodeName);
    }

    public Node getNode(String nodeName) {
        if (!contains(nodeName)) {
            throw new Node.NotFound(this, nodeName);
        }
        return nodes.get(nodeName);
    }

    public Collection<Node> getNodes() {
        return new ArrayList<Node>(nodes.values());
    }
    //// Naming operations ////
    /**
     * The nodes doesn't have to be in the network, but it will get a name
     * that is unique for this network
     * @param node
     * @return the unique node name
     */
    public String setUniqueNodeName(Node node) {
        int counter = 1;
        while (true) {
            String suggestedName = node.defaultName() + counter;
            if (!contains(suggestedName)) {
                // We don't use rename here, since it assumes the node will be in 
                // this network.
                node.setName(suggestedName);
                return suggestedName;
            }
            ++counter;
        }
    }

    public boolean rename(Node node, String newName) {
        assert (contains(node));
        if (node.getName().equals(newName)) {
            return true;
        }
        if (contains(newName)) {
            return false;
        }
        nodes.remove(node.getName());
        node._setName(newName);
        nodes.put(newName, node);
        return true;
    }

    //// Rendered node ////
    public Node getRenderedNode() {
        return renderedNode;
    }

    public void setRenderedNode(Node renderedNode) {
        if (renderedNode != null && !contains(renderedNode)) {
            throw new NodeNotInNetwork(this, renderedNode);
        }
        if (this.renderedNode == renderedNode) return;        
        this.renderedNode = renderedNode;
        markDirty();
    // TODO: notify
    }

    //// Processing ////
    @Override
    protected void process() {
        if (renderedNode == null) {
            throw new ProcessingError(this, "No node to render");
        }
        assert(contains(renderedNode));
        renderedNode.update();
        // TODO: Channel support
        _setOutput(renderedNode.outputAsData());
    }
}
