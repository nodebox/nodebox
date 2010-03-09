package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;

public class ChildAddedEvent extends NodeEvent {

    private Node child;

    public ChildAddedEvent(Node source, Node child) {
        super(source);
        this.child = child;
    }

    public Node getChild() {
        return child;
    }

    @Override
    public String toString() {
        return "ChildAddedEvent{" +
                "child=" + child +
                "} " + super.toString();
    }
}
