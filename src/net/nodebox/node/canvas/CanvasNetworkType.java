package net.nodebox.node.canvas;

import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class CanvasNetworkType extends CanvasNodeType {

    public CanvasNetworkType(NodeManager manager) {
        super(manager, "net.nodebox.node.canvas.network");
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(1000.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(1000.0);
    }

    @Override
    public String getDefaultName() {
        return "canvasnet";
    }

    public boolean process(Node node, ProcessingContext ctx) {
        throw new AssertionError("Nodes created by CanvasNetworkType should not reach this point.");
    }

    @Override
    public Node createNode() {
        return new CanvasNetwork(this);
    }

}
