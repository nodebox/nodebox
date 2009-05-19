package net.nodebox.node;

import java.util.EventListener;

/**
 * A NodeChildListener responds to events happening in the children of this node.
 */
public interface NodeChildListener extends EventListener {

    /**
     * Invoked when a node was added to the network.
     *
     * @param source the node this event comes from
     * @param child  the child added to the source node
     */
    public void childAdded(Node source, Node child);

    /**
     * Invoked when a node was removed from the network.
     * The node property in the network event indicates the removed node.
     *
     * @param source the node this event comes from
     * @param child  the child removed from the source node
     */
    public void childRemoved(Node source, Node child);

    /**
     * Invoked when a connection was added to the node.
     *
     * @param source     the node this event comes from
     * @param connection the new connection
     */
    public void connectionAdded(Node source, Connection connection);

    /**
     * Invoked when a connection was removed from the node.
     *
     * @param source     the node this event comes from
     * @param connection the removed connection
     */
    public void connectionRemoved(Node source, Connection connection);

    /**
     * Invoked when the rendered child was changed.
     *
     * @param source the node this event comes from
     * @param child  the new rendered child
     */
    public void renderedChildChanged(Node source, Node child);

    /**
     * Invoked when an attribute change was made to the child.
     *
     * This could happen when the node was renamed or one if its parameters was added or deleted.
     * This event does not fire when the value of a node was changed, only its metadata.
     *
     * @param source the node this event comes from
     * @param child the child that was changed
     */
    public void childAttributeChanged(Node source, Node child);
}
