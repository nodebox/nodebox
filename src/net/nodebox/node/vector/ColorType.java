package net.nodebox.node.vector;

import net.nodebox.graphics.*;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;
import net.nodebox.node.grob.GrobNodeType;

public class ColorType extends GrobNodeType {

    public ColorType(NodeTypeLibrary library) {
        super(library, "color");
        setDescription("Changes the color of the input shape.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB);
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setBoundingMethod(ParameterType.BoundingMethod.HARD);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Grob shape = node.asGrob("shape").clone();
        Color fillColor = node.asColor("fill");
        Color strokeColor = node.asColor("stroke");
        double strokeWidth = node.asFloat("strokewidth");
        changeColor(shape, fillColor, strokeColor, strokeWidth);
        node.setOutputValue(shape);
        return true;
    }

    private void changeColor(Grob grob, Color fillColor, Color strokeColor, double strokeWidth) {
        if (grob instanceof BezierPath) {
            BezierPath p = (BezierPath) grob;
            p.setFillColor(fillColor.clone());
            p.setStrokeColor(strokeColor.clone());
            p.setStrokeWidth(strokeWidth);

        } else if (grob instanceof Text) {
            Text t = (Text) grob;
            t.setFillColor(fillColor.clone());

        } else if (grob instanceof Group) {
            Group g = (Group) grob;
            for (Grob child : g.getGrobs()) {
                changeColor(child, fillColor, strokeColor, strokeWidth);
            }
        }
    }

}
