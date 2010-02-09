package nodebox.client.parameter;

import nodebox.node.Parameter;

import javax.swing.*;

public abstract class AbstractParameterControl extends JComponent implements ParameterControl {

    protected Parameter parameter;
    private boolean disabled;

    protected AbstractParameterControl(Parameter parameter) {
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        parameter.getNode().addParameterValueListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        parameter.getNode().removeParameterValueListener(this);
    }

    public void valueChanged(Parameter source) {
        if (parameter != source) return;
        setValueForControl(source.getValue());
    }

}
