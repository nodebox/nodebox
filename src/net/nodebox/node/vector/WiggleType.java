package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.PathElement;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.Random;

public class WiggleType extends PathNodeType {

    public WiggleType(NodeTypeLibrary library) {
        super(library, "wiggle");
        setDescription("Mutates the points of the path by adding a random value.");
        addParameterType("path", ParameterType.Type.GROB_PATH);
        ParameterType wx = addParameterType("wx", ParameterType.Type.FLOAT);
        wx.setLabel("Wiggle X");
        ParameterType wy = addParameterType("wy", ParameterType.Type.FLOAT);
        wy.setLabel("Wiggle Y");
        ParameterType seed = addParameterType("seed", ParameterType.Type.SEED);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath path = (BezierPath) node.asGrob("path");
        path = path.clone();
        double wx = node.asFloat("wx");
        double wy = node.asFloat("wy");
        Random random = new Random(node.asInt("seed"));
        for (PathElement el : path.getElements()) {
            if (el.getCommand() == PathElement.CLOSE) continue;
            // Random returns a value between 0 and 1.
            // We need a value that goes from -wx to wx.
            double dx = (random.nextDouble() - 0.5) * wx * 2;
            double dy = (random.nextDouble() - 0.5) * wy * 2;
            el.getPoint().move(dx, dy);
            if (el.getCommand() == PathElement.CURVETO) {
                el.getControl1().move(dx, dy);
                el.getControl2().move(dx, dy);
            }
        }
        node.setOutputValue(path);
        return true;
    }
}
