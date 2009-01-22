package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.handle.FourPointHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class StarType extends PathNodeType {

    public StarType(NodeTypeLibrary library) {
        super(library, "star");
        setDescription("Creates a star.");
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pPoints = addParameterType("points", ParameterType.Type.INT);
        pPoints.setDefaultValue(20);
        pPoints.setBoundingMethod(ParameterType.BoundingMethod.HARD);
        pPoints.setMinimumValue((double) 1);
        ParameterType pOuter = addParameterType("outer", ParameterType.Type.FLOAT);
        pOuter.setDefaultValue(100.0);
        ParameterType pInner = addParameterType("inner", ParameterType.Type.FLOAT);
        pInner.setDefaultValue(50.0);
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setBoundingMethod(ParameterType.BoundingMethod.HARD);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath p = new BezierPath();
        p.setFillColor(node.asColor("fill"));
        p.setStrokeColor(node.asColor("stroke"));
        p.setStrokeWidth(node.asFloat("strokewidth"));
        double startx = node.asFloat("x");
        double starty = node.asFloat("y");
        double outer = node.asFloat("outer");
        double inner = node.asFloat("inner");
        int points = node.asInt("points");
        p.moveto(startx, starty + outer);

        double angle, x, y, radius;
        for (int i = 1; i < points * 2; i++) {
            angle = i * Math.PI / (double) points;
            x = Math.sin(angle);
            y = Math.cos(angle);
            radius = i % 2 == 1 ? inner : outer;
            x = startx + radius * x;
            y = starty + radius * y;
            p.lineto(x, y);
        }
        p.close();
        node.setOutputValue(p);
        return true;
    }

    /**
     * Creates and returns a Handle object that can be used for direct manipulation of the parameters of this node.
     * By default, this code returns null to indicate that no handle is available. Classes can override this method
     * to provide an appropriate handle implementation.
     *
     * @param node the node instance that is bound to this handle.
     * @return a handle instance bound to this node, or null.
     */
    @Override
    public Handle createHandle(Node node) {
        return new FourPointHandle(node, "x", "y", "outer", "inner");
    }

}
