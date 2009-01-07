package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class VectorNetworkType extends VectorNodeType {

    public VectorNetworkType(NodeTypeLibrary library) {
        super(library, "vecnet");
        setDescription("Contains vector nodes.");
        ParameterType pTx = addParameterType("tx", ParameterType.Type.FLOAT);
        pTx.setLabel("Transform X");
        ParameterType pTy = addParameterType("ty", ParameterType.Type.FLOAT);
        pTy.setLabel("Transform Y");
        ParameterType pR = addParameterType("r", ParameterType.Type.FLOAT);
        pR.setLabel("Rotation");
        ParameterType pSx = addParameterType("sx", ParameterType.Type.FLOAT);
        pSx.setLabel("Scale X");
        pSx.setDefaultValue(1.0);
        ParameterType pSy = addParameterType("sy", ParameterType.Type.FLOAT);
        pSy.setLabel("Scale Y");
        pSy.setDefaultValue(1.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        throw new AssertionError("Nodes created by CanvasNetworkType should not reach this point.");
    }

    @Override
    public Node createNode() {
        return new VectorNetwork(this);
    }
}
