package net.nodebox.node;

import java.util.EventListener;

/**
 * A NodeAttributeListener responds to changes in the metadata of the node, such as its name, position, etc.
 */
public interface NodeAttributeListener extends EventListener {

    /**
     * Invoked when the metadata of a node was changed.
     *
     * @param source   the Node this event comes from
     */
    public void attributeChanged(Node source);

}
