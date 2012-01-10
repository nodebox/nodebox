package nodebox.node;

public abstract class NodeEvent {

    private Node source;

    protected NodeEvent(Node source) {
        this.source = source;
    }

    public Node getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "NodeEvent{" +
                "source=" + source +
                '}';
    }
}
