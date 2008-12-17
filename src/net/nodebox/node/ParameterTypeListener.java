package net.nodebox.node;

import java.util.EventListener;

/**
 * A ParameterTypeListener responds to changes in the parameter type. The Parameter class implements this, and can
 * adapt its data accordingly.
 */
public interface ParameterTypeListener extends EventListener {

    /**
     * Invoked when the type/core type were changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void typeChanged(ParameterType source);

    /**
     * Invoked when the bounding method or minimum/maximum values were changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void boundingChanged(ParameterType source);

    /**
     * Invoked when the display level was changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void displayLevelChanged(ParameterType source);

    /**
     * Invoked when the null allowed flag was changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void nullAllowedChanged(ParameterType source);
}