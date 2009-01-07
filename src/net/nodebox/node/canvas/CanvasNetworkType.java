package net.nodebox.node.canvas;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class CanvasNetworkType extends CanvasNodeType {

    public CanvasNetworkType(NodeTypeLibrary library) {
        super(library, "canvasnet");
        setDescription("Contains image, vector and text nodes.");
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(1000.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(1000.0);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        throw new AssertionError("Nodes created by CanvasNetworkType should not reach this point.");
    }

    @Override
    public Node createNode() {
        return new CanvasNetwork(this);
    }

}
