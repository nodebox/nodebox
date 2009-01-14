package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ProcessingContext;

public class SortType extends VectorNodeType {

    public SortType(NodeTypeLibrary library) {
        super(library, "sort");
        setDescription("Sorts the points on the object.");
    }

    public boolean process(Node node, ProcessingContext ctx) {
        return false;
    }
}
