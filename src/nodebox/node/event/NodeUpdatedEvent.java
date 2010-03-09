package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;
import nodebox.node.ProcessingContext;

public class NodeUpdatedEvent extends NodeEvent {

    private ProcessingContext context;


    public NodeUpdatedEvent(Node source, ProcessingContext context) {
        super(source);
        this.context = context;
    }

    public ProcessingContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "NodeUpdatedEvent{" +
                "source=" + getSource() +
                '}';
    }

}
