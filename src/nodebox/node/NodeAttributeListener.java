package nodebox.node;

import java.util.EventListener;

/**
 * A NodeAttributeListener responds to changes in the metadata of the node, such as its name, position, etc.
 */
public interface NodeAttributeListener extends EventListener {

    public enum Attribute {
        LIBRARY, NAME, POSITION, EXPORT, DESCRIPTION, IMAGE, PARAMETER, PORT
    }

    /**
     * Invoked when the metadata of a node was changed.
     *
     * @param source    the Node this event comes from
     * @param attribute the changed attribute
     */
    public void attributeChanged(Node source, Attribute attribute);

}
