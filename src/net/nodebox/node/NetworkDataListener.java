package net.nodebox.node;

import java.util.EventListener;

/**
 * A DirtyListener responds to data changes happening in the network that mark nodes as dirty.
 */
public interface NetworkDataListener extends EventListener {

    /**
     * Invoked when the network is dirty.
     *
     * @param network the dirty network
     */
    public void networkDirty(Network network);

    /**
     * Invoked when the data in the network gets updated.
     *
     * @param network the newly updated network
     */
    public void networkUpdated(Network network);

}