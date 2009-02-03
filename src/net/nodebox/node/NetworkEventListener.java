package net.nodebox.node;

import java.util.EventListener;

/**
 * A NetworkListener responds to events happening in the network: when the network was changed, when
 * it is going to be processed or has been processed, when nodes have been added or removed, connections
 * were made or destroyed.
 */
public interface NetworkEventListener extends EventListener {

    /**
     * Invoked when a node was added to the network.
     *
     * @param source the network this event comes from
     * @param node   the node added to the network
     */
    public void nodeAdded(Network source, Node node);

    /**
     * Invoked when a node was removed from the network.
     * The node property in the network event indicates the removed node.
     *
     * @param source the network this event comes from
     * @param node   the node removed from the network
     */
    public void nodeRemoved(Network source, Node node);

    /**
     * Invoked when a connection was added to the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param source     the network this event comes from
     * @param connection the new connection
     */
    public void connectionAdded(Network source, Connection connection);

    /**
     * Invoked when a connection was removed from the network.
     * The input and output properties in the network event
     * indicate the attributes of the connection.
     *
     * @param source     the network this event comes from
     * @param connection the removed connection
     */
    public void connectionRemoved(Network source, Connection connection);

    /**
     * Invoked when the rendered node was changed.
     * The node property in the network event holds the node that is now being rendered,
     * while the oldNode holds the previously rendered node. Both can be null.
     *
     * @param source the source network this event comes from
     * @param node   the new rendered node
     */
    public void renderedNodeChanged(Network source, Node node);

    /**
     * Invoked when a generic change to a node was made.
     * This could happen when the node was renamed or one if its parameters was added or deleted.
     * This event does not fire when the value of a node was changed, only its metadata.
     *
     * @param source the network this event comes from
     * @param node   the node that was changed
     */
    public void nodeChanged(Network source, Node node);


    /**
     * Invoked when the position of the node was changed, i.e. the node was moved.
     * This event does not fire when the value of a node was changed, only its position.
     *
     * @param source the network this event comes from
     * @param node   the node that was changed
     */
    public void nodePositionChanged(Network source, Node node);

}
