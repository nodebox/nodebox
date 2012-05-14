package nodebox.node;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A combination of a node and a port.
 * <p/>
 * This is used as the key for the inputValuesMap.
 */
public final class NodePort {
    public final Node node;
    public final Port port;

    public static NodePort of(Node node, Port port) {
        return new NodePort(node, port);
    }

    public static NodePort of(Node node, String portName) {
        return new NodePort(node, node.getInput(portName));
    }

    NodePort(Node node, Port port) {
        checkNotNull(node);
        checkNotNull(port);
        this.node = node;
        this.port = port;
    }

    public Node getNode() {
        return node;
    }

    public Port getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodePort)) return false;
        final NodePort other = (NodePort) o;
        return Objects.equal(node, other.node)
                && Objects.equal(port, other.port);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(node, port);
    }
}
