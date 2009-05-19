package net.nodebox.node;

import java.util.EventListener;

/**
 * A ParameterValueListener responds to changes in the data of the parameter.
 * <p/>
 * This is useful for controls that need to know if the parameter they edit has been changed.
 */
public interface ParameterValueListener extends EventListener {

    /**
     * Invoked when the value of a parameter was changed, either through expressions or directly.
     *
     * @param source   the Parameter this event comes from
     */
    public void valueChanged(Parameter source);

}