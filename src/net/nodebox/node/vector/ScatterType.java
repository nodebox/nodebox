package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Point;
import net.nodebox.graphics.Rect;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.Random;

public class ScatterType extends VectorNodeType {

    public ScatterType(NodeTypeLibrary library) {
        super(library, "scatter");
        setDescription("Randomly distributes points inside the shape.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB_VECTOR);
        ParameterType pPoints = addParameterType("points", ParameterType.Type.INT);
        pPoints.setDefaultValue(20);
        ParameterType pSeed = addParameterType("seed", ParameterType.Type.SEED);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        Grob shape = node.asGrob("shape").clone();
        int amount = node.asInt("points");
        Random random = new Random(node.asInt("seed"));
        for (Grob grob : shape.getChildren(BezierPath.class)) {
            BezierPath p = (BezierPath) grob;
            // We just want all the path settings, not the path data,
            // so copy and clear the path.
            BezierPath oldPath = p.clone();
            Rect bounds = oldPath.getBounds();
            double bx = bounds.getX();
            double by = bounds.getY();
            double bw = bounds.getWidth();
            double bh = bounds.getHeight();
            Point pt = null;
            p.clear();
            for (int i = 0; i < amount; i++) {
                int tries = 100;
                while (tries > 0) {
                    pt = new Point(bx + random.nextDouble() * bw, by + random.nextDouble() * bh);
                    if (oldPath.contains(pt))
                        break;
                    tries--;
                }
                if (tries == 0) {
                    node.addWarning("Scatter: no points found on the path.");
                } else {
                    p.moveto(pt);
                }
            }
        }
        node.setOutputValue(shape);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        return new DisplayPointsHandle(node);
    }
}
