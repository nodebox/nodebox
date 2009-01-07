package net.nodebox.node.image;

import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ProcessingContext;

public class ImageNetworkType extends ImageNodeType {

    public ImageNetworkType(NodeTypeLibrary library) {
        super(library, "imagenet");
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        throw new RuntimeException("Image network is not implemented yet.");
    }

    @Override
    public Node createNode() {
        return new Network(this);
    }

}
