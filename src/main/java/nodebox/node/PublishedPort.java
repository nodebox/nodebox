package nodebox.node;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Publishes a child node's input port on a network node.
 * </p>
 *
 * By publishing a child node's port on a network node, you make the port and it's data accessible
 * to the network and all of its sibling nodes. The aliased port can be used as on the network node
 * as if it was one of its own actual input ports.
 * </p>
 * A published port can itself be published again on a higher network level.
 */
public final class PublishedPort {

    private final String childNode;
    private final String childPort;
    private final String publishedName;

    /**
     * Creates a published port of a given child node and port and a name by which it is known.
     *
     * @param childNode     The name of the child Node.
     * @param childPort     The name of the child Port.
     * @param publishedName The name by which the published port is known.
     */
    public PublishedPort(String childNode, String childPort, String publishedName) {
        checkNotNull(childNode);
        checkNotNull(childPort);
        checkNotNull(publishedName);
        this.childNode = childNode;
        this.childPort = childPort;
        this.publishedName = publishedName;
    }

    public String getChildNode() {
        return childNode;
    }

    public String getChildPort() {
        return childPort;
    }

    public String getPublishedName() {
        return publishedName;
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(childNode, childPort, publishedName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PublishedPort)) return false;
        final PublishedPort other = (PublishedPort) o;
        return Objects.equal(childNode, other.childNode)
                && Objects.equal(childPort, other.childPort)
                && Objects.equal(publishedName, other.publishedName);
    }

    @Override
    public String toString() {
        return String.format("<PublishedPort %s : %s.%s>", getPublishedName(), getChildNode(), getChildPort());
    }

}
