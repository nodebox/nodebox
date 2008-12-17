package net.nodebox.node.canvas;

import net.nodebox.graphics.Canvas;
import net.nodebox.graphics.Grob;
import net.nodebox.node.*;

public class CanvasNetworkType extends CanvasNodeType {

    public class CanvasNetworkExtender extends Network {
        public CanvasNetworkExtender(NodeType nodeType) {
            super(nodeType);
        }

        @Override
        public boolean process(ProcessingContext ctx) {
            boolean success = updateRenderedNode(ctx);
            if (success) {
                Canvas c = new Canvas(asFloat("width"), asFloat("height"));
                Object outputValue = getRenderedNode().getOutputValue();
                if (outputValue instanceof Grob) {
                    c.add((Grob) outputValue);
                    setOutputValue(c);
                } else {
                    throw new AssertionError(getAbsolutePath() + ": output of rendered node is not Grob, but " + outputValue);
                }
            } else {
                getOutputParameter().revertToDefault();
            }
            return success;
        }
    }

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
        return new CanvasNetworkExtender(this);
    }

}
