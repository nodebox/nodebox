package nodebox.node.event;

import nodebox.node.Node;
import nodebox.node.NodeEvent;

public class RenderedChildChangedEvent extends NodeEvent {

    public Node renderedChild;

    public RenderedChildChangedEvent(Node source, Node renderedChild) {
        super(source);
        this.renderedChild = renderedChild;
    }

    public Node getRenderedChild() {
        return renderedChild;
    }

    @Override
    public String toString() {
        return "RenderedChildChangedEvent{" +
                "source=" + getSource() +
                ", renderedChild=" + renderedChild +
                '}';
    }
}
