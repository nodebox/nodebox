package net.nodebox.event;

import net.nodebox.event.NetworkEvent;
import net.nodebox.event.NetworkListener;

/**
 * An abstract adapter class for receiving network events. The methods in
 * this class are empty. This class exists only as a convenience for creating
 * listener objects.
 */
public abstract class NetworkAdapter implements NetworkListener {

    /**
     * Invoked when a node was added to the network.
     * The node property in the network event indicates the added node.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     */
    public void nodeAdded(NetworkEvent e) {
    }

    /**
     * Invoked when a node was removed from the network.
     * The node property in the network event indicates the removed node.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     */
    public void nodeRemoved(NetworkEvent e) {
    }

    /**
     * Invoked when a connection was added to the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#input
     * @see NetworkEvent#output
     */
    public void connectionAdded(NetworkEvent e) {
    }

    /**
     * Invoked when a connection was removed from the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#input
     * @see NetworkEvent#output
     */
    public void connectionRemoved(NetworkEvent e) {
    }

    /**
     * Invoked when a node was renamed.
     * The node property in the network event holds the node which was renamed,
     * while the oldName and newName contain the name before and after the change
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     * @see NetworkEvent#oldName
     * @see NetworkEvent#newName
     */
    public void nodeRenamed(NetworkEvent e) {
    }

    /**
     * Invoked when the rendered node was changed.
     * The node property in the network event holds the node that is now being rendered,
     * while the oldNode holds the previously rendered node. Both can be null.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     * @see NetworkEvent#oldNode
     */
    public void renderedNodeChanged(NetworkEvent e) {
    }

}
