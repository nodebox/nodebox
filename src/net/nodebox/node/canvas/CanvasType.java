package net.nodebox.node.canvas;

import net.nodebox.graphics.Canvas;
import net.nodebox.graphics.Color;
import net.nodebox.graphics.Grob;
import net.nodebox.handle.FourPointHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.List;

public class CanvasType extends CanvasNodeType {

    public CanvasType(NodeTypeLibrary library) {
        super(library, "canvas");
        ParameterType pShapes = addParameterType("shapes", ParameterType.Type.GROB);
        pShapes.setCardinality(ParameterType.Cardinality.MULTIPLE);
        addParameterType("x", ParameterType.Type.FLOAT);
        addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType width = addParameterType("width", ParameterType.Type.FLOAT);
        width.setDefaultValue(500);
        ParameterType height = addParameterType("height", ParameterType.Type.FLOAT);
        height.setDefaultValue(500);
        ParameterType background = addParameterType("background", ParameterType.Type.COLOR);
        background.setDefaultValue(new Color(1, 1, 1));
    }

    public boolean process(Node node, ProcessingContext ctx) {
        Canvas canvas = new Canvas(node.asFloat("width"), node.asFloat("height"));
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        //canvas.translate(-x, -y);
        canvas.setBackground(node.asColor("background"));
        List<Object> shapes = node.getValues("shapes");
        for (Object shapeObject : shapes) {
            Grob shape = (Grob) shapeObject;
            shape = shape.clone();
            shape.translate(x, y);
            canvas.add(shape);
        }
        node.setOutputValue(canvas);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        FourPointHandle handle = new FourPointHandle(node);
        return handle;
    }
}
