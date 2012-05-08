package nodebox.node;

/**
 * An exception that wraps the errors that occur during node renderNetwork time.
 */
public class NodeRenderException extends RuntimeException {

    private final Node node;

    public NodeRenderException(Node node, String message) {
        super("Error while rendering " + node + ": " + message);
        this.node = node;
    }

    public NodeRenderException(Node node, Throwable t) {
        super("Error while rendering " + node + ": " + t, t);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

}
