package net.nodebox.node.grob;

import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Transform;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class TransformType extends GrobNodeType {

    public TransformType(NodeTypeLibrary library) {
        super(library, "transform");
        setDescription("Transforms the input geometry.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB);
        ParameterType pTx = addParameterType("tx", ParameterType.Type.FLOAT);
        pTx.setLabel("Transform X");
        ParameterType pTy = addParameterType("ty", ParameterType.Type.FLOAT);
        pTy.setLabel("Transform Y");
        ParameterType pR = addParameterType("r", ParameterType.Type.FLOAT);
        pR.setLabel("Rotation");
        ParameterType pSx = addParameterType("sx", ParameterType.Type.FLOAT);
        pSx.setLabel("Scale X");
        pSx.setDefaultValue(100.0);
        ParameterType pSy = addParameterType("sy", ParameterType.Type.FLOAT);
        pSy.setLabel("Scale Y");
        pSy.setDefaultValue(100.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Grob g = node.asGrob("shape");
        Grob outGrob = g;
        Transform t = new Transform();
        t.translate(node.asFloat("tx"), node.asFloat("ty"));
        t.rotate(node.asFloat("r"));
        t.scale(node.asFloat("sx") / 100.0, node.asFloat("sy") / 100.0);
        outGrob.appendTransform(t);
        node.setOutputValue(outGrob);
        return true;
    }

}
