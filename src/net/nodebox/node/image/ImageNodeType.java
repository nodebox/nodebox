package net.nodebox.node.image;

import net.nodebox.node.NodeManager;
import net.nodebox.node.NodeType;
import net.nodebox.node.ParameterType;

public abstract class ImageNodeType extends NodeType {

    public ImageNodeType(NodeManager manager, String identifier) {
        super(manager, identifier, ParameterType.Type.GROB_IMAGE);
    }

}
