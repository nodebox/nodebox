package net.nodebox.event;

import net.nodebox.node.Network;

import java.util.EventListener;

/**
 * Start an end processing.
 */
public interface ProcessingListener extends EventListener {

    /**
     * Invoked when the network starts processing.
     *
     * @param network the network
     */
    public void startProcessing(Network network);

    /**
     * Invoked when the network stops processing.
     *
     * @param network
     */
    public void endProcessing(Network network);

}
