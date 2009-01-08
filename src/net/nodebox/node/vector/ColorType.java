package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Color;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Text;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class ColorType extends VectorNodeType {

    public ColorType(NodeTypeLibrary library) {
        super(library, "color");
        setDescription("Changes the color of the input shape.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB_VECTOR);
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Grob shape = node.asGrob("shape").clone();
        Color fillColor = node.asColor("fill");
        Color strokeColor = node.asColor("stroke");
        double strokeWidth = node.asFloat("strokewidth");
        for (Grob grob : shape.getChildren()) {
            if (grob instanceof BezierPath) {
                BezierPath p = (BezierPath) grob;
                p.setFillColor(fillColor.clone());
                p.setStrokeColor(strokeColor.clone());
                p.setStrokeWidth(strokeWidth);
            } else if (grob instanceof Text) {
                Text t = (Text) grob;
                t.setFillColor(fillColor.clone());
            }
        }
        node.setOutputValue(shape);
        return true;
    }

}
