package net.nodebox.node;

import java.util.ArrayList;
import java.util.List;

public class OutputParameter extends Parameter {

    private List<Connection> downstreams = new ArrayList<Connection>();

    public OutputParameter(Node node, Type type) {
        super(node, "output", type);
    }

    /**
     * Adds a connection to the list of downstreams.
     * This creates the "other" end in the two-way connection relationship.
     * This method is not for general use, but the Parameter class uses this.
     * To create a connection, use Parameter.connect.
     *
     * @param connection the connection to add.
     * @return true (as per the general contract of the Collection.add method).
     * @see Parameter#connect(Node)
     */
    public boolean addDownstreamConnection(Connection connection) {
        return downstreams.add(connection);
    }

    /**
     * Removes a connection from the downstreams.
     *
     * @param connection the connection to remove.
     * @return true if the downstreams contained the specified connetion.
     */
    public boolean removeDownstreamConnection(Connection connection) {
        return downstreams.remove(connection);
    }

    public List<Connection> getDownstreamConnections() {
        return downstreams;
    }

    public void markDirtyDownstream() {
        for (Connection c : downstreams) {
            c.markDirtyDownstream();
        }
    }

    @Override
    public Connection connect(Node outputNode) {
        throw new ConnectionError(outputNode, this, "You can only connect from an input parameter.");
    }

    @Override
    public boolean disconnect() {
        throw new ConnectionError(getNode(), this, "You can only disconnect from an input parameter.");
    }

    @Override
    public void validate(Object value) {
        throw new AssertionError("Code should never validate output parameters.");
    }

    @Override
    public boolean isConnected() {
        return !downstreams.isEmpty();
    }

    @Override
    public boolean isConnectedTo(Parameter inputParameter) {
        // Output paramters can only be connected to input parameters.
        if (inputParameter instanceof OutputParameter) return false;
        for (Connection c : downstreams) {
            if (c.getInputParameter() == inputParameter)
                return true;
        }
        return false;
    }

    @Override
    public boolean canConnectTo(Parameter parameter) {
        // Output paramters can only be connected to input parameters.
        if (parameter instanceof OutputParameter) return false;
        return parameter.getCoreType() == getCoreType();
    }

    @Override
    public boolean isConnectedTo(Node node) {
        for (Connection c : downstreams) {
            if (c.getInputNode() == node)
                return true;
        }
        return false;
    }

}
