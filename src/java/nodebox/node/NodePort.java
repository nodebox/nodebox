package nodebox.node;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A combination of a node and a port.
 * <p/>
 * This is used as the key for the inputValuesMap.
 */
public final class NodePort {
    public final String node;
    public final String port;

    public static NodePort of(String node, String port) {
        return new NodePort(node, port);
    }

    NodePort(String node, String port) {
        checkNotNull(node);
        checkNotNull(port);
        this.node = node;
        this.port = port;
    }

    public String getNode() {
        return node;
    }

    public String getPort() {
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
