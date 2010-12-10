package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;
import nodebox.node.Parameter;

public class ValueChangedEvent extends NodeEvent {

    private Parameter parameter;

    public ValueChangedEvent(Node source, Parameter parameter) {
        super(source);
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return "ValueChangedEvent{" +
                "source=" + getSource() +
                ", parameter=" + parameter +
                '}';
    }
}
