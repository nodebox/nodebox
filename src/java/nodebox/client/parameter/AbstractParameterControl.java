package nodebox.client.parameter;

import nodebox.node.Parameter;

import javax.swing.*;

public abstract class AbstractParameterControl extends JComponent implements ParameterControl {

    protected Parameter parameter;
    private OnValueChangeListener onValueChangeListener;

    protected AbstractParameterControl(Parameter parameter) {
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameterValue(Object value) {
        onValueChangeListener.onValueChange(this, value);
    }

    public void setValueChangeListener(OnValueChangeListener l) {
        onValueChangeListener = l;
    }

    public OnValueChangeListener getValueChangeListener() {
        return onValueChangeListener;
    }
}
