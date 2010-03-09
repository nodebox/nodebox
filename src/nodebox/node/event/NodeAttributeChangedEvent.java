package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;

public class NodeAttributeChangedEvent extends NodeEvent {

    private Node.Attribute attribute;

    public NodeAttributeChangedEvent(Node source, Node.Attribute attribute) {
        super(source);
        this.attribute = attribute;
    }

    public Node.Attribute getAttribute() {
        return attribute;
    }

    @Override
    public String toString() {
        return "NodeAttributeChangedEvent{" +
                "source=" + getSource() +
                "attribute=" + attribute +
                '}';
    }
}
