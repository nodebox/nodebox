package nodebox.client.parameter;

import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

/**
 * Interface for controls. We also expect the control to extend JComponent or a subclass.
 * <p/>
 * Parameter controls are also ParameterValueListenersm, because they receive events from
 * their parameter. They do this by overriding addNotify() to register for the event, and removeNotify() to unregister.
 * <p/>
 * In the valueChanged event, they need to check if they are the source for the event.
 */
public interface ParameterControl extends ParameterValueListener {

    public Parameter getParameter();

    public void setValueForControl(Object v);

}
