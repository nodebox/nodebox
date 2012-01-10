package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;

public class ChildRemovedEvent extends NodeEvent {

    private Node child;

    public ChildRemovedEvent(Node source, Node child) {
        super(source);
        this.child = child;
    }

    public Node getChild() {
        return child;
    }

    @Override
    public String toString() {
        return "ChildRemovedEvent{" +
                "source=" + getSource() +
                ", child=" + child +
                '}';
    }
}
