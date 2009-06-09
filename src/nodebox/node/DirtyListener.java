package nodebox.node;

import java.util.EventListener;

/**
 * A DirtyListener responds to nodes being marked dirty.
 */
public interface DirtyListener extends EventListener {

    /**
     * Invoked when the node gets marked dirty.
     *
     * @param node the dirty node
     */
    public void nodeDirty(Node node);

    /**
     * Invoked when the node gets updated.
     *
     * @param node the newly updated node
     */
    public void nodeUpdated(Node node);

}