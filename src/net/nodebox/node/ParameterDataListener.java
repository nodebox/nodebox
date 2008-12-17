package net.nodebox.node;

import java.util.EventListener;

/**
 * A ParameterDataListener responds to changes in the data of the parameter. The Parameter class implements this,
 * to support expression dependencies.
 */
public interface ParameterDataListener extends EventListener {

    /**
     * Invoked when the value of a parameter was changed, either through connections, expressions or directly.
     *
     * @param source   the Parameter this event comes from
     * @param newValue the new value of the Parameter
     */
    public void valueChanged(Parameter source, Object newValue);

}