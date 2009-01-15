package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class GridType extends PathNodeType {

    public GridType(NodeTypeLibrary library) {
        super(library, "grid");
        setDescription("Creates a grid of points.");
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(300.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(300.0);
        ParameterType pRows = addParameterType("rows", ParameterType.Type.INT);
        pRows.setDefaultValue(10);
        ParameterType pColumns = addParameterType("columns", ParameterType.Type.INT);
        pColumns.setDefaultValue(10);
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        double width = node.asFloat("width");
        double height = node.asFloat("height");
        int rows = node.asInt("rows");
        int columns = node.asInt("columns");
        double x = node.asFloat("x");
        double y = node.asFloat("y");

        double columnsize = 0;
        double left = 0;
        if (columns > 1) {
            columnsize = width / (columns - 1);
            left = x - width / 2;
        }
        double rowsize = 0;
        double top = 0;
        if (rows > 1) {
            rowsize = height / (rows - 1);
            top = y - height / 2;
        }

        BezierPath p = new BezierPath();
        for (int rowindex = 0; rowindex < rows; rowindex++) {
            for (int columnindex = 0; columnindex < columns; columnindex++) {
                p.moveto(left + columnindex * columnsize, top + rowindex * rowsize);
            }
        }
        node.setOutputValue(p);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        return new DisplayPointsHandle(node);
    }
}
