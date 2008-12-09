package net.nodebox.event;

import net.nodebox.event.NetworkEvent;

import java.util.EventListener;


/**
 * A NetworkListener responds to events happening in the network: when the network was changed, when
 * it is going to be processed or has been processed, when nodes have been added or removed, connections
 * were made or destroyed.
 */
public interface NetworkListener extends EventListener {

    /**
     * Invoked when a node was added to the network.
     * The node property in the network event indicates the added node.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     */
    public void nodeAdded(NetworkEvent e);

    /**
     * Invoked when a node was removed from the network.
     * The node property in the network event indicates the removed node.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     */
    public void nodeRemoved(NetworkEvent e);

    /**
     * Invoked when a connection was added to the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#input
     * @see NetworkEvent#output
     */
    public void connectionAdded(NetworkEvent e);

    /**
     * Invoked when a connection was removed from the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#input
     * @see NetworkEvent#output
     */
    public void connectionRemoved(NetworkEvent e);

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
    public void nodeRenamed(NetworkEvent e);

    /**
     * Invoked when the rendered node was changed.
     * The node property in the network event holds the node that is now being rendered,
     * while the oldNode holds the previously rendered node. Both can be null.
     *
     * @param e a NetworkEvent object
     * @see NetworkEvent#node
     * @see NetworkEvent#oldNode
     */
    public void renderedNodeChanged(NetworkEvent e);

}

