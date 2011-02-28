package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.node.NodeEvent;
import nodebox.node.Parameter;
import nodebox.node.event.ValueChangedEvent;

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

    @Override
    public void addNotify() {
        super.addNotify();
        parameter.getLibrary().addListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        parameter.getLibrary().removeListener(this);
    }

    public void receive(NodeEvent event) {
        if (!(event instanceof ValueChangedEvent)) return;
        if (((ValueChangedEvent) event).getParameter() != parameter) return;
        setValueForControl(parameter.getValue());
    }

    public void valueChanged(Parameter source) {
        if (parameter != source) return;
        setValueForControl(source.getValue());
    }

    public void setParameterValue(Object value) {
        document.setParameterValue(parameter, value);
    }

}
