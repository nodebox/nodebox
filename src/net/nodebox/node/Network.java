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

import net.nodebox.event.NetworkListener;
import net.nodebox.event.NetworkEvent;
import net.nodebox.event.ProcessingListener;
import net.nodebox.event.DirtyListener;

import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public abstract class Network extends Node {

    /**
     * A list of all the nodes in this network.
     */
    private HashMap<String, Node> nodes = new HashMap<String, Node>();

    /**
     * The node being rendered in this network.
     */
    private Node renderedNode = null;

    /**
     * A list of event listeners for this component.
     */
    private transient EventListenerList listenerList = new EventListenerList();

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

    //// Constructors ////

    public Network(Parameter.Type outputType) {
        super(outputType);
    }

    public Network(Parameter.Type outputType, String name) {
        super(outputType, name);
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
     *
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
    }

    //// Processing ////

    @Override
    protected boolean process(ProcessingContext ctx) {
        if (renderedNode == null) {
            addError("No node to render");
            return false;
        }
        assert (contains(renderedNode));
        renderedNode.update();
        // TODO: place output of rendered node into network output.
        return true;
    }

    //// Network events ////

    /**
     * Add the specified network listener to receive
     * network events from this network.
     *
     * @param l the network listener to add.
     */
    public synchronized void addNetworkListener(NetworkListener l) {
        listenerList.add(NetworkListener.class, l);
    }

    /**
     * Removes the specified network listener so that it no longer
     * receives network events from this network.
     *
     * @param l the network listener to be removed
     */
    public synchronized void removeNetworkListener(NetworkListener l) {
        listenerList.remove(NetworkListener.class, l);
    }

    /**
     * Returns an array of all the <code>NetworkListener</code>s added
     * to this Network with addNetworkListener().
     *
     * @return all of the <code>NetworkListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public synchronized NetworkListener[] getNetworkListeners() {
        return (NetworkListener[]) listenerList.getListeners(
                NetworkListener.class);
    }

    /**
     * Notifies all listeners that a node was added to the network.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireNodeAdded(Node node) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, node);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).nodeAdded(e);
            }
        }
    }

    /**
     * Notifies all listeners that a node was removed from the network.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireNodeRemoved(Node node) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, node);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).nodeRemoved(e);
            }
        }
    }

    /**
     * Notifies all listeners that a connection was added to the network.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */

    protected void fireConnectionAdded(Parameter input, Parameter output) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, input, output);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).connectionAdded(e);
            }
        }

    }

    /**
     * Notifies all listeners that a connection was removed from the network.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireConnectionRemoved(Parameter input, Parameter output) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, input, output);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).connectionRemoved(e);
            }
        }
    }

    /**
     * Notifies all listeners that a node was renamed.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */

    protected void fireNodeRenamed(Node node, String oldName, String newName) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, node, oldName, newName);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).nodeRenamed(e);
            }
        }
    }

    /**
     * Notifies all listeners that the rendered node was changed.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */

    protected void fireRenderedNodeChanged(Node oldNode, Node newNode) {
        Object[] listeners = listenerList.getListenerList();
        NetworkEvent e = new NetworkEvent(this, oldNode, newNode);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).renderedNodeChanged(e);
            }
        }
    }

    /**
     * Add the specified processing listener to receive
     * processing events from this network.
     *
     * @param l the processing listener to add.
     */
    public synchronized void addProcessingListener(ProcessingListener l) {
        listenerList.add(ProcessingListener.class, l);
    }

    /**
     * Removes the specified processing listener so that it no longer
     * receives processing events from this network.
     *
     * @param l the processing listener to be removed
     */
    public synchronized void removeProcessingListener(ProcessingListener l) {
        listenerList.remove(ProcessingListener.class, l);
    }

    /**
     * Returns an array of all the <code>ProcessingListener</code>s added
     * to this Network with addProcessingListener().
     *
     * @return all of the <code>ProcessingListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public synchronized ProcessingListener[] geProcessingListeners() {
        return (ProcessingListener[]) listenerList.getListeners(
                ProcessingListener.class);
    }

    /**
     * Notifies all listeners that the network starts processing.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireStartProcessing(Network network) {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ProcessingListener.class) {
                ((ProcessingListener) listeners[i + 1]).startProcessing(network);
            }
        }
    }

    /**
     * Notifies all listeners that the network ended processing.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireEndProcessing(Network network) {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ProcessingListener.class) {
                ((ProcessingListener) listeners[i + 1]).endProcessing(network);
            }
        }
    }

    /**
     * Add the specified dirty listener to receive
     * dirty events from this network.
     *
     * @param l the dirty listener to add.
     */
    public synchronized void addDirtyListener(DirtyListener l) {
        listenerList.add(DirtyListener.class, l);
    }

    /**
     * Removes the specified dirty listener so that it no longer
     * receives dirty events from this network.
     *
     * @param l the dirty listener to be removed
     */
    public synchronized void removeDirtyListener(DirtyListener l) {
        listenerList.remove(DirtyListener.class, l);
    }

    /**
     * Returns an array of all the <code>DirtyListener</code>s added
     * to this Network with addDirtyListener().
     *
     * @return all of the <code>DirtyListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public synchronized DirtyListener[] getDirtyListeners() {
        return (DirtyListener[]) listenerList.getListeners(
                DirtyListener.class);
    }

    /**
     * Notifies all listeners that the network is dirty.
     * The listener list is processed in last to first order.
     *
     * @see javax.swing.event.EventListenerList
     */
    protected void fireNetworkDirty(Network network) {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DirtyListener.class) {
                ((DirtyListener) listeners[i + 1]).networkDirty(network);
            }
        }
    }

    /**
     * Mark this network as dirty.
     */
    public void markDirty() {
        super.markDirty();
        fireNetworkDirty(this);
    }
}
