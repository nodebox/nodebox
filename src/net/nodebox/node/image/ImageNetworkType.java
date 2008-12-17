package net.nodebox.node.image;

import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.ProcessingContext;

public class ImageNetworkType extends ImageNodeType {

    public ImageNetworkType(NodeManager manager) {
        super(manager, "net.nodebox.node.image.network");
    }

    @Override
    public String getDefaultName() {
        return "imagenet";
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
