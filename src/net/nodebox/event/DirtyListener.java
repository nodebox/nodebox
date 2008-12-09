package net.nodebox.event;

import net.nodebox.node.Network;

import java.util.EventListener;

/**
 * A DirtyListener responds to data changes happening in the network that mark nodes as dirty.
 */
public interface DirtyListener extends EventListener {

    /**
     * Invoked when the network is dirty.
     *
     * @param network the dirty network
     */
    public void networkDirty(Network network);

}
