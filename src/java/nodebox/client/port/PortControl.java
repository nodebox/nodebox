package nodebox.client.port;

import nodebox.node.Port;

/**
 * Interface for controls. We also expect the control to extend JComponent or a subclass.
 */
public interface PortControl {

    public Port getPort();

    public void setValueForControl(Object v);

    public boolean isEnabled();

    public boolean isVisible();

    public void setValueChangeListener(OnValueChangeListener l);

    public OnValueChangeListener getValueChangeListener();

    public static interface OnValueChangeListener {
        public void onValueChange(PortControl control, Object newValue);
    }

}
