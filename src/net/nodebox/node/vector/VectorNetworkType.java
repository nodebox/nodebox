package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.node.*;

public class VectorNetworkType extends VectorNodeType {

    public class VectorNetworkExtender extends Network {
        public VectorNetworkExtender(NodeType nodeType) {
            super(nodeType);
        }

        @Override
        public boolean process(ProcessingContext ctx) {
            boolean success = updateRenderedNode(ctx);
            if (success) {
                Object outputValue = getRenderedNode().getOutputValue();
                if (outputValue instanceof Grob) {
                    Grob g = (Grob) outputValue;
                    g.translate(asFloat("tx"), asFloat("ty"));
                    g.rotate(asFloat("r"));
                    g.scale(asFloat("sx"), asFloat("sy"));
                    setOutputValue(g);
                } else {
                    throw new AssertionError(getAbsolutePath() + ": output of rendered node is not Grob, but " + outputValue);
                }
            } else {
                getOutputParameter().revertToDefault();
            }
            return success;
        }
    }

    public VectorNetworkType(NodeManager manager) {
        super(manager, "net.nodebox.node.vector.network");
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
    public String getDefaultName() {
        return "vecnet";
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        throw new AssertionError("Nodes created by CanvasNetworkType should not reach this point.");
    }

    @Override
    public Node createNode() {
        return new VectorNetworkExtender(this);
    }
}
