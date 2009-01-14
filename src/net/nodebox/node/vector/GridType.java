package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class GridType extends VectorNodeType {

    public GridType(NodeTypeLibrary library) {
        super(library, "grid");
        setDescription("Creates a grid of points.");
        ParameterType pRows = addParameterType("rows", ParameterType.Type.INT);
        pRows.setDefaultValue(10);
        ParameterType pColumns = addParameterType("columns", ParameterType.Type.INT);
        pColumns.setDefaultValue(10);
        ParameterType pRowsize = addParameterType("rowsize", ParameterType.Type.FLOAT);
        pRowsize.setDefaultValue(10.0);
        ParameterType pColumnsize = addParameterType("columnsize", ParameterType.Type.FLOAT);
        pColumnsize.setDefaultValue(10.0);
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        int rows = node.asInt("rows");
        int columns = node.asInt("columns");
        double rowsize = node.asFloat("rowsize");
        double columnsize = node.asFloat("columnsize");
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        Group g = new Group();
        BezierPath p = new BezierPath();
        for (int rowindex = 0; rowindex < rows; rowindex++) {
            for (int columnindex = 0; columnindex < columns; columnindex++) {
                p.moveto(x + columnindex * columnsize, y + rowindex * rowsize);
            }
        }
        g.add(p);
        node.setOutputValue(g);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        return new DisplayPointsHandle(node);
    }
}
