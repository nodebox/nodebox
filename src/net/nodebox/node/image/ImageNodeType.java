package net.nodebox.node.image;

import net.nodebox.node.NodeType;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;

public abstract class ImageNodeType extends NodeType {

    public ImageNodeType(NodeTypeLibrary library, String identifier) {
        super(library, identifier, ParameterType.Type.GROB_IMAGE);
    }

}
