package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Parameter;

import javax.swing.*;

public abstract class AbstractParameterControl extends JComponent implements ParameterControl {

    protected NodeBoxDocument document;
    protected Parameter parameter;
    private boolean disabled;

    protected AbstractParameterControl(NodeBoxDocument document, Parameter parameter) {
        this.document = document;
        this.parameter = parameter;
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void valueChanged(Parameter source) {
        if (parameter != source) return;
        setValueForControl(source.getValue());
    }

    public void setParameterValue(Object value) {
        document.setParameterValue(parameter, value);
    }

}
