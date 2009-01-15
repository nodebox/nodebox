package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.handle.FourPointHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class RectType extends PathNodeType {

    public RectType(NodeTypeLibrary library) {
        super(library, "rect");
        setDescription("Creates a rectangle.");

        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(100.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(100.0);
        ParameterType pRx = addParameterType("rx", ParameterType.Type.FLOAT);
        pRx.setLabel("Roundness X");
        ParameterType pRy = addParameterType("ry", ParameterType.Type.FLOAT);
        pRy.setLabel("Roundness Y");
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath p = new BezierPath();
        p.setFillColor(node.asColor("fill"));
        p.setStrokeColor(node.asColor("stroke"));
        p.setStrokeWidth(node.asFloat("strokewidth"));
        double rx = node.asFloat("rx");
        double ry = node.asFloat("ry");
        if (rx == 0 && ry == 0) {
            p.rect(node.asFloat("x"), node.asFloat("y"), node.asFloat("width"), node.asFloat("height"));
        } else {
            p.roundedRect(node.asFloat("x"), node.asFloat("y"), node.asFloat("width"), node.asFloat("height"), rx, ry);
        }
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
        return new FourPointHandle(node);
    }
}
