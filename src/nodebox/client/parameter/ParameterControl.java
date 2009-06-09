package nodebox.client.parameter;

import nodebox.node.Parameter;

/**
 * Interface for controls. We also expect the control to extend JComponent or a subclass.
 */
public interface ParameterControl {
    Parameter getParameter();

    void setValueForControl(Object v);
}
