package net.nodebox.node.grob;

import net.nodebox.node.NodeType;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;

public abstract class GrobNodeType extends NodeType {

    public GrobNodeType(NodeTypeLibrary library, String identifier) {
        super(library, identifier, ParameterType.Type.GROB);
    }

}
