package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.handle.FourPointHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class RectType extends VectorNodeType {

    public RectType(NodeManager manager) {
        super(manager, "net.nodebox.node.vector.rect");
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(100.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(100.0);
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Group g = new Group();
        BezierPath p = new BezierPath();
        p.setFillColor(node.asColor("fill"));
        p.setStrokeColor(node.asColor("stroke"));
        p.setStrokeWidth(node.asFloat("strokewidth"));
        p.addRect(node.asFloat("x"), node.asFloat("y"), node.asFloat("width"), node.asFloat("height"));
        g.add(p);
        node.setOutputValue(g);
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
