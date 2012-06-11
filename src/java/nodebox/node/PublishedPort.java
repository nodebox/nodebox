package nodebox.node;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class PublishedPort {

    private final String inputNode;
    private final String inputPort;
    private final String publishedName;

    /**
     * Creates a published port of a given port and a name by which it is known.
     *
     * @param inputNode     The name of the input Node.
     * @param inputPort     The name of the input Port.
     * @param publishedName The name by which the published port is known.
     */
    public PublishedPort(String inputNode, String inputPort, String publishedName) {
        checkNotNull(inputNode);
        checkNotNull(inputPort);
        checkNotNull(publishedName);
        this.inputNode = inputNode;
        this.inputPort = inputPort;
        this.publishedName = publishedName;
    }

    public String getInputNode() {
        return inputNode;
    }

    public String getInputPort() {
        return inputPort;
    }

    public String getPublishedName() {
        return publishedName;
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(inputNode, inputPort, publishedName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PublishedPort)) return false;
        final PublishedPort other = (PublishedPort) o;
        return Objects.equal(inputNode, other.inputNode)
                && Objects.equal(inputPort, other.inputPort)
                && Objects.equal(publishedName, other.publishedName);
    }

    @Override
    public String toString() {
        return String.format("<PublishedPort %s : %s.%s>", getPublishedName(), getInputNode(), getInputPort());
    }

}
