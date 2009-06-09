package nodebox.graphics;

/**
 * Generic error object that happens during execution of NodeBox commands, generally in the GraphicsContext.
 *
 * @see nodebox.graphics.GraphicsContext
 */
public class NodeBoxError extends RuntimeException {
    public NodeBoxError(String message) {
        super(message);
    }
}
