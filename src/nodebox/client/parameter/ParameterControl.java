package nodebox.client.parameter;

import nodebox.node.Parameter;

/**
 * Interface for controls. We also expect the control to extend JComponent or a subclass.
 */
public interface ParameterControl {

    public Parameter getParameter();

    public void setValueForControl(Object v);

    public boolean isEnabled();

    public boolean isVisible();

    public void setValueChangeListener(OnValueChangeListener l);

    public OnValueChangeListener getValueChangeListener();

    public static interface OnValueChangeListener {
        public void onValueChange(ParameterControl control, Object newValue);
    }

}
