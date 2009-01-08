package net.nodebox.node;

public class CoreNodeTypeLibrary extends NodeTypeLibrary {

    public CoreNodeTypeLibrary(String name, Version version) {
        super(name, version);
    }

    /**
     * Adds a node type to this library.
     * <p/>
     * This method is made public from protected access in the NodeTypeLibrary.
     *
     * @param nodeType the node type to add.
     */
    @Override
    public void addNodeType(NodeType nodeType) {
        super.addNodeType(nodeType);
    }
}
