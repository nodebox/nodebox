package net.nodebox.node;

import java.util.EventListener;

/**
 * A ParameterValueListener responds to changes in the metadata of the parameter, such as its type, bounding, etc.
 */
public interface ParameterAttributeListener extends EventListener {

    /**
     * Invoked when the metadata of a parameter was changed.
     *
     * @param source   the Parameter this event comes from
     */
    public void attributeChanged(Parameter source);

}
